package de.thomasdreja.tools.sqlitestoreable.template;

import de.thomasdreja.tools.sqlitestoreable.reflection.TableInformation;

public class DbComparison {

    final String where;
    private final String value;

    private DbComparison(String name, String operator, String value) {
        this.where = String.format(operator, TableInformation.cleanName(name));
        this.value = value;
    }

    String[] array() {
        return new String[] {value};
    }

    public static DbComparison equalsId(StoreAble storeAble) {
        return equalsId(storeAble.getId());
    }

    static DbComparison equalsId(long id) {
        return equals(StoreAble.ID, id);
    }

    public static DbComparison equalsRelatedId(StoreAbleCollection collection) {
        return equals(StoreAble.RELATED_ID, collection.getId());
    }

    public static DbComparison equals(TableInformation.DbColumn column, Object value) {
        return equals(column.getName(), value);
    }

    private static DbComparison equals(String name, Object value) {
        return new DbComparison(name, "%s =?", String.valueOf(value));
    }

    public static DbComparison notEquals(TableInformation.DbColumn column, Object value) {
        return new DbComparison(column.getName(), "%s !=?", String.valueOf(value));
    }

    public static DbComparison less(TableInformation.DbColumn column, Object value) {
        return new DbComparison(column.getName(), "%s <?", String.valueOf(value));
    }

    public static DbComparison lessEqual(TableInformation.DbColumn column, Object value) {
        return new DbComparison(column.getName(), "%s <=?", String.valueOf(value));
    }

    public static DbComparison greater(TableInformation.DbColumn column, Object value) {
        return new DbComparison(column.getName(), "%s <?", String.valueOf(value));
    }

    public static DbComparison greaterEqual(TableInformation.DbColumn column, Object value) {
        return new DbComparison(column.getName(), "%s <=?", String.valueOf(value));
    }

    static class Query {
        final String where;
        final String[] values;

        Query(boolean and, DbComparison... comparisons) {
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
}
