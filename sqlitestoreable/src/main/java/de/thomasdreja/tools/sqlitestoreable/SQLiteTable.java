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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides all the methods necessary to create, read and write StoreAbles in specific table in SQLite database.
 */
public class SQLiteTable {

    /**
     * This class is used to provide the type specific information to the SQLiteTable.
     * It names the table, provides a list of fields and it handles the creation of new StoreAble objects.
     */
    public static abstract class TableInformation {

        /**
         * DatabaseFields within the table
         */
        private final DatabaseField[] dbFields;

        /**
         * Name of the table, always set to the class name of the stored objects
         */
        private final String name;

        /**
         * Class Object of the objects to be stored in the table
         */
        final Class<? extends StoreAble> storageClass;

        /**
         * Creates a new TableInformation object for the given StoreAble class with the given database fields
         * This in turn is used to create a matching table in the database
         * @param storageClass Class of the Object to be stored into the table
         * @param databaseFields Fields to store the information of the object (if no fields are given, the database can only store the ID)
         * @see DatabaseField
         * @see SQLiteTable#SQLiteTable(TableInformation)
         */
        public TableInformation(Class<? extends StoreAble> storageClass, DatabaseField... databaseFields) {
            this.storageClass = storageClass;
            this.name = storageClass.getSimpleName();

            if(databaseFields == null) {
                databaseFields = new DatabaseField[0];
            }

            dbFields = new DatabaseField[databaseFields.length+1];
            dbFields[0] = DatabaseField.FIELD_ID;
            System.arraycopy(databaseFields, 0, dbFields, 1, databaseFields.length);
        }

        /**
         * Returns a new object based on the values from the database, stored within the given cursor.
         * The cursor will already point to the correct element, only the current row needs to be read.
         * @param cursor Cursor that contains values for 1 new element from the database
         * @param storageClass Requested Class container for the object to be created. Use for safety checks!
         * @param <S> Class of the object
         * @return A object of class S that contains the values from the cursor
         * @see SQLiteTable#get(long, SQLiteDatabase, Class)
         * @see SQLiteTable#getAll(SQLiteDatabase, Class)
         * @see SQLiteTable#getWhere(SQLiteDatabase, String, String, Class)
         * @see SQLiteTable#readNewElement(Cursor, Class)
         */
        <S extends StoreAble> S read(Cursor cursor, Class<S> storageClass) {
            StoreAble result = read(cursor);
            if(storageClass.isInstance(result)) {
                return storageClass.cast(read(cursor));
            }
            return null;
        }

        /**
         * Returns a new object based on the values from the database, stored within the given cursor.
         * The cursor will already point to the correct element, only the current row needs to be read.
         * @param cursor Cursor that contains values for 1 new element from the database
         * @return A StoreAble that contains the values from the cursor
         * @see SQLiteTable#get(long, SQLiteDatabase, Class)
         * @see SQLiteTable#getAll(SQLiteDatabase, Class)
         * @see SQLiteTable#getWhere(SQLiteDatabase, String, String, Class)
         * @see SQLiteTable#readNewElement(Cursor, Class)
         */
        protected abstract StoreAble read(Cursor cursor);
    }

    /**
     * This class represents a field within a SQLite table. Each field has a name as identifier and a fixed field type.
     * The static construction method ensures that only valid types are available
     */
    public static class DatabaseField {

        /**
         * Name identifier of the database field.
         * Must be unique per table!
         */
        public final String name;

        /**
         * Type of the database field. Can only be one of a fixed set of SQLite types!
         */
        public final String fieldType;

        /**
         * Creates a new DatabaseField with the given name and type parameters.
         * Note: This constructor is private and should only be accessed with a fixed type in mind!
         * @param name Name identifier of the database field
         * @param type Type of the database field (must be a valid SQLite type)
         */
        private DatabaseField(String name, String type) {
            this.name = name;
            this.fieldType = type;
        }

        /**
         * Name of the ID database field. Each StoreAble SQLiteTable will have this field automatically!
         * @see SQLiteTable
         * @see StoreAble
         */
        static final String ID = "ID";

        /**
         * Constant DatabaseField for the ID in the table. Each StoreAble SQLiteTable will have this field automatically!
         * This field will also serve as primary key for the table.
         * @see SQLiteTable
         * @see StoreAble
         */
        static final DatabaseField FIELD_ID = new DatabaseField("INTEGER PRIMARY KEY", ID);

        /**
         * Creates a new field that will contain strings
         * @param name Name of the field
         * @return A new field with the given name and the string type
         */
        public static DatabaseField newStringField(String name) {
            return new DatabaseField("TEXT", name);
        }

