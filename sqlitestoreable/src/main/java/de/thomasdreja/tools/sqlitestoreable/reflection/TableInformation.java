package de.thomasdreja.tools.sqlitestoreable.reflection;

import android.content.ContentValues;
import android.database.Cursor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import de.thomasdreja.tools.sqlitestoreable.template.StoreAble;

public class TableInformation {

    public interface DbColumn {
        String getName();
        ContentType getType();
        void restoreValue(StoreAble element, Cursor cursor);
        <S extends StoreAble> void storeValue(S element, ContentValues values);
    }

    protected final Class<?> storageClass;
    protected final Constructor<?> constructor;
    protected final DbColumn[] columns;
    //protected final DbField[] fields;

    public TableInformation(Class<?> storageClass) {
        this.storageClass = storageClass;

        if(StoreAble.class.isAssignableFrom(storageClass)) {
            columns = createColumns(storageClass);
            constructor = getConstructor(storageClass);
        } else {
            columns = new DbColumn[0];
            constructor = null;
        }
    }

    public <S extends StoreAble> ContentValues store(S element, String... removeColumns) {
        ContentValues values = new ContentValues();
        if(storageClass.isInstance(element)) {
            for(DbColumn column : columns) {
                for(String ignore : removeColumns) {
                    if(!column.getName().equals(ignore)) {
                        column.storeValue(element, values);
                    }
                }
            }
        }
        return values;
    }

    public StoreAble restore(Cursor cursor) {
        try {
            Object object = constructor.newInstance();
            if(storageClass.isInstance(object)) {
                final StoreAble element = (StoreAble) object;
                for(DbColumn column : columns) {
                    column.restoreValue(element, cursor);
                }
                return element;
            }
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getName() {
        return storageClass.getSimpleName();
    }

    public Class<?> getStorageClass() {
        return storageClass;
    }

    public DbColumn[] getColumns() {
        return columns;
    }

    @Override
    public String toString() {
        return "TableInformation{" +
                "storageClass=" + storageClass.getName() +
                ", constructor=" + constructor.getName() +
                ", columns=" + Arrays.toString(columns) +
                '}';
    }

    private static Constructor<?> getConstructor(Class<?> storageClass) {
        for(Constructor<?> constructor : storageClass.getDeclaredConstructors()) {
            if(constructor.getParameterTypes().length == 0) {
                return constructor;
            }
        }
        return null;
    }

    private static DbColumn[] createColumns(Class<?> storageClass) {
        ArrayList<DbColumn> columns = new ArrayList<>();
        createColumns(columns, storageClass);
        return columns.toArray(new DbColumn[0]);
    }

    private static void createColumns(ArrayList<DbColumn> columns, Class<?> storageClass) {
        for(Field field : storageClass.getDeclaredFields()) {
            if(field.getAnnotation(StoreAbleField.class) != null) {
                try {
                    columns.add(new FieldColumn(field));
                } catch (DatabaseSchemeException e) {
                    e.printStackTrace();
                }
            }
        }
        HashMap<String,Method> methodHashMap = new HashMap<>();
        StoreAbleField annotation;
        Method other;
        for(Method method : storageClass.getMethods()) {
            annotation = method.getAnnotation(StoreAbleField.class);
            if(annotation != null && !annotation.fieldName().isEmpty()) {
                if((other = methodHashMap.get(annotation.fieldName())) != null) {
                    try {
                        columns.add(new MethodColumn(method, other));
                    } catch (DatabaseSchemeException e) {
                        e.printStackTrace();
                    }
                } else {
                    methodHashMap.put(annotation.fieldName(), method);
                }
            }
        }

        if(storageClass.getSuperclass() != null) {
            createColumns(columns, storageClass.getSuperclass());
        }

        for(Class<?> mInterface : storageClass.getInterfaces()) {
            createColumns(columns, mInterface);
        }
    }

    public static TableInformation[] createInformation(Class<?>[] classes) {
        TableInformation[] array = new TableInformation[classes.length];
        for(int i=0; i < array.length; i++) {
            array[i] = new TableInformation(classes[i]);
        }
        return array;
    }

    public static String cleanName(String name) {
        return name.replaceAll("[^a-zA-Z]", "");
    }
}
