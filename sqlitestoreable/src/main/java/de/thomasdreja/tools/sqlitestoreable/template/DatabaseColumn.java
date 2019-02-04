package de.thomasdreja.tools.sqlitestoreable.template;

import android.database.Cursor;

/**
 * This class represents a field within a SQLite table. Each field has a name as identifier and a fixed field type.
 * The static construction method ensures that only valid types are available
 */
public class DatabaseColumn {

    public static class Comparison {

        final String where;
        final String value;

        private Comparison(DatabaseColumn column, String operator, String value) {
            this.where = String.format(operator, column.name);
            this.value = value;
        }

        String[] array() {
            return new String[] {value};
        }

        public static Comparison equalsId(StoreAble storeAble) {
            return equalsId(storeAble.getId());
        }

        static Comparison equalsId(long id) {
            return equals(DatabaseColumn.COLUMN_ID, id);
        }

        public static Comparison equalsRelatedId(StoreAbleCollection collection) {
            return equals(DatabaseColumn.COLUMN_RELATED_ID, collection.getId());
        }

        public static Comparison equals(DatabaseColumn column, Object value) {
            return new Comparison(column, "%s =?", String.valueOf(value));
        }

        public static Comparison notEquals(DatabaseColumn column, Object value) {
            return new Comparison(column, "%s !=?", String.valueOf(value));
        }

        public static Comparison less(DatabaseColumn column, Object value) {
            return new Comparison(column, "%s <?", String.valueOf(value));
        }

        public static Comparison lessEqual(DatabaseColumn column, Object value) {
            return new Comparison(column, "%s <=?", String.valueOf(value));
        }

        public static Comparison greater(DatabaseColumn column, Object value) {
            return new Comparison(column, "%s <?", String.valueOf(value));
        }

        public static Comparison greaterEqual(DatabaseColumn column, Object value) {
            return new Comparison(column, "%s <=?", String.valueOf(value));
        }
    }

    static class Query {
        final String where;
        final String[] values;

        Query(boolean and, Comparison... comparisons) {
            StringBuilder builder = new StringBuilder();
            values = new String[comparisons.length];

            for(int i=0; i < comparisons.length; i++) {
                if(i > 0) {
                    if(and) {
                        builder.append(" AND ");
                    } else {
                        builder.append(" OR ");
                    }
                }
                builder.append(comparisons[i].where);
                values[i] = comparisons[i].value;
            }

            where = builder.toString();
        }

        boolean isValid() {
            return values.length > 0;
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
        this.name = name.replaceAll("[^a-zA-Z]", "");
        this.columnType = type;
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
