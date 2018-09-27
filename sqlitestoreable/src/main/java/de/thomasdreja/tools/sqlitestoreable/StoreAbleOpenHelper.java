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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * This class extends the Android SQLiteOpenHelper to allow for direct integration with the StoreAble system.
 * It provides all tools to create, read and write a database with one or more SQLiteTables
 * @see SQLiteTable
 * @see StoreAble
 */
public class StoreAbleOpenHelper extends SQLiteOpenHelper {

    /**
     * Command used to delete the given table on version upgrade
     */
    protected static final String DROP_TABLE_IF = "DROP TABLE IF EXISTS %s";

    /**
     * Hashmap containing all tables of the database, indexed by table name
     */
    protected final HashMap<Class<? extends StoreAble>,SQLiteTable> tableMap;

    /**
     * Creates a new database based on the application context, name and version.
     * Also fills in all SQLiteTables based upon the given helpers.
     * @param context Application context for the database
     * @param name Name of the database
     * @param version Version of the database - Increasing it will drop all existing elements!
     * @param tables Table information for creating all tables in the database
     * @see SQLiteTable.TableInformation
     * @see SQLiteTable
     */
    public StoreAbleOpenHelper(Context context, String name, int version, SQLiteTable.TableInformation... tables) {
        super(context, name, null, version);
        tableMap = new HashMap<>();

        for (SQLiteTable.TableInformation helper : tables) {
            tableMap.put(helper.storageClass, new SQLiteTable(helper));
        }
    }


    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        for(SQLiteTable table : tableMap.values()) {
            sqLiteDatabase.execSQL(table.createTable());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        for(SQLiteTable table : tableMap.values()) {
            sqLiteDatabase.execSQL(String.format(DROP_TABLE_IF, table));
            sqLiteDatabase.execSQL(table.createTable());
        }
    }

    /**
     * Returns the names of all tables in the database
     * @return Set with all Class objects used to identify the database tables
     */
    public Set<Class<? extends StoreAble>> getAllTableClasses() {
        return tableMap.keySet();
    }

    /**
     * Returns whether the database has the given table or not
     * @param storageClass Class object used to identify the database table
     * @return True: Table exists in database, False: Table not found
     */
    public boolean hasTable(Class<? extends StoreAble> storageClass) {
        return tableMap.containsKey(storageClass);
    }

    public SQLiteTable getTableFor(Class<? extends StoreAble> storageClass) {
        return tableMap.get(storageClass);
    }

    /**
     * Reads a single element from the table for the given class with the matching id
     * @param id ID of the element
     * @param storageClass Class object used to identify the database table and for casting
     * @param <S> Class of the element
     * @return Element as object of given class with the matching id
     * @see SQLiteTable#get(long, SQLiteDatabase, Class)
     */
    public <S extends StoreAble> S get(long id, Class<S> storageClass) {
        if(tableMap.containsKey(storageClass)) {
            return tableMap.get(storageClass).get(id, getReadableDatabase(), storageClass);
        }
        return null;
    }

    /**
     * Stores a StoreAble object within the database. If it has an ID, the existing data set will be updated,
     * otherwise a new set will be added and the new ID will be set in the StoreAble
     * @param element  Element to be saved into the database
     * @param storageClass Class object used to identify the database table and for casting
     * @param <S> Class of the element
     * @return The given StoreAble with a valid database ID (if not already present)
     * @see SQLiteTable#save(StoreAble, SQLiteDatabase, Class)
     */
    public <S extends StoreAble> S save(StoreAble element, Class<S> storageClass) {
        if(tableMap.containsKey(storageClass)) {
            return tableMap.get(storageClass).save(element, getWritableDatabase(), storageClass);
        }
        return null;
    }

    /**
     * Removes a StoreAble from the database. If the StoreAble doesn't exist, no action will be performed.
     * @param element Element to be deleted from the database
     * @param storageClass Class object used to identify the database table and for casting
     * @param <S> Class of the element
     * @return True: Element was deleted, False: Element did not exist in database (no deletion necessary)
     * @see SQLiteTable#delete(StoreAble, SQLiteDatabase)
     */
    public <S extends StoreAble> boolean delete(StoreAble element, Class<S> storageClass) {
        return tableMap.containsKey(storageClass) && storageClass.isInstance(element) && tableMap.get(storageClass).delete(element, getWritableDatabase());
    }

    /**
     * Reads all elements contained in the table for the given class
     * @param storageClass Class object used to identify the database table and for casting
     * @param <S> Class of the element
     * @return All elements currently in the table as objects of class S
     * @see SQLiteTable#getAll(SQLiteDatabase, Class)
     */
    public <S extends StoreAble> List<S> getAll(Class<S> storageClass) {
        if(tableMap.containsKey(storageClass)) {
            return tableMap.get(storageClass).getAll(getReadableDatabase(), storageClass);
        }
        return null;
    }

    /**
     * Reads all elements contained in the table for the given class that match the given criteria.
     * @param field Name of the database field that will be compared
     * @param value Value of the database field that needs to be matched
     * @param storageClass Class object used to identify the database table and for casting
     * @param <S> Class of the element
     * @return A list of all elements that have a matching value in their given field
     * @see SQLiteTable#getWhere(SQLiteDatabase, String, String, Class)
     */
    public <S extends StoreAble> List<S> getWhere(String field, String value, Class<S> storageClass) {
        if(tableMap.containsKey(storageClass)) {
            return tableMap.get(storageClass).getWhere(getReadableDatabase(), field, value, storageClass);
        }
        return null;
    }

    /**
     * Returns the row count of the given table, aka the count of all StoreAble elements therein.
     * @param storageClass Class object used to identify the database table and for casting
     * @param <S> Class of the element
     * @return Count of all elements in the table.
     * @see SQLiteTable#count(SQLiteDatabase)
     */
    public <S extends StoreAble> int count(Class<S> storageClass) {
        if(tableMap.containsKey(storageClass)) {
            return tableMap.get(storageClass).count(getReadableDatabase());
        }
        return -1;
    }

    /**
     * Stores all given StoreAbles within the database. Will also add IDs if necessary. Returns the stored objects in a new list.
     * @param elements List of StoreAbles that needs to be stored
     * @param storageClass Class object used to identify the database table and for casting
     * @param <S> Class of the element
     * @return A new list with the given StoreAbles stored in the database and their IDs updated if necessary
     * @see SQLiteTable#saveAll(List, SQLiteDatabase, Class)
     */
    public <S extends StoreAble> List<S> saveAll(List<S> elements, Class<S> storageClass) {
        if(tableMap.containsKey(storageClass)) {
            return tableMap.get(storageClass).saveAll(elements, getWritableDatabase(), storageClass);
        }
        return elements;
    }

    /**
     * Stores all given StoreAbles within the database. Will also add IDs if necessary. Returns the stored objects in the same array.
     * @param elements Array of StoreAbles that needs to be stored
     * @param storageClass Class object used to identify the database table and for casting
     * @param <S> Class of the element
     * @return The given array with its content stored to the database and IDs updated if necessary
     * @see SQLiteTable#saveAll(StoreAble[], SQLiteDatabase, Class)
     */
    public <S extends StoreAble> S[] saveAll(S[] elements, Class<S> storageClass) {
        if(tableMap.containsKey(storageClass)) {
            return tableMap.get(storageClass).saveAll(elements, getWritableDatabase(), storageClass);
        }
        return elements;
    }
}
