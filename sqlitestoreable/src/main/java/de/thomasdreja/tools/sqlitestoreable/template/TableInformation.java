package de.thomasdreja.tools.sqlitestoreable.template;

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
    final DatabaseColumn[] dbColumns;

    /**
     * Name of the table, always set to the class name of the stored objects
     */
    public final String name;

    /**
     * Class Object of the objects to be stored in the table
     */
    final Class<? extends StoreAble> storageClass;

    /**
     * Creates a new TableInformation object for the given StoreAble class with the given database fields
     * This in turn is used to create a matching table in the database
     * @param storageClass Class of the Object to be stored into the table
     * @param databaseColumns Fields to store the information of the object (if no fields are given, the database can only store the ID)
     * @see DatabaseColumn
     * @see SQLiteTable#SQLiteTable(TableInformation)
     */
    public TableInformation(Class<? extends StoreAble> storageClass, DatabaseColumn... databaseColumns) {
        this.storageClass = storageClass;
        this.name = storageClass.getSimpleName();
        dbColumns = addColumns(databaseColumns, DatabaseColumn.COLUMN_ID, DatabaseColumn.COLUMN_RELATED_ID);
    }

    /**
     * Returns a new object based on the values from the database, stored within the given cursor.
     * The cursor will already point to the correct element, only the current row needs to be read.
     * @param cursor Cursor that contains values for 1 new element from the database
     * @param storageClass Requested Class container for the object to be created. Use for safety checks!
     * @param <S> Class of the object
     * @return A object of class S that contains the values from the cursor
     * @see SQLiteTable#get(Class, long, SQLiteDatabase)
     * @see SQLiteTable#getAll(Class, SQLiteDatabase)
     * @see SQLiteTable#getWhere(SQLiteDatabase, DatabaseColumn, DatabaseColumn.Operator, String, Class)
     * @see SQLiteTable#getNewElement(Cursor, Class)
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
     * @see SQLiteTable#get(Class, long, SQLiteDatabase)
     * @see SQLiteTable#getAll(Class, SQLiteDatabase)
     * @see SQLiteTable#getWhere(SQLiteDatabase, DatabaseColumn, DatabaseColumn.Operator, String, Class)
     * @see SQLiteTable#getNewElement(Cursor, Class)
     */
    protected abstract StoreAble read(Cursor cursor);

    /**
     * Adds the latter columns in front of the columnList
     * @param existingColumns Already existing columns
     * @param furtherColumns Columns to be added in front
     * @return An array with both column lists merged
     */
    private static DatabaseColumn[] addColumns(DatabaseColumn[] existingColumns, DatabaseColumn... furtherColumns) {
        if(existingColumns == null) {
            existingColumns = new DatabaseColumn[0];
        }

        if(furtherColumns == null) {
            furtherColumns = new DatabaseColumn[0];
        }

        DatabaseColumn[] newFields = new DatabaseColumn[existingColumns.length + furtherColumns.length];
        System.arraycopy(furtherColumns, 0, newFields, 0, furtherColumns.length);
        System.arraycopy(existingColumns, 0, newFields, furtherColumns.length, existingColumns.length);
        return newFields;
    }

}
