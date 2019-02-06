/*
 * Copyright (c) 2018.  Thomas Dreja
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.thomasdreja.tools.sqlitestoreable.impl;

import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;

import de.thomasdreja.tools.sqlitestoreable.template.TableWrapper;
import de.thomasdreja.tools.sqlitestoreable.template.StoreAble;

/**
 * This class extends the database storage capabilities of the basic StoreAble to include Parcel support for Android.
 * It contains a constructor for Parcels, Databases
 * and an empty default constructor that should be used to create an empty object before adding it to the database.
 * @see TableWrapper#save(StoreAble, SQLiteDatabase)
 */
@SuppressWarnings({"unused"})
public abstract class ParcelStoreAble implements StoreAble, Parcelable {

    /**
     * ID of the StoreAble, invalid id if not yet added to database
     * @see StoreAble#INVALID_ID
     */
    private long id;

    /**
     * ID of a parent StoreAble, invalid id if none exists
     * @see StoreAble#INVALID_ID
     */
    private long relatedId;

    /**
     * Creates a new StoreAble based upon the data stored in the Parcel
     * @param in Parcel to be read from
     */
    @SuppressWarnings({"WeakerAccess"})
    protected ParcelStoreAble(Parcel in) {
        setId(in.readLong());
        setRelatedId(in.readLong());
    }

    /**
     * Creates a new and empty ParcelStoreAble with an invalid database ID
     * Use this constructor to create new instances before saving them into the database
     */
    public ParcelStoreAble() {
        setId(INVALID_ID);
        setRelatedId(INVALID_ID);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(getId());
        parcel.writeLong(getRelatedId());
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @Override
    public void setRelatedId(long relatedId) {
        this.relatedId = relatedId;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getRelatedId() {
        return relatedId;
    }
}
