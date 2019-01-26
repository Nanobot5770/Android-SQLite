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
import java.util.Collection;
import java.util.List;

/**
 * This class provides all the methods necessary to create, read and write StoreAbles in specific table in SQLite database.
 */
public class SQLiteTable {

    /**
     * String constant used to create comparisons for SQLite requests
     */
    private static final String EQUALS = "%s = ?";

    /**
     * String constant for comparisons for the ID field in SQLite requests
     * @see TableInformation.DatabaseField#ID
     */
    private static final String ID_EQUALS = String.format(EQUALS, TableInformation.DatabaseField.ID);

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

    public TableInformation.DatabaseField[] getFields() {
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
            builder.append(information.dbFields[i].fieldType);
            builder.append(" ");
            builder.append(information.dbFields[i].name);
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
     * @see TableInformation.DatabaseField
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
     * @return True: The element was saved, False: The element could not be saved
     */
    boolean save(StoreAble element, SQLiteDatabase database) {
        if(element != null) {
            if(element.getId() >= 0) {
                return update(element, -1, database);
            } else {
                return insert(element, -1, database);
            }
        }
        return false;
    }

    /**
     * NOTE: This function is used for nodes within a StoredCollection, other items use the regular save function!
     * Stores a StoreAble object within the database. If it has an ID, the existing data set will be updated,
     * otherwise a new set will be added and the new ID will be set in the StoreAble
     * @param element NodeElement from a collection that needs to be stored in the database
     * @param database Database where the StoreAble should be stored
     * @return True: The element was saved, False: The element could not be saved
     * @see SQLiteTable#save(StoreAble, SQLiteDatabase)
     */
    boolean save(StoredCollection.CollectionNode element, SQLiteDatabase database) {
        if(element != null) {
            if(element.getId() >= 0) {
                return update(element, element.getParentId(), database);
            } else {
                return insert(element,  element.getParentId(), database);
            }
        }
        return false;
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

    /**
     * NOTE: This function is used for nodes within a StoredCollection, other items use the regular save function!
     * Works similar to the regular saveAll, but focusses only on Node elements for CollictionStoreAbles.
     * @param collection Collection of Nodes, which should be stored
     * @param database Database where the StoreAble should be stored
     * @param <N> CollectionNode Class - Nodes within the given Parent
     * @return True: all elements were saved, False: Not all elements could be saved
     * @see SQLiteTable#saveAll(Collection, SQLiteDatabase)
     */
    <N extends StoredCollection.CollectionNode> boolean saveAll(StoredCollection<N> collection, SQLiteDatabase database) {
        boolean saved = true;

        for(N node : collection) {
            node.setParentId(collection.getId());
            saved = save(node, database) && saved;
        }

        return saved;
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
     * @param parentId ID of the Collection Parent, -1 if no parent present
     * @param database Database for storage
     * @return True: The element was added, False: The element could not be added
     */
    private boolean insert(StoreAble element, long parentId, SQLiteDatabase database) {
        final long id = database.insert(information.name, null, getStoreAbleValues(element, -1, parentId));
        element.setId(id);
        database.close();
        return id >= 0;
    }

    /**
     * Updates the dataset matching the ID of the given StoreAble in the database.
     * Note: Do not call if the object has no ID and doesn't exist in the database.
     * @param element Element to be stored
     * @param parentId ID of the Collection Parent, -1 if no parent present
     * @param database Database for storage
     * @return True: The element was updated, False: The element could not be updated
     */
    private boolean update(StoreAble element, long parentId, SQLiteDatabase database) {
        final int rows = database.update(information.name, getStoreAbleValues(element, element.getId(), parentId), ID_EQUALS, new String[]{String.valueOf(element.getId())});
        database.close();
        return rows > 0;
    }

    /**
     * Prepares the values of a StoreAble into a ContentValues container
     * @param element Element to be stored
     * @param elementId ID of the element, -1 if the field should be left empty
     * @param parentId ID of the parent, -1 if the field should be left empty
     * @return All storable data as ContentValues container
     * @see ContentValues
     */
    private static ContentValues getStoreAbleValues(StoreAble element, long elementId, long parentId) {
        ContentValues values = new ContentValues();
        element.exportToDatabase(values);

        if(elementId >= 0) {
            values.put(TableInformation.DatabaseField.ID, elementId);
        }

        if(parentId >= 0) {
            values.put(TableInformation.DatabaseField.PARENT_ID, parentId);
        }

        return values;
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
        element.setId(cursor.getLong(cursor.getColumnIndex(TableInformation.DatabaseField.ID)));
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