        /**
         * Creates a new field that will contain integers or longs
         * @param name Name of the field
         * @return A new field with the given name and integer/long type
         */
        public static DatabaseField newIntField(String name) {
                return new DatabaseField("INTEGER", name);
        }

        /**
         * Creates a new field that will contain floats or doubles
         * @param name Name of the field
         * @return A new field with the given name and float or double type
         */
        public static DatabaseField newFloatField(String name) {
            return new DatabaseField("REAL", name);
        }

        /**
         * Creates a new field that will contain binary data, useful for object serialisation.
         * @param name Name of the field
         * @return A new field with the given name and binary data type
         * @see java.io.Serializable
         */
        public static DatabaseField newBinaryField(String name) {
            return new DatabaseField("BLOB", name);
        }
    }

    /**
     * String constant used to create comparisons for SQLite requests
     */
    private static final String EQUALS = "%s = ?";

    /**
     * String constant for comparisons for the ID field in SQLite requests
     * @see DatabaseField#ID
     */
    private static final String ID_EQUALS = String.format(EQUALS, DatabaseField.ID);

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

    public Class<? extends StoreAble> getStorageClass() {
        return information.storageClass;
    }

    public DatabaseField[] getFields() {
        return information.dbFields;
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
        for(int i = 0; i < information.dbFields.length; i++) {
            builder.append(information.dbFields[i].name);
            builder.append(" ");
            builder.append(information.dbFields[i].fieldType);
            if(i != (information.dbFields.length-1)) {
                builder.append(",");
            }
        }

        builder.append(");");
        return builder.toString();
    }

