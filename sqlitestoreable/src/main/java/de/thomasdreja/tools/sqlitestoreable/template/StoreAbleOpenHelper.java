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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import de.thomasdreja.tools.sqlitestoreable.reflection.TableInformation;

/**
 * This class extends the Android SQLiteOpenHelper to allow for direct integration with the StoreAble system.
 * It provides all tools to create, read and write a database with one or more SQLiteTables
 * @see TableWrapper
 * @see StoreAble
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class StoreAbleOpenHelper extends SQLiteOpenHelper {

    //region Variables and Constructors

    /**
     * Command used to delete the given table on version upgrade
     */
    protected static final String DROP_TABLE_IF = "DROP TABLE IF EXISTS %s";

    /**
     * Hashmap containing all tables of the database, indexed by table name
     */
    protected final HashMap<Class<?>, TableWrapper> tableMap;

    /**
     * Application context, set via constructor
     */
    protected final Context context;

    /**
     * Creates a new database based on the application context, name and version.
     * Also fills in all SQLiteTables based upon the given helpers.
     * @param context Application context for the database
     * @param name Name of the database
     * @param version Version of the database - Increasing it will drop all existing elements!
     * @param tables Table information for creating all tables in the database
     * @see TableInformation
     * @see TableWrapper
     */
    public StoreAbleOpenHelper(Context context, String name, int version, TableInformation... tables) {
        super(context, name, null, version);
        this.context = context;
        tableMap = new HashMap<>();

        for (TableInformation helper : tables) {
            if(helper.isValid()) {
                tableMap.put(helper.getStorageClass(), new TableWrapper(helper));
            }
        }
    }

    /**
     * Creates a new database based on the application context, name and version.
     * Also creates the necessary helpers for the database based upon the given classes
     * @param context Application context for the database
     * @param name Name of the database
     * @param version ersion of the database - Increasing it will drop all existing elements!
     * @param classes List of classes to be stored within the database. All classes must implement StoreAble
     * @see StoreAble
     * @see TableInformation#createColumns(Class)
     */
    public StoreAbleOpenHelper(Context context, String name, int version, Class<?>... classes) {
        this(context, name, version, TableInformation.createInformation(classes));
    }


    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        for(TableWrapper table : tableMap.values()) {
            sqLiteDatabase.execSQL(table.createTable());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        for(TableWrapper table : tableMap.values()) {
            sqLiteDatabase.execSQL(String.format(DROP_TABLE_IF, table.getName()));
            sqLiteDatabase.execSQL(table.createTable());
        }
    }

    /**
     * Returns the names of all tables in the database
     * @return Set with all Class objects used to identify the database tables
     */
    public Set<Class<?>> getAllTableClasses() {
        return tableMap.keySet();
    }

    /**
     * Returns whether the database has the given table or not
     * @param storageClass Class object used to identify the database table
     * @return True: Table exists in database, False: Table not found
     */
    public boolean hasTable(Class<?> storageClass) {
        return tableMap.containsKey(storageClass);
    }

    /**
     * Returns the TableWrapper that is used for the given class
     * @param storageClass Class object used to identify the database table
     * @return TableWrapper (Wrapper) that stores the elements of this class or null if the class is not within the database
     */
    public TableWrapper getTableFor(Class<? extends StoreAble> storageClass) {
        return tableMap.get(storageClass);
    }

    /**
     * Returns all columns of the table used to store objects of the given class
     * @param storageClass Class object used to identify the database table
     * @return An array of all columns used for storage. Array is empty if the class is not supported.
     */
    public TableInformation.DbColumn[] getColumnsFor(Class<? extends StoreAble> storageClass) {
        final TableWrapper table = tableMap.get(storageClass);
        if(table != null) {
            return table.getColumns();
        }
        return new TableInformation.DbColumn[0];
    }

    // endregion

    // region Getter
    /**
     * Reads a single element from the table for the given class with the matching id
     * @param <S> Class of the element
     * @param storageClass Class object used to identify the database table and for casting
     * @param id ID of the element
     * @return Element as object of given class with the matching id
     * @see TableWrapper#get(Class, long, SQLiteDatabase)
     */
    @SuppressWarnings("unchecked")
    public <S extends StoreAble> S get(Class<S> storageClass, long id) {
        final TableWrapper table = tableMap.get(storageClass);
        if(table != null) {
            S element = table.get(storageClass, id, getReadableDatabase());
            if(element instanceof StoreAbleCollection) {
                final StoreAbleCollection collection = (StoreAbleCollection) element;
                fillCollection(collection.getChildClass(), collection);
            }
            return element;
        }
        return null;
    }

    /**
     * Reads all elements contained in the table for the given class
     * @param storageClass Class object used to identify the database table and for casting
     * @param <S> Class of the element
     * @return A collection of all elements currently in the table as objects of class S
     * @see TableWrapper#getAll(Class, SQLiteDatabase)
     */
    public <S extends StoreAble> Collection<S> getAll(Class<S> storageClass) {
        final TableWrapper table = tableMap.get(storageClass);
        if(table != null) {
            final Collection<S> elements = table.getAll(storageClass, getReadableDatabase());
            checkChildren(storageClass, elements);
            return elements;
        }
        return new ArrayList<>();
    }

    /**
     * Returns all elements that match the given criteria
     * @param storageClass Class object used to identify the database table and for casting
     * @param and True: All comparisons have to be fulfilled, False: Only one has to be fulfilled
     * @param comparisons Comparisons to filter the elements
     * @param <S> Class of the element
     * @return A collection of all elements matching the query in the table as objects of the given class
     * @see TableWrapper#getWhere(Class, SQLiteDatabase, boolean, DbComparison...)
     */
    public <S extends StoreAble> Collection<S> getWhere(Class<S> storageClass, boolean and, DbComparison... comparisons) {
        final TableWrapper table = tableMap.get(storageClass);
        if(table != null) {
            final Collection<S> elements = table.getWhere(storageClass, getReadableDatabase(), and, comparisons);
            checkChildren(storageClass, elements);
            return elements;
        }
        return new ArrayList<>();
    }

    /**
     * Checks whether the elements in the given collection could contain child objects and adds those if necessary
     * @param <S> Class of the element
     * @param storageClass Class object used to identify the database table and for casting
     * @param elements A collection of elements that may potentially have children attached to each of them
     */
    @SuppressWarnings("unchecked")
    private <S extends StoreAble> void checkChildren(Class<S> storageClass, Collection<S> elements) {
        if(StoreAbleCollection.class.isAssignableFrom(storageClass)) {
            for(S element : elements) {
                StoreAbleCollection collection = (StoreAbleCollection) element;
                fillCollection(collection.getChildClass(), collection);
            }
        }
    }

    /**
     * Reads all related elements from the database the fit into the given collection.
     * @param <S> Related Element Class
     * @param <C> Collection Class
     * @param relatedClass Related Class object used to identify the database table and for casting
     * @param collection Collection to be filled with related elements
     * @see TableWrapper#getWhere(Class, SQLiteDatabase, boolean, DbComparison[])
     * @see DbComparison#equalsRelatedId(StoreAbleCollection)
     */
    private <S extends StoreAble, C extends StoreAbleCollection<S>> void fillCollection(Class<S> relatedClass, C collection) {
        if(collection != null) {
            collection.setCollection(getWhere(relatedClass, true, DbComparison.equalsRelatedId(collection)));
        }
    }

    // endregion

    // region Setter / Saver
    /**
     * Stores a StoreAble object within the database. If it has an ID, the existing data set will be updated,
     * otherwise a new set will be added and the new ID will be set in the StoreAble
     * @param <S> Class of the element
     * @param storageClass Class object used to identify the database table and for casting
     * @param element  Element to be saved into the database
     * @return True: The element was saved, False: The element could not be saved
     * @see TableWrapper#save(StoreAble, SQLiteDatabase, boolean)
     */
    @SuppressWarnings("unchecked")
    public <S extends StoreAble> boolean save(Class<S> storageClass, StoreAble element) {
        final TableWrapper table = tableMap.get(storageClass);
        if(table != null) {
            boolean success = table.save(element, getWritableDatabase(), true);
            if(success && element instanceof StoreAbleCollection) {
                final StoreAbleCollection collection = (StoreAbleCollection) element;
                success = saveAll(collection.getChildClass(), collection.getCollection(), collection.getId());
            }
            return success;
        }
        return false;
    }

    /**
     * Stores all given StoreAbles within the database. Will also add IDs if necessary.
     * @param <S> Class of the element
     * @param storageClass Class object used to identify the database table and for casting
     * @param elements List of StoreAbles that needs to be stored
     * @return True: all elements were saved, False: Not all elements could be saved
     * @see TableWrapper#saveAll(Collection, SQLiteDatabase)
     */
    @SuppressWarnings("unchecked")
    public <S extends StoreAble> boolean saveAll(Class<S> storageClass, Collection<S> elements) {
        final TableWrapper table = tableMap.get(storageClass);
        if(table != null) {
            boolean success = table.saveAll(elements, getWritableDatabase());
            if(success && StoreAbleCollection.class.isAssignableFrom(storageClass)) {
                for(S element : elements) {
                    StoreAbleCollection collection = (StoreAbleCollection) element;
                    success = saveAll(collection.getChildClass(), collection.getCollection(), collection.getId()) && success;
                }
            }
            return success;
        }
        return false;
    }

    /**
     * Stores all given StoreAbles within the database. Will also add IDs if necessary.
     * All given StoreAbles will have their related ID set to the given ID!
     * @param storageClass Class object used to identify the database table and for casting
     * @param elements List of StoreAbles that needs to be stored
     * @param relatedId ID of the parent object / Related ID
     * @param <S> Class of the element
     * @return True: all elements were saved, False: Not all elements could be saved
     * @see StoreAbleOpenHelper#saveAll(Class, Collection)
     */
    private <S extends StoreAble> boolean saveAll(Class<S> storageClass, Collection<S> elements, long relatedId) {
        for(S element : elements) {
            element.setRelatedId(relatedId);
        }
        return saveAll(storageClass, elements);
    }

    // endregion

    // region Other Operations

    /**
     * Removes a StoreAble from the database. If the StoreAble doesn't exist, no action will be performed.
     * @param <S> Class of the element
     * @param storageClass Class object used to identify the database table and for casting
     * @param element Element to be deleted from the database
     * @return True: Element was deleted, False: Element did not exist in database (no deletion necessary)
     * @see TableWrapper#delete(StoreAble, SQLiteDatabase)
     */
    @SuppressWarnings("unchecked")
    public <S extends StoreAble> boolean delete(Class<S> storageClass, StoreAble element) {
        final TableWrapper table = tableMap.get(storageClass);
        final boolean success = table != null
                && storageClass.isInstance(element)
                && table.delete(element, getWritableDatabase());

        if(success && element instanceof StoreAbleCollection) {
            final StoreAbleCollection collection = (StoreAbleCollection) element;
            return deleteAll(collection.getChildClass(), collection.getCollection());
        }

        return success;
    }

    /**
     * Removes all StoreAbles within the collection from the database
     * @param storageClass Class object used to identify the database table and for casting
     * @param elements Collection of elements to be deleted
     * @param <S> Class of the element
     * @return True: All elements were deleted, False: Not all elements were deleted
     * @see StoreAbleOpenHelper#delete(Class, StoreAble)
     * @see TableWrapper#delete(StoreAble, SQLiteDatabase)
     */
    public <S extends StoreAble> boolean deleteAll(Class<S> storageClass, Collection<S> elements) {
        final TableWrapper table = tableMap.get(storageClass);
        if(table != null) {
            boolean success = true;
            for(S element : elements) {
                success = delete(storageClass, element) && success;
            }
            return success;
        }
        return false;
    }

    /**
     * Returns the row count of the given table, aka the count of all StoreAble elements therein.
     * @param storageClass Class object used to identify the database table and for casting
     * @param <S> Class of the element
     * @return Count of all elements in the table.
     * @see TableWrapper#count(SQLiteDatabase)
     */
    public <S extends StoreAble> int count(Class<S> storageClass) {
        final TableWrapper table = tableMap.get(storageClass);
        if(table != null) {
            return table.count(getReadableDatabase());
        }
        return -1;
    }

    // endregion
}
