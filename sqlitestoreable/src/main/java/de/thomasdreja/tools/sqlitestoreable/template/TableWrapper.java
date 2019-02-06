/*
 * Copyright (c) 2019 Thomas Dreja
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.thomasdreja.tools.sqlitestoreable.template;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collection;

import de.thomasdreja.tools.sqlitestoreable.reflection.ContentType;
import de.thomasdreja.tools.sqlitestoreable.reflection.TableInformation;

/**
 * This class provides all the methods necessary to create, read and write StoreAbles in specific table in SQLite database.
 * This class serves as a wrapper and should only be used in conjunction with a SQLiteOpenHelper.
 * @see android.database.sqlite.SQLiteOpenHelper
 * @see StoreAbleOpenHelper
 * @see TableInformation
 */
public class TableWrapper {
    // region Variables & Constructors

    /**
     * The table information defining the structure and type of this table
     */
    private final TableInformation information;

    /**
     * Creates a new SQLite table based upon the given information
     * @param information Helper used to create the table structure and read objects as StoreAble
     * @see TableInformation
     * @see StoreAble
     */
    TableWrapper(TableInformation information) {
        this.information = information;
    }

    /**
     * Returns the name of this table (must be constant)
     * @return Name of the table
     */
    @SuppressWarnings("WeakerAccess")
    public String getName() {
        return information.getName();
    }

    /**
     * Class that is stored within this table
     * @return Class of StoreAbles stored within this table
     */
    @SuppressWarnings("unused")
    public Class<?> getStorageClass() {
        return information.getStorageClass();
    }


    /**
     * Columns that this table consists out of
     * @return Columns of this table
     */
    @SuppressWarnings("WeakerAccess")
    public TableInformation.DbColumn[] getColumns() {
        return information.getColumns();
    }

    /**
     * Returns a SQLite command string that creates the structure of the table based upon the table information
     * @return SQL command that creates the table
     * @see TableInformation
     */
    String createTable() {
        StringBuilder builder = new StringBuilder();

        builder.append("CREATE TABLE ");
        builder.append(information.getName());
        builder.append(" (");
        final TableInformation.DbColumn[] columns = information.getColumns();
        for(int i = 0; i < columns.length; i++) {
            if(columns[i].getType() != ContentType.UNSUPPORTED) {
                builder.append(columns[i].getName());
                builder.append(" ");
                if(columns[i].getName().equals(StoreAble.ID)
                        && columns[i].getType().sqlType == ContentType.SQLiteType.INTEGER) {
                    builder.append("INTEGER PRIMARY KEY");
                } else {
                    builder.append(columns[i].getType().sqlType.name());
                }
                if(i != (columns.length-1)) {
                    builder.append(",");
                }
            }
        }

        builder.append(");");
        return builder.toString();
    }

    //endregion

    // region Getters
    /**
     * Reads a single element from the database with the matching id
     * @param id ID of the element, must be >= 0
     * @param database Database the element will be searched for
     * @return A StoreAble containing the data for the given ID or null if no element was found
     * @see StoreAble
     * @see TableInformation#restore(Cursor)
     */
    <S extends StoreAble> S get(Class<S> storeClass, long id, SQLiteDatabase database) {
        final DbComparison idComp = DbComparison.equalsId(id);
        Cursor cursor = database.query(information.getName(), null, idComp.where, idComp.array(), null, null, null);
        S element = null;
        if(cursor.moveToFirst()) {
            element = storeClass.cast(information.restore(cursor));
        }
        cursor.close();

        return element;
    }

    /**
     * Reads all elements contained in the table
     * @param <S> Class of the elements
     * @param storeClass Class object used to identify the database table and for casting
     * @param database Database the elements are stored in
     * @return All elements currently in the table as a list of objects of class S
     * @see StoreAble
     * @see TableWrapper#getList(Class, Cursor)
     */
    <S extends StoreAble> Collection<S> getAll(Class<S> storeClass, SQLiteDatabase database) {
        Cursor cursor = database.query(information.getName(), null, null, null, null, null, null);
        Collection<S> collection = getList(storeClass, cursor);
        cursor.close();
        return collection;
    }

    /**
     * Reads all elements contained in the table that match the given criteria.
     * @param database Database the elements are stored in
     * @param and True: All comparisons have to be fulfilled, False: Only one has to be fulfilled
     * @param comparisons Comparisons to filter the elements
     * @return A collection of all elements (as objects of class S) that have matching values
     * @see DbComparison
     * @see DbComparison.DbQuery
     * @see StoreAble
     * @see TableWrapper#getList(Class, Cursor)
     */
    <S extends StoreAble> Collection<S> getWhere(Class<S> storageClass, SQLiteDatabase database, boolean and, DbComparison... comparisons) {
        DbComparison.DbQuery mQuery = new DbComparison.DbQuery(and, comparisons);
        if(mQuery.isValid()) {
            Cursor cursor = database.query(information.getName(), null, mQuery.where, mQuery.values, null, null, null);
            Collection<S> elements = getList(storageClass, cursor);
            cursor.close();

            return elements;
        }
        return new ArrayList<>();
    }

