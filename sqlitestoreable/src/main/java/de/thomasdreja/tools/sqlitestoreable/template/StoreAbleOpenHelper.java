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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

/**
 * This class extends the Android SQLiteOpenHelper to allow for direct integration with the StoreAble system.
 * It provides all tools to create, read and write a database with one or more SQLiteTables
 * @see SQLiteTable
 * @see StoreAble
 */
public class StoreAbleOpenHelper extends SQLiteOpenHelper {

    //region Variables and Constructors

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
     * @see TableInformation
     * @see SQLiteTable
     */
    public StoreAbleOpenHelper(Context context, String name, int version, TableInformation... tables) {
        super(context, name, null, version);
        tableMap = new HashMap<>();

        for (TableInformation helper : tables) {
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

    /**
     *
     * @param storageClass Class object used to identify the database table
     * @return
     */
    public SQLiteTable getTableFor(Class<? extends StoreAble> storageClass) {
        return tableMap.get(storageClass);
    }

    /**
     *
     * @param storageClass Class object used to identify the database table
     * @return
     */
    public DatabaseColumn[] getColumnsFor(Class<? extends StoreAble> storageClass) {
        final SQLiteTable table = tableMap.get(storageClass);
        if(table != null) {
            return table.getColumns();
        }
        return new DatabaseColumn[0];
    }

    // endregion

    // region Getter
    /**
     * Reads a single element from the table for the given class with the matching id
     * @param id ID of the element
     * @param storageClass Class object used to identify the database table and for casting
     * @param <S> Class of the element
     * @return Element as object of given class with the matching id
     * @see SQLiteTable#get(long, SQLiteDatabase, Class)
     */
    public <S extends StoreAble> S get(long id, Class<S> storageClass) {
        final SQLiteTable table = tableMap.get(storageClass);
        if(table != null) {
            return table.get(id, getReadableDatabase(), storageClass);
        }
        return null;
    }

    /**
     * Reads all elements contained in the table for the given class
     * @param storageClass Class object used to identify the database table and for casting
     * @param <S> Class of the element
     * @return A collection of all elements currently in the table as objects of class S
     * @see SQLiteTable#getAll(SQLiteDatabase, Class)
     */
    public <S extends StoreAble> Collection<S> getAll(Class<S> storageClass) {
        final SQLiteTable table = tableMap.get(storageClass);
        if(table != null) {
            return table.getAll(getReadableDatabase(), storageClass);
        }
        return new ArrayList<>();
    }

    /**
     *
     * @param column Column in table that will be compared
     * @param comparison Which type of comparison should be used?
     * @param value Value to be compared to the column
     * @param storageClass Class object used to identify the database table and for casting
     * @param <S> Class of the element
     * @return A collection of all elements matching the query in the table as objects of the given class
     * @see SQLiteTable#getWhere(SQLiteDatabase, DatabaseColumn, DatabaseColumn.CompareOperation, String, Class)
     */
    public <S extends StoreAble> Collection<S> getWhere(DatabaseColumn column, DatabaseColumn.CompareOperation comparison, String value, Class<S> storageClass) {
        final SQLiteTable table = tableMap.get(storageClass);
        if(table != null) {
            return table.getWhere(getReadableDatabase(), column, comparison, value, storageClass);
        }
        return new ArrayList<>();
    }

    // endregion

    // region Collection Getter

    /**
     * Reads all related elements from the database the fit into the given collection.
     * @param collection Collection to be filled with related elements
     * @param relatedClass Related Class object used to identify the database table and for casting
     * @param <S> Related Element Class
     * @param <C> Collection Class
     * @return The given collection with all related elements add to it
     * @see SQLiteTable#getAllRelated(SQLiteDatabase, long, Class)
     */
    private <S extends StoreAble, C extends StoreAbleCollection<S>> C fillCollection(C collection, Class<S> relatedClass) {
        final SQLiteTable table = getTableFor(relatedClass);
        if(collection != null && table != null) {
            collection.setCollection(table.getAllRelated(getReadableDatabase(), collection.getRelatedId(), relatedClass));
        }
        return collection;
    }

    /**
     * Reads a single element from the table for the given class with the matching id. Also fills in all related elements to the collection.
     * @param id ID of the collection
     * @param collectionClass Collection Class object used to identify the database table and for casting
     * @param relatedClass Related Class object used to identify the database table and for casting
     * @param <S> Related Element Class
     * @param <C> Collection Class
     * @return Element as object of given class with the matching id, with all related elements
     * @see SQLiteTable#get(long, SQLiteDatabase, Class)
     */
    public <S extends StoreAble, C extends StoreAbleCollection<S>> C get(long id, Class<C> collectionClass, Class<S> relatedClass) {
        return fillCollection(get(id, collectionClass), relatedClass);
    }

    /**
     * Reads all elements contained in the table for the given class. Also fills in all related elements to the collection.
     * @param collectionClass Collection Class object used to identify the database table and for casting
     * @param relatedClass Related Class object used to identify the database table and for casting
     * @param <S> Related Element Class
     * @param <C> Collection Class
     * @return A collection of all elements currently in the table as objects of class S, with all related elements
     * @see SQLiteTable#getAll(SQLiteDatabase, Class)
     */
    public <S extends StoreAble, C extends StoreAbleCollection<S>> Collection<C> getAll(Class<C> collectionClass, Class<S> relatedClass) {
        Collection<C> allCollections = getAll(collectionClass);
        for(C element : allCollections) {
            fillCollection(element, relatedClass);
        }
        return allCollections;
    }

    /**
     * Reads all elements contained in the table for the given class that match the given criteria. Also fills in all related elements to the collection.
     * @param column Column in table that will be compared
     * @param comparison Which type of comparison should be used?
     * @param value Value to be compared to the column
     * @param collectionClass Collection Class object used to identify the database table and for casting
     * @param relatedClass Related Class object used to identify the database table and for casting
     * @param <S> Related Element Class
     * @param <C> Collection Class
     * @return A collection of all elements matching the query in the table as objects of the given class, with all related elements
     * @see SQLiteTable#getWhere(SQLiteDatabase, DatabaseColumn, DatabaseColumn.CompareOperation, String, Class)
     */
    public <S extends StoreAble, C extends StoreAbleCollection<S>> Collection<C> getWhere(DatabaseColumn column, DatabaseColumn.CompareOperation comparison, String value, Class<C> collectionClass, Class<S> relatedClass) {
        Collection<C> allCollections = getWhere(column, comparison, value, collectionClass);
        for(C element : allCollections) {
            fillCollection(element, relatedClass);
        }
        return allCollections;
    }

    // endregion

    // region Setter / Saver
    /**
     * Stores a StoreAble object within the database. If it has an ID, the existing data set will be updated,
     * otherwise a new set will be added and the new ID will be set in the StoreAble
     * @param element  Element to be saved into the database
     * @param storageClass Class object used to identify the database table and for casting
     * @param <S> Class of the element
     * @return True: The element was saved, False: The element could not be saved
     * @see SQLiteTable#save(StoreAble, SQLiteDatabase)
     */
    public <S extends StoreAble> boolean save(StoreAble element, Class<S> storageClass) {
        final SQLiteTable table = tableMap.get(storageClass);
        if(table != null) {
            return table.save(element, getWritableDatabase());
        }
        return false;
    }

    /**
     * Stores all given StoreAbles within the database. Will also add IDs if necessary.
     * @param elements List of StoreAbles that needs to be stored
     * @param storageClass Class object used to identify the database table and for casting
     * @param <S> Class of the element
     * @return True: all elements were saved, False: Not all elements could be saved
     * @see SQLiteTable#saveAll(Collection, SQLiteDatabase)
     */
    public <S extends StoreAble> boolean saveAll(Collection<S> elements, Class<S> storageClass) {
        final SQLiteTable table = tableMap.get(storageClass);
        if(table != null) {
            return table.saveAll(elements, getWritableDatabase());
        }
        return false;
    }

    // endregion

    // region Collection Setter / Saver

    /**
     * Stores a Collection object within the database. If it has an ID, the existing data set will be updated,
     * otherwise a new set will be added and the new ID will be set in the Collection. This process will also be repeated for all included related elements.
     * @param collection Collection to be saved into the database
     * @param collectionClass Collection Class object used to identify the database table and for casting
     * @param relatedClass Related Class object used to identify the database table and for casting
     * @param <S> Related Element Class
     * @param <C> Collection Class
     * @return True: The element was saved, False: The element could not be saved
     */
    public <S extends StoreAble, C extends StoreAbleCollection<S>> boolean save(C collection, Class<C> collectionClass, Class<S> relatedClass) {
        boolean success = save(collection, collectionClass);

        if(success) {
            for(S related : collection.getCollection()) {
                success = save(related, relatedClass) && success;
            }
        }

        return success;
    }

    /**
     * Stores all given Collections and their related elements within the database. Will also add IDs if necessary.
     * @param collections All collections to be saved into the database
     * @param collectionClass Collection Class object used to identify the database table and for casting
     * @param relatedClass Related Class object used to identify the database table and for casting
     * @param <S> Related Element Class
     * @param <C> Collection Class
     * @return True: all elements were saved, False: Not all elements could be saved
     */
    public <S extends StoreAble, C extends StoreAbleCollection<S>> boolean saveAll(Collection<C> collections, Class<C> collectionClass, Class<S> relatedClass) {
        boolean success = true;

        for(C collection : collections) {
            success = save(collection, collectionClass, relatedClass) && success;
        }

        return success;
    }

    // endregion

    // region Other Operations

    /**
     * Removes a StoreAble from the database. If the StoreAble doesn't exist, no action will be performed.
     * @param element Element to be deleted from the database
     * @param storageClass Class object used to identify the database table and for casting
     * @param <S> Class of the element
     * @return True: Element was deleted, False: Element did not exist in database (no deletion necessary)
     * @see SQLiteTable#delete(StoreAble, SQLiteDatabase)
     */
    public <S extends StoreAble> boolean delete(StoreAble element, Class<S> storageClass) {
        final SQLiteTable table = tableMap.get(storageClass);
        return table != null
                && storageClass.isInstance(element)
                && table.delete(element, getWritableDatabase());
    }

    /**
     * Removes a Collection and its related StoreAbles from the database.
     * @param collection Collection to be deleted
     * @param collectionClass Collection to be filled with related elements
     * @param relatedClass Related Class object used to identify the database table and for casting
     * @param <S> Related Element Class
     * @param <C> Collection Class
     * @return True: Collection and related elements were deleted, False: Deletion was not successful
     */
    public <S extends StoreAble, C extends StoreAbleCollection<S>> boolean delete(StoreAbleCollection<S> collection, Class<C> collectionClass, Class<S> relatedClass) {
        final SQLiteTable table = tableMap.get(relatedClass);
        if(delete(collection, collectionClass) && table != null) {
            return collection.getCollection().size() ==
                    table.deleteWhere(DatabaseColumn.COLUMN_RELATED_ID, DatabaseColumn.CompareOperation.EQUAL, String.valueOf(collection.getId()), getWritableDatabase());
        }

        return false;
    }


    /**
     * Returns the row count of the given table, aka the count of all StoreAble elements therein.
     * @param storageClass Class object used to identify the database table and for casting
     * @param <S> Class of the element
     * @return Count of all elements in the table.
     * @see SQLiteTable#count(SQLiteDatabase)
     */
    public <S extends StoreAble> int count(Class<S> storageClass) {
        final SQLiteTable table = tableMap.get(storageClass);
        if(table != null) {
            return table.count(getReadableDatabase());
        }
        return -1;
    }

    // endregion
}
