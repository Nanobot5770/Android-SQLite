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

package de.thomasdreja.tools.sqlitestoreable.reflection;

import android.content.ContentValues;
import android.database.Cursor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import de.thomasdreja.tools.sqlitestoreable.template.StoreAble;

/**
 * This wrapper class extracts class information from another class implementing StoreAble
 * and provides them to TableWrappers so that objects of the given class can be stored into and restored from the database.
 * @see de.thomasdreja.tools.sqlitestoreable.template.TableWrapper
 * @see StoreAble
 */
public class TableInformation {

    /**
     * This interface provides the template for a column in the database.
     * Each column is defined by name and type, and can store and restore objects
     */
    public interface DbColumn {

        /**
         * Returns the name of the column
         * @return Name of the column where values will be stored
         */
        String getName();

        /**
         * Type of the column.
         * @return Type of values that will be stored within this column
         */
        ContentType getType();

        /**
         * Tries to restore the value of this column from the cursor to the given element
         * @param element Element that needs to be restored
         * @param cursor Database query containing values for this column
         * @see Cursor
         * @see TableInformation#restore(Cursor)
         */
        void restoreValue(StoreAble element, Cursor cursor);

        /**
         * Tries to store the value of this column from the given element into the ContentValues container
         * @param element Element that needs to be stored
         * @param values Container of key value pairs
         * @param <S> Class of the element
         * @see ContentValues
         * @see TableInformation#store(StoreAble, String...)
         */
        <S extends StoreAble> void storeValue(S element, ContentValues values);
    }

    /**
     * Class of the objects that will be stored within the database
     */
    private final Class<?> storageClass;

    /**
     * The default (empty) constructor used to create new instances of the class
     */
    private final Constructor<?> constructor;

    /**
     * All columns that store values in the database for the given class
     */
    private final DbColumn[] columns;

    /**
     * Creates a new TableInformation containing all fields for the given class.
     * If the class does not implement StoreAble, the result will not be valid.
     * @param storageClass Class to be stored in the database, must implement StoreAble
     * @see StoreAble
     * @see TableInformation#isValid()
     */
    @SuppressWarnings("WeakerAccess")
    public TableInformation(Class<?> storageClass) {
        this.storageClass = storageClass;

        if(StoreAble.class.isAssignableFrom(storageClass)) {
            columns = createColumns(storageClass);
            constructor = getConstructor(storageClass);
        } else {
            columns = new DbColumn[0];
            constructor = null;
        }
    }

    /**
     * Tries to store the values contained in the given object into a ContentValues container.
     * All fields named in the removeColumns parameter will be ignored for storage.
     * @param element Element containing the values to be saved
     * @param removeColumns Columns/Fields that should be ignored
     * @param <S> Class of the Element
     * @return A ContentValues container, filled with key value pairs if successful, empty if not
     * @see ContentValues
     * @see DbColumn#storeValue(StoreAble, ContentValues)
     */
    public <S extends StoreAble> ContentValues store(S element, String... removeColumns) {
        ContentValues values = new ContentValues();
        if(storageClass.isInstance(element)) {
            for(DbColumn column : columns) {
                for(String ignore : removeColumns) {
                    if(!column.getName().equals(ignore)) {
                        column.storeValue(element, values);
                    }
                }
            }
        }
        return values;
    }

