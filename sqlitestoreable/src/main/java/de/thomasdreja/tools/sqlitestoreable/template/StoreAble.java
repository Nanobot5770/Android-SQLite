/*
 * Copyright (c) 2018.  Thomas Dreja
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.thomasdreja.tools.sqlitestoreable.template;

import android.database.sqlite.SQLiteDatabase;

import de.thomasdreja.tools.sqlitestoreable.reflection.StoreAbleField;

/**
 * This interface provides the most basic methods to allow for storing objects into a SQLite database.
 * Any implementation should also provide a TableInformation to interconnect with the TableWrapper.
 * To include fields into the database, you must annotate them with StoreAbleField.
 * @see de.thomasdreja.tools.sqlitestoreable.reflection.TableInformation
 * @see TableWrapper
 * @see StoreAbleField
 */
public interface StoreAble {

    /**
     * Constant name for the ID field every StoreAble will have
     * @see StoreAble#getId()
     */
    String ID = "storeAbleId";

    /**
     * Constant name for the Related ID field every StoreAble will have
     * @see StoreAble#getRelatedId()
     */
    String RELATED_ID = "storeAbleRelatedID";

    /**
     * Whenever an object is not added to the database, it MUST have this value set as its ID
     * All objects in the database will have larger numbers than this value
     * @see StoreAble#setId(long)
     * @see StoreAble#getId()
     * @see TableWrapper#insert(StoreAble, SQLiteDatabase, boolean)
     */
    long INVALID_ID = 0;

    /**
     * Returns the database ID of the StoreAble. Used to update and store the element.
     * @return Database ID, or Invalid ID if none was set
     * @see StoreAble#INVALID_ID
     * @see StoreAble#ID
     */
    @StoreAbleField(fieldName = ID)
    long getId();

    /**
     * Updates the database ID of the StoreAble. Should not be called directly by the user, only TableWrapper
     * @param id New database ID of the StoreAble or Invalid ID to invalidate the ID
     * @see TableWrapper
     * @see StoreAble#INVALID_ID
     * @see StoreAble#ID
     */
    @StoreAbleField(fieldName = ID)
    void setId(long id);

    /**
     * Returns the reference to the parent collection, if there is any. Invalid ID if no parent is set
     * @return ID of the parent collection, Invalid ID if not is set
     * @see StoreAbleCollection
     * @see StoreAble#INVALID_ID
     * @see StoreAble#RELATED_ID
     */
    @StoreAbleField(fieldName = RELATED_ID)
    @SuppressWarnings("unused")
    long getRelatedId();

    /**
     * Sets the reference to the parent collection. To remove any reference, set the ID to invalid ID.
     * @param id ID of the parent collection, Invalid ID to remove reference
     * @see StoreAbleCollection
     * @see StoreAble#INVALID_ID
     * @see StoreAble#RELATED_ID
     */
    @StoreAbleField(fieldName = RELATED_ID)
    void setRelatedId(long id);

}