    /**
     * Reads a single element from the database with the matching id
     * @param id ID of the element, must be >= 0
     * @param database Database the element will be searched for
     * @return A StoreAble containing the data for the given ID or null if no element was found
     * @see StoreAble
     */
    <S extends StoreAble> S get(long id, SQLiteDatabase database, Class<S> storeClass) {
        Cursor cursor = database.query(information.name, null, ID_EQUALS, new String[]{String.valueOf(id)}, null, null, null);
        S element = null;
        if(cursor.moveToFirst()) {
            element = readNewElement(cursor, storeClass);
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
     * @see SQLiteTable#readList(Cursor, Class)
     */
    <S extends StoreAble> List<S> getAll(SQLiteDatabase database, Class<S> storeClass) {
        Cursor cursor = database.query(information.name, null, null, null, null, null, null);
        ArrayList<S> elements = readList(cursor, storeClass);
        cursor.close();

        return elements;
    }

    /**
     * Reads all elements contained in the table that match the given criteria.
     * @param <S> Class of the elements
     * @param database Database the elements are stored in
     * @param field Name of the database field that will be compared
     * @param value Value of the database field that needs to be matched
     * @param storeClass Class object used to identify the database table and for casting
     * @return A list of all elements (as objects of class S) that have a matching value in their given field
     * @see DatabaseField
     * @see StoreAble
     * @see SQLiteTable#readList(Cursor, Class)
     */
    <S extends StoreAble> List<S> getWhere(SQLiteDatabase database, String field, String value, Class<S> storeClass) {
        Cursor cursor = database.query(information.name, null, String.format(EQUALS, field), new String[]{value}, null, null, null);
        ArrayList<S> elements = readList(cursor, storeClass);
        cursor.close();

        return elements;
    }

    /**
     * Reads all elements contained within the cursor and returns them in the given list
     * @param <S> Class of the elements
     * @param cursor Cursor that has performed a database request
     * @param storeClass Class object used to identify the database table and for casting
     * @return  list of all elements (as objects of class S)
     * @see SQLiteTable#readNewElement(Cursor, Class)
     * @see TableInformation#read(Cursor, Class)
     */
    private <S extends StoreAble> ArrayList<S> readList(Cursor cursor, Class<S> storeClass) {
        ArrayList<S> list = new ArrayList<>();

        if(cursor.moveToFirst()) {
            do {
                list.add(readNewElement(cursor, storeClass));
            } while(cursor.moveToNext());
        }
        return list;
    }

    /**
     * Stores a StoreAble object within the database. If it has an ID, the existing data set will be updated,
     * otherwise a new set will be added and the new ID will be set in the StoreAble
     * @param element StoreAble that needs to be stored in the database
     * @param database Database where the StoreAble should be stored
     * @return The given StoreAble with a valid database ID (if not already present)
     */
    <S extends StoreAble> S save(StoreAble element, SQLiteDatabase database, Class<S> storeClass) {
        if(storeClass.isInstance(element)) {
            S mElement = storeClass.cast(element);

            if(element.getId() >= 0) {
                return update(mElement, database);
            } else {
                return insert(mElement, database);
            }
        }

        return null;
    }

    /**
     * Stores all given StoreAbles within the database. Will also add IDs if necessary. Returns the stored objects in a new list.
     * @param elements List of StoreAbles that needs to be stored
     * @param database Database where the StoreAble should be stored
     * @return A new list with the given StoreAbles stored in the database and their IDs updated if necessary
     * @see SQLiteTable#save(StoreAble, SQLiteDatabase, Class)
     */
    <S extends StoreAble> List<S> saveAll(List<S> elements, SQLiteDatabase database, Class<S> storeClass) {
        ArrayList<S> results = new ArrayList<>();

        for(S element : elements) {
            results.add(save(element, database, storeClass));
        }

        return results;
    }

    /**
     * Stores all given StoreAbles within the database. Will also add IDs if necessary. Returns the stored objects in the same array.
     * @param elements Array of StoreAbles that needs to be stored
     * @param database Database where the StoreAble should be stored
     * @return The given array with its content stored to the database and IDs updated if necessary
     * @see SQLiteTable#save(StoreAble, SQLiteDatabase, Class)
     */
    <S extends StoreAble> S[] saveAll(S[] elements, SQLiteDatabase database, Class<S> storeClass) {
        for(int i=0; i < elements.length; i++) {
            elements[i] = save(elements[i], database, storeClass);
        }
        return elements;
    }

    /**
     * Removes a StoreAble from the database. If the StoreAble doesn't exist, no action will be performed.
     * @param element Element to be deleted from the database
     * @param database Database where the StoreAble is stored
     * @return True: Element was deleted, False: Element did not exist in database (no deletion necessary)
     */
    boolean delete(StoreAble element, SQLiteDatabase database) {
        int rows = 0;
        if(element.getId() >= 0) {
            rows = database.delete(information.name, ID_EQUALS, new String[]{String.valueOf(element.getId())});
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
        Cursor cursor = database.query(information.name, null, null, null, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    /**
     * Adds a StoreAble as a new dataset into the database.
     * Note: Do not call when object has a valid ID and already exists in database!
     * @param element Element to be stored
     * @param database Database for storage
     * @param <S> Class of the element
     * @return Element with the new database ID set
     */
    private <S extends StoreAble> S insert(S element, SQLiteDatabase database) {
        ContentValues values = new ContentValues();
        element.exportToDatabase(values);
        long id = database.insert(information.name, null, values);
        element.setId(id);
        database.close();

        return element;
    }

    /**
     * Updates the dataset matching the ID of the given StoreAble in the database.
     * Note: Do not call if the object has no ID and doesn't exist in the database.
     * @param element Element to be stored
     * @param database Database for storage
     * @param <S> Class of the element
     * @return Element with the new database ID set
     */
    private <S extends StoreAble> S update(S element, SQLiteDatabase database) {
        ContentValues values = new ContentValues();
        element.exportToDatabase(values);
        values.put(DatabaseField.ID, element.getId());
        database.update(information.name, values, ID_EQUALS, new String[]{String.valueOf(element.getId())});
        database.close();

        return element;
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
    private <S extends StoreAble> S readNewElement(Cursor cursor, Class<S> storageClass) {
        S element = information.read(cursor, storageClass);
        element.setId(cursor.getLong(cursor.getColumnIndex(DatabaseField.ID)));
        return element;
    }

    /**
     * Stores the given object as binary blob into the given field of the contentvalues store.
     * @param field Field where the object will be stored
     * @param mObject Object to be stored (must be serializable)
     * @param content ContentValues Container for storage
     * @param <P> Class of the object to be stored
     * @return True: Object was stored as serialized byte array, False: Object could not be serialized
     */
    public static <P extends Serializable> boolean serializeToDatabase(String field, P mObject, ContentValues content) {
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        try {
            ObjectOutputStream objectStream = new ObjectOutputStream(byteOutput);
            objectStream.writeObject(mObject);
            content.put(field, byteOutput.toByteArray());
            byteOutput.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Reads the given field from the database and attempts to deserialize the byte array into an object of the given class
     * @param field Field where the object was stored
     * @param cursor Database Cursor that contains the field data
     * @param objectClass Class of the object to be deserialized
     * @param <P>  Class of the object to be stored
     * @return Object of given Class from the database cursor
     */
    public static <P extends Serializable> P deserializeFromDatabase(String field, Cursor cursor, Class<P> objectClass) {
        try {
            ByteArrayInputStream byteInput = new ByteArrayInputStream(cursor.getBlob(cursor.getColumnIndex(field)));
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
}

