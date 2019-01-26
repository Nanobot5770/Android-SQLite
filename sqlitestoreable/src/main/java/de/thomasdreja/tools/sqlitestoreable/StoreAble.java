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

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

/**
 * This interface provides the most basic methods to allow for storing objects into a SQLite database.
 * Any implementation should also provide a TableInformation to interconnect with the SQLiteTable
 * @see TableInformation
 * @see SQLiteTable
 */
public interface StoreAble {

    /**
     * Returns the database ID of the StoreAble. Used to update and store the element.
     * @return Database ID, or -1 if no valid ID was set
     */
    long getId();

    /**
     * Updates the database ID of the StoreAble. Should not be called directly by the user, only SQLiteTable
     * @param id New database ID of the StoreAble or -1 to invalidate the ID
     * @see SQLiteTable
     */
    void setId(long id);

    /**
     * Exports the objects values into the given ContentValues container so that they may be stored in the database.
     * @param databaseValues Empty ContentValues container, can be passed down the hierarchy.
     * @see SQLiteTable#save(StoreAble, SQLiteDatabase, Class)
     */
    void exportToDatabase(ContentValues databaseValues);

}
