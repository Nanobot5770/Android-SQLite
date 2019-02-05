package de.thomasdreja.tools.sqlitestoreable.reflection;

import android.content.ContentValues;
import android.database.Cursor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import de.thomasdreja.tools.sqlitestoreable.template.StoreAble;

public class MethodColumn implements TableInformation.DbColumn {

    private final Method get;
    private final Method set;
    private final String fieldName;
    private final ContentType contentType;

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

    @Override
    public String toString() {
        return "MethodColumn{" +
                "get=" + get.getName() +
                ", set=" + set.getName() +
                ", fieldName='" + fieldName + '\'' +
                ", contentType=" + contentType +
                '}';
    }

    private static boolean isSetter(Method method) {
        return method.getParameterTypes().length == 1 && method.getReturnType() == Void.TYPE;
    }

    private static boolean isGetter(Method method) {
        return method.getParameterTypes().length == 0 && method.getReturnType() != Void.TYPE;
    }
}