    /**
     * Reads all elements contained within the cursor and returns them in the given list
     * @param cursor Cursor that has performed a database request
     * @return  list of all elements (as objects of class S)
     * @see TableInformation#restore(Cursor)
     */
    private <S extends StoreAble> Collection<S> getList(Class<S> storageClass, Cursor cursor) {
        ArrayList<S> list = new ArrayList<>();

        if(cursor.moveToFirst()) {
            do {
                list.add(storageClass.cast(information.restore(cursor)));
            } while(cursor.moveToNext());
        }
        return list;
    }

    // endregion

    // region Setter / Saver

    /**
     * Stores a StoreAble object within the database. If it has an ID, the existing data set will be updated,
     * otherwise a new set will be added and the new ID will be set in the StoreAble
     * @param element StoreAble that needs to be stored in the database
     * @param database Database where the StoreAble should be stored
     * @return True: The element was saved, False: The element could not be saved
     * @see TableWrapper#insert(StoreAble, SQLiteDatabase)
     * @see TableWrapper#update(StoreAble, SQLiteDatabase)
     */
    boolean save(StoreAble element, SQLiteDatabase database) {
        if(element != null) {
            if(element.getId() > StoreAble.INVALID_ID) {
                return update(element, database);
            } else {
                return insert(element, database);
            }
        }
        return false;
    }

    /**
     * Adds a StoreAble as a new dataset into the database.
     * Note: Do not call when object has a valid ID and already exists in database!
     * @param element Element to be stored
     * @param database Database for storage
     * @return True: The element was added, False: The element could not be added
     * @see TableInformation#store(StoreAble, String...)
     */
    private boolean insert(StoreAble element, SQLiteDatabase database) {
        final long id = database.insert(information.getName(), null, information.store(element, StoreAble.ID));
        element.setId(id);
        database.close();
        return id >= 0;
    }

    /**
     * Updates the dataset matching the ID of the given StoreAble in the database.s
     * Note: Do not call if the object has no ID and doesn't exist in the database.
     * @param element Element to be stored
     * @param database Database for storage
     * @return True: The element was updated, False: The element could not be updated
     * @see DbComparison#equalsId(StoreAble)
     * @see TableInformation#store(StoreAble, String...)
     */
    private boolean update(StoreAble element, SQLiteDatabase database) {
        final DbComparison idComp = DbComparison.equalsId(element);
        final int rows = database.update(information.getName(), information.store(element), idComp.where, idComp.array());
        database.close();
        return rows > 0;
    }

    /**
     * Stores all given StoreAbles within the database. Will also add IDs if necessary. Returns the stored objects in a new list.
     * @param elements List of StoreAbles that needs to be stored
     * @param database Database where the StoreAble should be stored
     * @return True: all elements were saved, False: Not all elements could be saved
     * @see TableWrapper#save(StoreAble, SQLiteDatabase)
     */
    boolean saveAll(Collection<?> elements, SQLiteDatabase database) {
        boolean saved = true;

        for(Object element : elements) {
            if(element instanceof StoreAble) {
                saved = save((StoreAble) element, database) && saved;
            }
        }

        return saved;
    }

    //endregion

    // region Other Queries
    /**
     * Removes a StoreAble from the database. If the StoreAble doesn't exist, no action will be performed.
     * @param element Element to be deleted from the database
     * @param database Database where the StoreAble is stored
     * @return True: Element was deleted, False: Element did not exist in database (no deletion necessary)
     * @see DbComparison#equalsId(StoreAble)
     */
    boolean delete(StoreAble element, SQLiteDatabase database) {
        int rows = 0;
        if(element.getId() > StoreAble.INVALID_ID) {
            final DbComparison idComp = DbComparison.equalsId(element);
            rows = database.delete(information.getName(), idComp.where, idComp.array());
        }
        database.close();

        return rows > 0;
    }

    /**
     * Returns the row count of the given table, aka the count of all StoreAble elements therein.
     * @param database Database that contains the table to be counted
     * @return Count of all elements in the table.
     */
    int count(SQLiteDatabase database) {
        Cursor cursor = database.query(information.getName(), null, null, null, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    // endregion
}

