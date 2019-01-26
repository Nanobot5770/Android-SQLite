/*
 * Copyright (c) 2018.  Thomas Dreja
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.thomasdreja.tools.sqlitestoreable;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class extends the database storage capabilities of the basic StoreAble to include Parcel support for Android.
 * It contains a constructor for Parcels, Databases
 * and an empty default constructor that should be used to create an empty object before adding it to the database.
 * @see SQLiteTable#save(StoreAble, SQLiteDatabase, Class)
 */
public abstract class ParcelStoreAble implements StoreAble, Parcelable {

    /**
     * Creates a new StoreAble based upon the data stored in the Parcel
     * @param in Parcel to be read from
     */
    protected ParcelStoreAble(Parcel in) {
        setId(in.readLong());
    }

    /**
     * Creates a new StoreAble object based upon the data contained in the given cursor
     * @param cursor Cursor containing data from the database
     * @see TableInformation#read(Cursor, Class)
     * @see SQLiteTable#get(long, SQLiteDatabase, Class)
     */
    public ParcelStoreAble(Cursor cursor) {
        this();
    }

    /**
     * Creates a new and empty ParcelStoreAble with an invalid database ID
     * Use this constructor to create new instances before saving them into the database
     */
    public ParcelStoreAble() {
        setId(-1);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(getId());
    }
}
