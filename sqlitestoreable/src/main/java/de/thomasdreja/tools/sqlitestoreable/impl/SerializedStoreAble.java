package de.thomasdreja.tools.sqlitestoreable.impl;

import android.content.ContentValues;
import android.database.Cursor;

import java.io.Serializable;

import de.thomasdreja.tools.sqlitestoreable.template.DatabaseColumn;
import de.thomasdreja.tools.sqlitestoreable.template.SQLiteTable;
import de.thomasdreja.tools.sqlitestoreable.template.StoreAble;
import de.thomasdreja.tools.sqlitestoreable.template.TableInformation;

/**
 * This class encapsulates a SerializeAble object and thus allows for non StoreAble objects to be stored within the database.
 * @param <C> Class of the SerializeAble object
 */
public class SerializedStoreAble<C extends Serializable> implements StoreAble {

    /**
     * In this column the binary data of the serialized object will be stored.
     */
    private static final DatabaseColumn CONTENT_COLUMN = DatabaseColumn.newBinaryColumn("Content");

    /**
     * Use this method to create a new TableInformation for SerializedStoreAble in your database.
     * @param contentClass Class of the SerializeAble objects stored in the database
     * @param <C> Class of the SerializeAble object
     * @return A new TableInformation that can return SerializedStoreAble containers with de-serialized objects of the given class.
     * @see SerializedStoreAble#SerializedStoreAble(Cursor, Class)
     * @see SQLiteTable#deserializeFromDatabase(DatabaseColumn, Cursor, Class)
     */
    public static <C extends Serializable> TableInformation createTableInfo(final Class<C> contentClass) {
        return new TableInformation(SerializedStoreAble.class) {
            @Override
            protected StoreAble read(Cursor cursor) {
                return new SerializedStoreAble<>(cursor, contentClass);
            }
        };
    }

    /**
     * Content object encapsulated by this class
     */
    protected C content;

    /**
     * ID of the StoreAble, invalid id if not yet added to database
     * @see StoreAble#INVALID_ID
     */
    protected long id;

    /**
     * ID of a parent StoreAble, invalid id if none exists
     * @see StoreAble#INVALID_ID
     */
    protected long relatedId;


    /**
     * Creates a new SerializedStoreAble container with the given content attached to it
     * @param content Content to be stored into the database
     */
    public SerializedStoreAble(C content) {
        this.content = content;
        this.id = INVALID_ID;
        this.relatedId = INVALID_ID;
    }

    /**
     * Creates a SerializeStoreAble container from the database. Also de-serializes the stored content from the given class.
     * @param cursor Values from the database
     * @param contentClass Class of the encapsulated content
     * @see SQLiteTable#deserializeFromDatabase(DatabaseColumn, Cursor, Class)
     */
    protected SerializedStoreAble(Cursor cursor, Class<C> contentClass) {
        this(SQLiteTable.deserializeFromDatabase(CONTENT_COLUMN, cursor, contentClass));
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @Override
    public long getRelatedId() {
        return relatedId;
    }

    @Override
    public void setRelatedId(long id) {
        relatedId = id;
    }

    /**
     * Returns the currently encapsulated content
     * @return Encapsulated content, or null if none present
     */
    public C getContent() {
        return content;
    }

    /**
     * Updates the currently encapsulated content
     * @param content New content or null if content should be removed
     */
    public void setContent(C content) {
        this.content = content;
    }

    @Override
    public void exportToDatabase(ContentValues databaseValues) {
        SQLiteTable.serializeToDatabase(CONTENT_COLUMN, content, databaseValues);
    }
}
