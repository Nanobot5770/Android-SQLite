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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * This class provides all the methods necessary to create, read and write StoreAbles in specific table in SQLite database.
 */
public class SQLiteTable {
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
    SQLiteTable(TableInformation information) {
        this.information = information;
    }

    /**
     * Returns the name of this table (must be constant)
     * @return Name of the table
     */
    public String getName() {
        return information.name;
    }

    /**
     * Class that is stored within this table
     * @return Class of StoreAbles stored within this table
     */
    public Class<? extends StoreAble> getStorageClass() {
        return information.storageClass;
    }


    /**
     * Columns that this table consists out of
     * @return Columns of this table
     */
    public DatabaseColumn[] getColumns() {
        return information.dbColumns;
    }

    /**
     * Returns a SQLite command string that creates the structure of the table based upon the table information
     * @return SQL command that creates the table
     * @see TableInformation
     */
    String createTable() {
        StringBuilder builder = new StringBuilder();

        builder.append("CREATE TABLE ");
        builder.append(information.name);
        builder.append(" (");
        for(int i = 0; i < information.dbColumns.length; i++) {
            builder.append(information.dbColumns[i].name);
            builder.append(" ");
            builder.append(information.dbColumns[i].columnType);
            if(i != (information.dbColumns.length-1)) {
                builder.append(",");
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
     */
    <S extends StoreAble> S get(long id, SQLiteDatabase database, Class<S> storeClass) {
        Cursor cursor = database.query(information.name, null, DatabaseColumn.COLUMN_ID.where(DatabaseColumn.CompareOperation.EQUAL), new String[]{String.valueOf(id)}, null, null, null);
        S element = null;
        if(cursor.moveToFirst()) {
            element = getNewElement(cursor, storeClass);
        }
        cursor.close();

        return element;
    }

    /**
     * Reads all elements contained in the table
     * @param <S> Class of the elements
     * @param database Database the elements are stored in
     * @param storeClass Class object used to identify the database table and for casting
     * @return All elements currently in the table as a list of objects of class S
     * @see StoreAble
     * @see SQLiteTable#getList(Cursor, Class)
     */
    <S extends StoreAble> Collection<S> getAll(SQLiteDatabase database, Class<S> storeClass) {
        Cursor cursor = database.query(information.name, null, null, null, null, null, null);
        Collection<S> elements = getList(cursor, storeClass);
        cursor.close();

        return elements;
    }

    /**
     * Reads all elements contained in the table that match the given criteria.
     * @param database Database the elements are stored in
     * @param column Name of the database column that will be compared
     * @param comparison Which type of comparison should be used?
     * @param value Value to be compared to the column
     * @param storeClass Class object used to identify the database table and for casting
     * @param <S> Class of the elements
     * @return A collection of all elements (as objects of class S) that have a matching value in their given column
     * @see DatabaseColumn
     * @see StoreAble
     * @see SQLiteTable#getList(Cursor, Class)
     */
    <S extends StoreAble> Collection<S> getWhere(SQLiteDatabase database, DatabaseColumn column, DatabaseColumn.CompareOperation comparison, String value, Class<S> storeClass) {
        Cursor cursor = database.query(information.name, null, column.where(comparison), new String[]{value}, null, null, null);
        Collection<S> elements = getList(cursor, storeClass);
        cursor.close();

        return elements;
    }

    /**
     * Reads all elements contained in the table that have a matching related ID
     * @param database Database the elements are stored in
     * @param relatedID ID of the related StoreAble (parent)
     * @param storeClass Class object used to identify the database table and for casting
     * @param <S> Class of the elements
     * @return A collection of all elements (as objects of class S) that have a matching related ID
     * @see StoreAble#getRelatedId()
     * @see SQLiteTable#getWhere(SQLiteDatabase, DatabaseColumn, DatabaseColumn.CompareOperation, String, Class)
     */
    <S extends StoreAble> Collection<S> getAllRelated(SQLiteDatabase database, long relatedID, Class<S> storeClass) {
        return getWhere(database, DatabaseColumn.COLUMN_RELATED_ID, DatabaseColumn.CompareOperation.EQUAL, String.valueOf(relatedID), storeClass);
    }

    /**
     * Reads all elements contained within the cursor and returns them in the given list
     * @param <S> Class of the elements
     * @param cursor Cursor that has performed a database request
     * @param storeClass Class object used to identify the database table and for casting
     * @return  list of all elements (as objects of class S)
     * @see SQLiteTable#getNewElement(Cursor, Class)
     * @see TableInformation#read(Cursor, Class)
     */
    private <S extends StoreAble> Collection<S> getList(Cursor cursor, Class<S> storeClass) {
        ArrayList<S> list = new ArrayList<>();

        if(cursor.moveToFirst()) {
            do {
                list.add(getNewElement(cursor, storeClass));
            } while(cursor.moveToNext());
        }
        return list;
    }

    /**
     * Reads a new element from the given cursor with the given class.
     * It uses the read function of the internal TableInformation to create the object.
     * Then the ID is read from the cursor and set for the object
     * @param cursor Cursor that contains a row of data from a database query
     * @param storageClass Class container of the new object
     * @param <S> Class of the object
     * @return A new object with the given class and a valid database ID
     */
    private <S extends StoreAble> S getNewElement(Cursor cursor, Class<S> storageClass) {
        S element = information.read(cursor, storageClass);
        element.setId(cursor.getLong(cursor.getColumnIndex(DatabaseColumn.COLUMN_ID.name)));
        element.setRelatedId(cursor.getLong(cursor.getColumnIndex(DatabaseColumn.COLUMN_RELATED_ID.name)));
        return element;
    }

    // endregion

    // region Setter / Saver

    /**
     * Stores a StoreAble object within the database. If it has an ID, the existing data set will be updated,
     * otherwise a new set will be added and the new ID will be set in the StoreAble
     * @param element StoreAble that needs to be stored in the database
     * @param database Database where the StoreAble should be stored
     * @return True: The element was saved, False: The element could not be saved
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
     */
    private boolean insert(StoreAble element, SQLiteDatabase database) {
        final long id = database.insert(information.name, null, getStoreAbleValues(element));
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
     */
    private boolean update(StoreAble element, SQLiteDatabase database) {
        final int rows = database.update(information.name, getStoreAbleValues(element), DatabaseColumn.COLUMN_ID.where(DatabaseColumn.CompareOperation.EQUAL), new String[]{String.valueOf(element.getId())});
        database.close();
        return rows > 0;
    }

    /**
     * Prepares the values of a StoreAble into a ContentValues container
     * @param element Element to be stored
     * @return All storable data as ContentValues container
     * @see ContentValues
     */
    private static ContentValues getStoreAbleValues(StoreAble element) {
        ContentValues values = new ContentValues();
        element.exportToDatabase(values);

        if(element.getId() > StoreAble.INVALID_ID) {
            values.put(DatabaseColumn.COLUMN_ID.name, element.getId());
        }

        values.put(DatabaseColumn.COLUMN_RELATED_ID.name, element.getRelatedId());

        return values;
    }

    /**
     * Stores all given StoreAbles within the database. Will also add IDs if necessary. Returns the stored objects in a new list.
     * @param elements List of StoreAbles that needs to be stored
     * @param database Database where the StoreAble should be stored
     * @return True: all elements were saved, False: Not all elements could be saved
     * @see SQLiteTable#save(StoreAble, SQLiteDatabase)
     */
    <S extends StoreAble> boolean saveAll(Collection<S> elements, SQLiteDatabase database) {
        boolean saved = true;

        for(S element : elements) {
            saved = save(element, database) && saved;
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
     */
    boolean delete(StoreAble element, SQLiteDatabase database) {
        int rows = 0;
        if(element.getId() > StoreAble.INVALID_ID) {
            rows = database.delete(information.name, DatabaseColumn.COLUMN_ID.where(DatabaseColumn.CompareOperation.EQUAL), new String[]{String.valueOf(element.getId())});
        }
        database.close();

        return rows > 0;
    }

    /**
     * Removes all elements from the database that match the given criteria.
     * @param column Name of the database column that will be compared
     * @param operation Which type of comparison should be used?
     * @param value Value to be compared to the column
     * @param database Database where the StoreAbles are stored
     * @return The number of rows affected
     */
    int deleteWhere(DatabaseColumn column, DatabaseColumn.CompareOperation operation, String value, SQLiteDatabase database) {
        int rows = database.delete(information.name, column.where(operation), new String[]{value});
        database.close();
        return rows;
    }

    /**
     * Returns the row count of the given table, aka the count of all StoreAble elements therein.
     * @param database Database that contains the table to be counted
     * @return Count of all elements in the table.
     */
    int count(SQLiteDatabase database) {
        Cursor cursor = database.query(information.name, null, null, null, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    // endregion

    // region Helper Functions
    /**
     * Stores the given object as binary blob into the given field of the contentvalues store.
     * @param column Column where the object was stored
     * @param mObject Object to be stored (must be serializable)
     * @param content ContentValues Container for storage
     * @param <P> Class of the object to be stored
     * @return True: Object was stored as serialized byte array, False: Object could not be serialized
     */
    public static <P extends Serializable> boolean serializeToDatabase(DatabaseColumn column, P mObject, ContentValues content) {
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        try {
            ObjectOutputStream objectStream = new ObjectOutputStream(byteOutput);
            objectStream.writeObject(mObject);
            content.put(column.name, byteOutput.toByteArray());
            byteOutput.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Reads the given field from the database and attempts to deserialize the byte array into an object of the given class
     * @param column Column where the object was stored
     * @param cursor Database Cursor that contains the field data
     * @param objectClass Class of the object to be deserialized
     * @param <P>  Class of the object to be stored
     * @return Object of given Class from the database cursor
     */
    public static <P extends Serializable> P deserializeFromDatabase(DatabaseColumn column, Cursor cursor, Class<P> objectClass) {
        try {
            ByteArrayInputStream byteInput = new ByteArrayInputStream(cursor.getBlob(cursor.getColumnIndex(column.name)));
            ObjectInputStream objectStream = new ObjectInputStream(byteInput);
            Object object = objectStream.readObject();
            objectStream.close();

            if(objectClass.isInstance(object)) {
                return objectClass.cast(object);
            }
        } catch (ClassNotFoundException | IOException ignored) {
        }
        return null;
    }

    // endregion
}

