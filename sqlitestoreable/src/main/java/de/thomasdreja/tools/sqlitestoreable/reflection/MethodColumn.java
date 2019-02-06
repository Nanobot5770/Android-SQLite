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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import de.thomasdreja.tools.sqlitestoreable.template.StoreAble;

/**
 * This class allows for the storing and restoring of a value in the database with a getter and setter method instead of a field.
 * Both methods in the class must share the same field name in their annotation
 * @see StoreAbleField#fieldName()
 * @see FieldColumn
 * @see TableInformation
 */
public class MethodColumn implements TableInformation.DbColumn {

    /**
     * Getter method to store the value into the database
     */
    private final Method get;

    /**
     * Setter method to restore the value from the database
     */
    private final Method set;

    /**
     * Name of the field in the database. Must be added in the annotation
     * @see StoreAbleField#fieldName()
     */
    private final String fieldName;

    /**
     * Type of content these methods return or receive
     */
    private final ContentType contentType;

    /**
     * Wraps the given methods for database storage.
     * The order of the methods does not matter, however there has to be on with a return value and one with only one parameter, both of the same type.
     * @param methodA First method, may be getter or setter
     * @param methodB Second method, may be getter or setter
     * @throws DatabaseSchemeException Thrown either when: The methods are not annotated, there is not a getter or setter method,
     * the field names are empty or types or different from each other.
     */
    MethodColumn(Method methodA, Method methodB) throws DatabaseSchemeException {
        final StoreAbleField annA = methodA.getAnnotation(StoreAbleField.class);
        final StoreAbleField annB = methodB.getAnnotation(StoreAbleField.class);
        if(annA != null && annB != null
                && !annA.fieldName().isEmpty()
                && annA.fieldName().equals(annB.fieldName())) {

            if(isSetter(methodA) && isGetter(methodB)) {
                this.set = methodA;
                this.get = methodB;
            } else if(isSetter(methodB) && isGetter(methodA)){
                this.set = methodB;
                this.get = methodA;
            } else {
                throw new DatabaseSchemeException("Methods are not a get & set pair!");
            }

            if(get.getReturnType() == set.getParameterTypes()[0]) {
                fieldName = TableInformation.cleanName(annA.fieldName());
                contentType = ContentType.getTypeOf(get.getReturnType());

                if(contentType == ContentType.UNSUPPORTED) {
                    throw new DatabaseSchemeException("Type of the methods is not supported");
                }

                get.setAccessible(true);
                set.setAccessible(true);
            } else {
                throw new DatabaseSchemeException("Get and Set Types do not match!");
            }
        } else {
            throw new DatabaseSchemeException("Methods are not marked correctly!");
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
        if(set.getDeclaringClass().isInstance(element)) {
            try {
                set.invoke(element, contentType.restore(fieldName, cursor));
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public <S extends StoreAble> void storeValue(S element, ContentValues values) {
        if(get.getDeclaringClass().isInstance(element)) {
            try {
                contentType.store(fieldName, get.invoke(element), values);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public String toString() {
        return "MethodColumn{" +
                "get=" + get.getName() +
                ", set=" + set.getName() +
                ", fieldName='" + fieldName + '\'' +
                ", contentType=" + contentType +
                '}';
    }

    /**
     * Returns whether a method is a setter, aka it has one parameter and returns void.
     * @param method Method to be checked
     * @return True: Method is a setter method, False: It is not
     */
    private static boolean isSetter(Method method) {
        return method.getParameterTypes().length == 1 && method.getReturnType() == Void.TYPE;
    }

    /**
     * Return whether a method is a getter, aka it has no parameters and the return type is not void.
     * @param method Method to be checked
     * @return True: Method is a getter method, False: It is not
     */
    private static boolean isGetter(Method method) {
        return method.getParameterTypes().length == 0 && method.getReturnType() != Void.TYPE;
    }
}
