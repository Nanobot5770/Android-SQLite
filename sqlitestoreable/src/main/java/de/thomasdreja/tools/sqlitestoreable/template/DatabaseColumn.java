package de.thomasdreja.tools.sqlitestoreable.template;

import android.database.Cursor;

/**
 * This class represents a field within a SQLite table. Each field has a name as identifier and a fixed field type.
 * The static construction method ensures that only valid types are available
 */
public class DatabaseColumn {

    /**
     * This enum provides comparison operations for the SQL WHERE clause to compare a database field with a given value
     */
    public enum CompareOperation {
        /**
         * Field = Value
         */
        EQUAL("%s =?"),

        /**
         * Field != Value
         */
        NOT_EQUAL("%s !=?"),

        /**
         * Field LIKE Value
         */
        LIKE("%s LIKE?"),

        /**
         * Field NOT LIKE Value
         */
        NOT_LIKE("%s NOT LIKE?"),

        /**
         * Field < Value
         */
        LESS("%s <?"),

        /**
         * Field > Value
         */
        GREATER("%s >?"),

        /**
         * Field <= Value
         */
        LESS_EQUAL("%s <=?"),

        /**
         * Field >= Value
         */
        GREATER_EQUAL("%s >=?"),

        /**
         * Field IN Value(s)
         */
        IN("%s IN?"),

        /**
         * Field NOT IN Value(s)
         */
        NOT_IN("%s NOT IN?");

        final String query;

        CompareOperation(String query) {
            this.query = query;
        }
    }

    /**
     * Name identifier of the database field.
     * Must be unique per table!
     */
    public final String name;

    /**
     * Type of the database field. Can only be one of a fixed set of SQLite types!
     */
    public final String columnType;

    /**
     * Creates a new DatabaseColumn with the given name and type parameters.
     * Note: This constructor is private and should only be accessed with a fixed type in mind!
     * @param name Name identifier of the database field
     * @param type Type of the database field (must be a valid SQLite type)
     */
    private DatabaseColumn(String name, String type) {
        this.name = name;
        this.columnType = type;
    }

    public String where(CompareOperation operation) {
        return String.format(operation.query, name);
    }

    public int getColumnIndex(Cursor cursor) {
        return cursor.getColumnIndex(name);
    }

    /**
     * Name of the ID database field. Each StoreAble SQLiteTable will have this field automatically!
     * @see SQLiteTable
     * @see StoreAble
     */
    private static final String ID = "ID";

    private static final String RELATED_ID = "ParentID";

    /**
     * Constant DatabaseColumn for the ID in the table. Each StoreAble SQLiteTable will have this field automatically!
     * This field will also serve as primary key for the table.
     * @see SQLiteTable
     * @see StoreAble
     */
    static final DatabaseColumn COLUMN_ID = new DatabaseColumn(ID, "INTEGER PRIMARY KEY");

    static final DatabaseColumn COLUMN_RELATED_ID = DatabaseColumn.newIntColumn(RELATED_ID);

    /**
     * Creates a new field that will contain strings
     * @param name Name of the field
     * @return A new field with the given name and the string type
     */
    public static DatabaseColumn newStringColumn(String name) {
        return new DatabaseColumn(name, "TEXT");
    }

    /**
     * Creates a new field that will contain integers or longs
     * @param name Name of the field
     * @return A new field with the given name and integer/long type
     */
    public static DatabaseColumn newIntColumn(String name) {
            return new DatabaseColumn(name, "INTEGER");
    }

    /**
     * Creates a new field that will contain floats or doubles
     * @param name Name of the field
     * @return A new field with the given name and float or double type
     */
    public static DatabaseColumn newFloatColumn(String name) {
        return new DatabaseColumn(name, "REAL");
    }

    /**
     * Creates a new field that will contain binary data, useful for object serialisation.
     * @param name Name of the field
     * @return A new field with the given name and binary data type
     * @see java.io.Serializable
     */
    public static DatabaseColumn newBinaryColumn(String name) {
        return new DatabaseColumn(name, "BLOB");
    }
}
