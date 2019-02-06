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

import java.lang.reflect.Field;

import de.thomasdreja.tools.sqlitestoreable.template.StoreAble;

/**
 * This class allows for the storing and restoring of values from a given field in a class.
 */
public class FieldColumn implements TableInformation.DbColumn {

    /**
     * Field that will be accessed
     */
    private final Field field;

    /**
     * Name of the field in the database (may be set in the annotation)
     */
    private final String fieldName;

    /**
     * Type of the content in the field
     */
    private final ContentType contentType;

    /**
     * Creates a new field wrapper for the given field
     * @param field Field to be wrapped
     * @throws DatabaseSchemeException Thrown if the field lacks an annotation or the type of field is not supported.
     */
    FieldColumn(Field field) throws DatabaseSchemeException {
        final StoreAbleField annotation = field.getAnnotation(StoreAbleField.class);
        if(annotation != null) {
            this.field = field;
            this.field.setAccessible(true);
            this.contentType = ContentType.getTypeOf(field.getType());

            if(contentType == ContentType.UNSUPPORTED) {
                throw new DatabaseSchemeException("Type of Field is not supported");
            }

            if(annotation.fieldName().isEmpty()) {
                fieldName = TableInformation.cleanName(field.getName());
            } else {
                fieldName = TableInformation.cleanName(annotation.fieldName());
            }
        } else {
            throw new DatabaseSchemeException("Field is not annotated!");
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

    @SuppressWarnings("NullableProblems")
    @Override
    public String toString() {
        return "FieldColumn{" +
                "field=" + field.getName() +
                ", fieldName='" + fieldName + '\'' +
                ", contentType=" + contentType +
                '}';
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public boolean equals(Object obj) {
        if(obj instanceof TableInformation.DbColumn) {
            return this.getName().equals(((TableInformation.DbColumn) obj).getName());
        }
        return super.equals(obj);
    }
}
