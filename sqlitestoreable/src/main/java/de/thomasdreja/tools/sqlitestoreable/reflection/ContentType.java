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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * This enum provides an enumeration for all possible types of content that can be stored into and restore from the database.
 * The types are based on the parameters provided by ContentValues and Cursor
 * @see ContentValues
 * @see Cursor
 */
public enum ContentType {
    /**
     * String, stored as Text
     * @see SQLiteType#TEXT
     * @see String
     */
    STRING(SQLiteType.TEXT, String.class),

    /**
     * Float, stored as Real
     * @see SQLiteType#REAL
     * @see Float
     */
    FLOAT(SQLiteType.REAL, Float.class, Float.TYPE),

    /**
     * Double, stored as Real     *
     * @see SQLiteType#REAL
     * @see Double
     */
    DOUBLE(SQLiteType.REAL, Double.class, Double.TYPE),

    /**
     * Boolean, stored as Integer
     * @see SQLiteType#INTEGER
     * @see Boolean
     */
    BOOLEAN(SQLiteType.INTEGER, Boolean.class, Boolean.TYPE),

    /**
     * Byte, stored as Integer
     * @see SQLiteType#INTEGER
     * @see Byte
     */
    BYTE(SQLiteType.INTEGER, Byte.class, Byte.TYPE),

    /**
     * Short, stored as Integer
     * @see SQLiteType#INTEGER
     * @see Short
     */
    SHORT(SQLiteType.INTEGER, Short.class, Short.TYPE),

    /**
     * Int, stored as Integer
     * @see SQLiteType#INTEGER
     * @see Integer
     */
    INT(SQLiteType.INTEGER, Integer.class, Integer.TYPE),

    /**
     * Long, stored as Integer
     * @see SQLiteType#INTEGER
     * @see Long
     */
    LONG(SQLiteType.INTEGER, Long.class, Long.TYPE),

    /**
     * Byte Array, stored as Blob
     * @see SQLiteType#BLOB
     * @see Byte
     */
    BYTE_ARRAY(SQLiteType.BLOB, byte[].class),

    /**
     * Binary, stored as Blob
     * @see SQLiteType#BLOB
     * @see Serializable
     */
    BINARY(SQLiteType.BLOB),

    /**
     * Unsupported, will not be stored!
     */
    UNSUPPORTED(SQLiteType.INTEGER);

    /**
     * Type the SQL Table will use to store the value
     */
    public final SQLiteType sqlType;

    /**
     * Java classes this content type supports. Empty if all classes are supported
     */
    private final Class<?>[] classes;

    /**
     * Creates a new ContentType that maps values of the given classes onto the given type of SQLite column
     * @param type Type of the SqLite column
     * @param classes Classes that will be mapped by this type
     */
    ContentType(SQLiteType type, Class<?>... classes) {
        this.sqlType = type;
        this.classes = classes;
    }

    /**
     * This enum represents the basic types of columns that SqLite can support.
     * ContentTypes map onto this base types for storage
     */
    public enum SQLiteType {
        /**
         * Stores byte arrays
         */
        BLOB,

        /**
         * Integer stores natural numbers (even long)
         */
        INTEGER,

        /**
         * Real stores all real numbers (double and float)
         */
        REAL,

        /**
         * Stores text strings
         */
        TEXT
    }

    /**
     * For a given class this will return the ContentType that can handle this class
     * @param type Class that needs to be stored in a column in the database
     * @return ContentType for this class, ContentType.BINARY if the class needs to be serialized or UNSUPPORTED if the class cannot be handled
     */
    public static ContentType getTypeOf(Class<?> type) {
        for(ContentType contentType : ContentType.values()) {
            for(Class<?> mClass : contentType.classes) {
                if(mClass == type) {
                    return contentType;
                }
            }
        }
        if(Serializable.class.isAssignableFrom(type)) {
            return BINARY;
        }
        return ContentType.UNSUPPORTED;
    }

    /**
     * Stores the given value into the column into the ContentValues container
     * @param columnName Name of the column (aka key)
     * @param object Value of the field
     * @param values Value Container
     * @see ContentValues
     * @see ContentType#store(ContentType, String, Object, ContentValues)
     */
    public void store(String columnName, Object object, ContentValues values) {
        store(this, columnName, object, values);
    }

    /**
     * Extracts the value for the column from the cursor
     * @param columnName Name of the Column
     * @param cursor Database Query with the values
     * @return Object or null if no value could be restored
     * @see Cursor
     * @see ContentType#restore(ContentType, String, Cursor)
     */
    public Object restore(String columnName, Cursor cursor) {
        return restore(this, columnName, cursor);
    }