    /**
     * Tries to restore/recreate a new object from the values stored within the given cursor.
     * @param cursor Database query that contains the new values of the object
     * @return A new instance of an StoreAble or null if the object could not be restored
     * @see TableInformation#getStorageClass()
     * @see DbColumn#restoreValue(StoreAble, Cursor)
     */
    public StoreAble restore(Cursor cursor) {
        if(constructor != null) {
            try {
                Object object = constructor.newInstance();
                if(storageClass.isInstance(object)) {
                    final StoreAble element = (StoreAble) object;
                    for(DbColumn column : columns) {
                        column.restoreValue(element, cursor);
                    }
                    return element;
                }
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Returns the name of the database table objects will be stored in.
     * This name is the name of the class stored within.
     * @return Name of the database table
     * @see Class#getSimpleName()
     */
    public String getName() {
        return storageClass.getSimpleName();
    }

    /**
     * Returns whether the given information object can be used to store objects or not.
     * Invalid information objects may not be able to fully restore objects and should be discarded!
     * @return True: Can store and restore objects, False: Cannot store and restore objects
     */
    public boolean isValid() {
        return StoreAble.class.isAssignableFrom(storageClass) && constructor != null;
    }

    /**
     * Returns the storageClass that this wrapper will store into and restore from the database.
     * @return Class of objects that this wrapper handles
     * @see StoreAble
     * @see de.thomasdreja.tools.sqlitestoreable.template.TableWrapper
     */
    public Class<?> getStorageClass() {
        return storageClass;
    }

    /**
     * Returns all columns needed to store an object of the storageClass
     * @return An array of Columns
     * @see TableInformation#getStorageClass()
     */
    public DbColumn[] getColumns() {
        return columns;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public String toString() {
        return "TableInformation{" +
                "storageClass=" + storageClass.getName() +
                ", constructor=" + constructor.getName() +
                ", columns=" + Arrays.toString(columns) +
                '}';
    }

    /**
     * Tries to find an constructor with an empty parameter list (aka default constructor) for creating new objects.
     * @param storageClass Class to be searched in/to be constructed later on
     * @return Constructor with no parameters, or null if none was found
     */
    private static Constructor<?> getConstructor(Class<?> storageClass) {
        for(Constructor<?> constructor : storageClass.getDeclaredConstructors()) {
            if(constructor.getParameterTypes().length == 0) {
                return constructor;
            }
        }
        return null;
    }

    /**
     * Extracts all annotated fields and methods and wraps them into DbColumns.
     * @param storageClass Class which fields should be extracted
     * @return An array of DbColumns based upon the given class
     * @see TableInformation#createColumns(ArrayList, Class)
     * @see FieldColumn
     * @see MethodColumn
     */
    private static DbColumn[] createColumns(Class<?> storageClass) {
        ArrayList<DbColumn> columns = new ArrayList<>();
        createColumns(columns, storageClass);
        return columns.toArray(new DbColumn[0]);
    }

    /**
     * Iterates through a given class and extracts all annotated fields and methods for database storage.
     * Note: Will also recursively iterate through superclasses and interfaces.
     * @param columns Temporary list storage for all columns found in the class
     * @param storageClass Class to be searched through.
     * @see FieldColumn
     * @see MethodColumn
     */
    private static void createColumns(ArrayList<DbColumn> columns, Class<?> storageClass) {
        for(Field field : storageClass.getDeclaredFields()) {
            if(field.getAnnotation(StoreAbleField.class) != null) {
                try {
                    columns.add(new FieldColumn(field));
                } catch (DatabaseSchemeException e) {
                    e.printStackTrace();
                }
            }
        }
        HashMap<String,Method> methodHashMap = new HashMap<>();
        StoreAbleField annotation;
        Method other;
        for(Method method : storageClass.getMethods()) {
            annotation = method.getAnnotation(StoreAbleField.class);
            if(annotation != null && !annotation.fieldName().isEmpty()) {
                if((other = methodHashMap.get(annotation.fieldName())) != null) {
                    try {
                        columns.add(new MethodColumn(method, other));
                    } catch (DatabaseSchemeException e) {
                        e.printStackTrace();
                    }
                } else {
                    methodHashMap.put(annotation.fieldName(), method);
                }
            }
        }

        if(storageClass.getSuperclass() != null) {
            createColumns(columns, storageClass.getSuperclass());
        }

        for(Class<?> mInterface : storageClass.getInterfaces()) {
            createColumns(columns, mInterface);
        }
    }

    /**
     * Wraps the given classes into TableInformation objects and returns them as an array.
     * Note: Classes that do not implement StoreAble will be ignored!
     * @param classes Classes to be stored in a database/wrapper into TableInformation objects
     * @return An array of TableInformation objects
     * @see TableInformation#TableInformation(Class)
     * @see StoreAble
     */
    public static TableInformation[] createInformation(Class<?>[] classes) {
        ArrayList<TableInformation> infoList = new ArrayList<>();
        for(Class<?> mClass : classes) {
            if(StoreAble.class.isAssignableFrom(mClass)) {
                infoList.add(new TableInformation(mClass));
            }
        }
        return infoList.toArray(new TableInformation[0]);
    }

    /**
     * Ensures that all field names only have save characters within them
     * @param name Field name to be cleaned
     * @return Cleaned field name (All characters except letters and numbers removed)
     */
    public static String cleanName(String name) {
        return name.replaceAll("[^a-zA-Z]", "");
    }
}
