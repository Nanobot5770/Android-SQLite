package de.thomasdreja.tools.sqlitestoreable.reflection;

import android.content.ContentValues;
import android.database.Cursor;

import java.lang.reflect.Field;

import de.thomasdreja.tools.sqlitestoreable.template.StoreAble;

public class FieldColumn implements TableInformation.DbColumn {

    private final Field field;
    private final String fieldName;
    private final ContentType contentType;

    FieldColumn(Field field) throws DatabaseSchemeException {
        final StoreAbleField annotation = field.getAnnotation(StoreAbleField.class);
        if(annotation != null) {
            this.field = field;
            this.field.setAccessible(true);
            this.contentType = ContentType.getTypeOf(field.getType());

            if(annotation.fieldName().isEmpty()) {
                fieldName = TableInformation.cleanName(field.getName());
            } else {
                fieldName = TableInformation.cleanName(annotation.fieldName());
            }
        } else {
            throw new DatabaseSchemeException();
        }
    }

    @Override
    public String getName() {
        return fieldName;
    }

    @Override
    public ContentType getType() {
        return contentType;
    }

    @Override
    public void restoreValue(StoreAble element, Cursor cursor) {
        if(field.getDeclaringClass().isInstance(element)) {
            try {
                field.set(element, contentType.restore(fieldName, cursor));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public <S extends StoreAble> void storeValue(S element, ContentValues values) {
        if(field.getDeclaringClass().isInstance(element)) {
            try {
                contentType.store(fieldName, field.get(element), values);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String toString() {
        return "FieldColumn{" +
                "field=" + field.getName() +
                ", fieldName='" + fieldName + '\'' +
                ", contentType=" + contentType +
                '}';
    }
}