    /**
     * Returns whether the given object is an instance of this content type or not
     * @param object Object to be compared
     * @return True: Is instance of the type, False: Is instance of a different type
     * @see ContentType#isInstanceOf(ContentType, Object)
     */
    @SuppressWarnings("unused")
    public boolean isInstanceOf(Object object) {
        return isInstanceOf(this, object);
    }

    /**
     * Stores the given value into the column into the ContentValues container
     * @param type Type of the value to be stored
     * @param columnName Name of the column (aka key)
     * @param object Value of the field
     * @param values Value Container
     */
    private static void store(ContentType type, String columnName, Object object, ContentValues values) {
        if(isInstanceOf(type, object)) {
            switch (type) {
                case BINARY:
                    values.put(columnName, serialize(object));
                    return;
                case BOOLEAN:
                    values.put(columnName, (Boolean) object);
                    return;
                case BYTE:
                    values.put(columnName, (Byte) object);
                    return;
                case SHORT:
                    values.put(columnName, (Short) object);
                    return;
                case INT:
                    values.put(columnName, (Integer) object);
                    return;
                case LONG:
                    values.put(columnName, (Long) object);
                    return;
                case FLOAT:
                    values.put(columnName, (Float) object);
                    return;
                case DOUBLE:
                    values.put(columnName, (Double) object);
                    return;
                case STRING:
                    values.put(columnName, (String) object);
                    return;
                case BYTE_ARRAY:
                    values.put(columnName, (byte[]) object);
            }
        }
    }

    /**
     * Extracts the value for the column from the cursor
     * @param type Type of the value to be extracted
     * @param columnName Name of the Column
     * @param cursor Database Query with the values
     * @return Object or null if no value could be restored
     * @see Cursor
     */
    private static Object restore(ContentType type, String columnName, Cursor cursor) {
        final int index = cursor.getColumnIndex(columnName);
        if(index >= 0) {
            switch (type) {
                case BINARY:
                    return deserialize(cursor.getBlob(index));
                case BOOLEAN:
                    return cursor.getInt(index) > 0;
                case BYTE:
                    return (byte) cursor.getInt(index);
                case SHORT:
                    return cursor.getShort(index);
                case INT:
                    return cursor.getInt(index);
                case LONG:
                    return cursor.getLong(index);
                case FLOAT:
                    return cursor.getFloat(index);
                case DOUBLE:
                    return cursor.getDouble(index);
                case STRING:
                    return cursor.getString(index);
                case BYTE_ARRAY:
                    return cursor.getBlob(index);
            }
        // Safety check: Don't return null for numbers and booleans
        } else {
            switch (type) {
                case BOOLEAN:
                    return false;
                case BYTE:
                    return (byte) 0;
                case SHORT:
                    return (short) 0;
                case INT:
                    return 0;
                case LONG:
                    return 0L;
                case FLOAT:
                    return 0F;
                case DOUBLE:
                    return 0D;
            }
        }
        return null;
    }

    /**
     * Returns whether the given object is an instance of this content type or not
     * @param type Type to be compared
     * @param object Object to be compared
     * @return True: Is Instance of given type, False: Is instance of a different type
     */
    private static boolean isInstanceOf(ContentType type, Object object) {
        for(Class<?> mClass : type.classes) {
            if(mClass.isInstance(object)) {
                return true;
            }
        }
        return object instanceof Serializable && type == BINARY;
    }

    /**
     * Serializes the given object into a byte array
     * @param object Object to be serialized
     * @return Byte Array with the serialized contents
     */
    private static byte[] serialize(Object object) {
        if(object instanceof Serializable) {
            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
            try {
                ObjectOutputStream objectStream = new ObjectOutputStream(byteOutput);
                objectStream.writeObject(object);
                byte[] result = byteOutput.toByteArray();
                byteOutput.close();
                return result;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Restores an previously serialized object from a given byte array
     * @param bytes Byte Array with the serialized contents
     * @return Restored Object
     */
    private static Object deserialize(byte[] bytes) {
        if(bytes != null && bytes.length > 0) {
            try {
                ByteArrayInputStream byteInput = new ByteArrayInputStream(bytes);
                ObjectInputStream objectStream = new ObjectInputStream(byteInput);
                Object object = objectStream.readObject();
                objectStream.close();
                return object;
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}
