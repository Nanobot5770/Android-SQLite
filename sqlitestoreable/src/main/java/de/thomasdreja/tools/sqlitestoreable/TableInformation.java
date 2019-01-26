package de.thomasdreja.tools.sqlitestoreable;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * This class is used to provide the type specific information to the SQLiteTable.
 * It names the table, provides a list of fields and it handles the creation of new StoreAble objects.
 */
public abstract class TableInformation {

    /**
     * DatabaseFields within the table
     */
    final DatabaseField[] dbFields;

    /**
     * Name of the table, always set to the class name of the stored objects
     */
    final String name;

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
        dbFields = addFirstField(DatabaseField.FIELD_ID, databaseFields);
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

    protected static DatabaseField[] addFirstField(DatabaseField first, DatabaseField... otherFields) {
        if(otherFields == null) {
            otherFields = new DatabaseField[0];
        }

        DatabaseField[] newFields = new DatabaseField[otherFields.length+1];
        newFields[0] = DatabaseField.FIELD_ID;
        System.arraycopy(otherFields, 0, newFields, 1, otherFields.length);
        return newFields;
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
        public static final String ID = "ID";

        public static final String PARENT_ID = "ParentID";

        /**
         * Constant DatabaseField for the ID in the table. Each StoreAble SQLiteTable will have this field automatically!
         * This field will also serve as primary key for the table.
         * @see SQLiteTable
         * @see StoreAble
         */
        protected static final DatabaseField FIELD_ID = new DatabaseField("INTEGER PRIMARY KEY", ID);

        public static final DatabaseField FIELD_PARENT_ID = DatabaseField.newIntField(PARENT_ID);

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
}
