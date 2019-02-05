package de.thomasdreja.tools.sqlitestoreable.reflection;


import android.content.ContentValues;
import android.database.Cursor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public enum ContentType {
    STRING(SqliteType.TEXT, String.class),

    FLOAT(SqliteType.REAL, Float.class, Float.TYPE),
    DOUBLE(SqliteType.REAL, Double.class, Double.TYPE),

    BOOLEAN(SqliteType.INTEGER, Boolean.class, Boolean.TYPE),
    BYTE(SqliteType.INTEGER, Byte.class, Byte.TYPE),
    SHORT(SqliteType.INTEGER, Short.class, Short.TYPE),
    INT(SqliteType.INTEGER, Integer.class, Integer.TYPE),
    LONG(SqliteType.INTEGER, Long.class, Long.TYPE),

    BYTE_ARRAY(SqliteType.BLOB, byte[].class),
    BINARY(SqliteType.BLOB);

    public final SqliteType sqlType;
    private final Class<?>[] classes;
    ContentType(SqliteType type, Class<?>... classes) {
        this.sqlType = type;
        this.classes = classes;
    }

    public enum SqliteType {
        BLOB,
        INTEGER,
        REAL,
        TEXT
    }

    public static ContentType getTypeOf(Class<?> type) {
        for(ContentType contentType : ContentType.values()) {
            for(Class<?> mClass : contentType.classes) {
                if(mClass == type) {
                    return contentType;
                }
            }
        }
        return ContentType.BINARY;
    }

    public boolean store(String fieldName, Object object, ContentValues values) {
        return store(this, fieldName, object, values);
    }

    public Object restore(String fieldName, Cursor cursor) {
        return restore(this, fieldName, cursor);
    }

    public boolean isInstanceOf(Object object) {
        return isInstanceOf(this, object);
    }

    public static boolean store(ContentType type, String fieldName, Object object, ContentValues values) {
        if(isInstanceOf(type, object)) {
            switch (type) {
                case BINARY:
                    values.put(fieldName, serialize(object));
                    return true;
                case BOOLEAN:
                    values.put(fieldName, (Boolean) object);
                    return true;
                case BYTE:
                    values.put(fieldName, (Byte) object);
                    return true;
                case SHORT:
                    values.put(fieldName, (Short) object);
                    return true;
                case INT:
                    values.put(fieldName, (Integer) object);
                    return true;
                case LONG:
                    values.put(fieldName, (Long) object);
                    return true;
                case FLOAT:
                    values.put(fieldName, (Float) object);
                    return true;
                case DOUBLE:
                    values.put(fieldName, (Double) object);
                    return true;
                case STRING:
                    values.put(fieldName, (String) object);
                    return true;
                case BYTE_ARRAY:
                    values.put(fieldName, (byte[]) object);
                    return true;
            }
        }
        return false;
    }

    public static Object restore(ContentType type, String fieldName, Cursor cursor) {
        final int index = cursor.getColumnIndex(fieldName);
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
        }
        return null;
    }

    public static boolean isInstanceOf(ContentType type, Object object) {
        for(Class<?> mClass : type.classes) {
            if(mClass.isInstance(object)) {
                return true;
            }
        }
        return type == BINARY;
    }

    public static byte[] serialize(Object object) {
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
        return new byte[0];
    }

    public static Object deserialize(byte[] bytes) {
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
