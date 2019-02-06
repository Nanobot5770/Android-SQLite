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

package de.thomasdreja.tools.sqlitestoreable.template;

import android.database.sqlite.SQLiteDatabase;

import de.thomasdreja.tools.sqlitestoreable.reflection.TableInformation;

/**
 * This class provides a wrapper for database compare operations.
 * It allows a given column to be compared against a given value in the database.
 * The TableWrapper implementation allows for chaining of many of such comparisons.
 * @see TableWrapper#getWhere(Class, SQLiteDatabase, boolean, DbComparison...)
 * @see SQLiteDatabase#query(String, String[], String, String[], String, String, String)
 */
public class DbComparison {

    /**
     * Combines the column and the compare operation into a single string so that it can be added into the query
     */
    final String where;

    /**
     * The value that will be compared to, wrapped into a string if necessary
     */
    private final String value;

    /**
     * Creates a new comparison wrapper based upon the column, the compare operation and the value
     * @param name Column that will be compared
     * @param operator Compare operator (e.g. =,<,>,...)
     * @param value Value for the comparison, wrapper into a string if necessary
     */
    private DbComparison(String name, String operator, String value) {
        this.where = String.format(operator, TableInformation.cleanName(name));
        this.value = value;
    }

    /**
     * Wraps the single value into an array. This is useful whenever a database operation only requires a single comparison.
     * @return The value string wrapped into an array with length 1
     * @see TableWrapper#get(Class, long, SQLiteDatabase)
     */
    String[] array() {
        return new String[] {value};
    }

    /**
     * This comparison compares the ids all of objects in the database with the ID of the given object
     * @param storeAble Object with the ID for comparison
     * @return A comparison that returns all IDs that match given the given one
     * @see StoreAble#ID
     */
    @SuppressWarnings("WeakerAccess")
    public static DbComparison equalsId(StoreAble storeAble) {
        return equalsId(storeAble.getId());
    }

    /**
     * This comparison compares the ids all of objects in the database with the given ID
     * @param id ID to be compared against
     * @return A comparison that returns all IDs that match given the given one
     * @see StoreAble#ID
     */
    static DbComparison equalsId(long id) {
        return equals(StoreAble.ID, id);
    }

    /**
     * This comparison compares the related IDs all of objects in the database with ID of the collection
     * @param collection Collection that serves as parent for the database objects
     * @return A comparison that returns all related IDs that match given the collection
     * @see StoreAble#RELATED_ID
     */
    @SuppressWarnings("WeakerAccess")
    public static DbComparison equalsRelatedId(StoreAbleCollection collection) {
        return equals(StoreAble.RELATED_ID, collection.getId());
    }

    /**
     * Comparison: Column Value = Value
     * @param column Column to be compared
     * @param value Value to be compared
     * @return Comparison CV = V
     */
    @SuppressWarnings("unused")
    public static DbComparison equals(TableInformation.DbColumn column, Object value) {
        return equals(column.getName(), value);
    }

    /**
     * Comparison: Column Value = Value
     * @param name Name of the Column to be compared
     * @param value Value to be compared
     * @return Comparison CV = V
     */
    @SuppressWarnings("unused")
    private static DbComparison equals(String name, Object value) {
        return new DbComparison(name, "%s =?", String.valueOf(value));
    }

    /**
     * Comparison: Column Value != Value
     * @param column Column to be compared
     * @param value Value to be compared
     * @return Comparison CV != V
     */
    @SuppressWarnings("unused")
    public static DbComparison notEquals(TableInformation.DbColumn column, Object value) {
        return new DbComparison(column.getName(), "%s !=?", String.valueOf(value));
    }

    /**
     * Comparison: Column Value < Value
     * @param column Column to be compared
     * @param value Value to be compared
     * @return Comparison CV < V
     */
    @SuppressWarnings("unused")
    public static DbComparison less(TableInformation.DbColumn column, Object value) {
        return new DbComparison(column.getName(), "%s <?", String.valueOf(value));
    }

    /**
     * Comparison: Column Value <= Value
     * @param column Column to be compared
     * @param value Value to be compared
     * @return Comparison CV <= V
     */
    @SuppressWarnings("unused")
    public static DbComparison lessEqual(TableInformation.DbColumn column, Object value) {
        return new DbComparison(column.getName(), "%s <=?", String.valueOf(value));
    }

    /**
     * Comparison: Column Value > Value
     * @param column Column to be compared
     * @param value Value to be compared
     * @return Comparison CV > V
     */
    @SuppressWarnings("unused")
    public static DbComparison greater(TableInformation.DbColumn column, Object value) {
        return new DbComparison(column.getName(), "%s >?", String.valueOf(value));
    }

    /**
     * Comparison: Column Value >= Value
     * @param column Column to be compared
     * @param value Value to be compared
     * @return Comparison CV >= V
     */
    @SuppressWarnings("unused")
    public static DbComparison greaterEqual(TableInformation.DbColumn column, Object value) {
        return new DbComparison(column.getName(), "%s >=?", String.valueOf(value));
    }

    /**
     * This class takes multiple comparisons and merges them into one query for the database
     * @see DbComparison
     */
    static class DbQuery {
        /**
         * Combines all fields and operators into one string (merged with either AND or OR)
         * @see DbComparison#where
         */
        final String where;

        /**
         * All values that will be compared with this query
         * @see DbComparison#value
         */
        final String[] values;

        /**
         * Creates a new query by merging the given comparisons (with either AND or OR)
         * @param and True: Merging with AND, False: Merging with OR
         * @param comparisons Comparisons that compare columns to values
         * @see TableWrapper#getWhere(Class, SQLiteDatabase, boolean, DbComparison...)
         * @see SQLiteDatabase#query(String, String[], String, String[], String, String, String)
         */
        DbQuery(boolean and, DbComparison... comparisons) {
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

        /**
         * Returns whether the query is valid aka there are values to be compared against
         * @return True: Query can be run on the database, False: Query has no values and should be ignored
         */
        boolean isValid() {
            return values.length > 0;
        }
    }
}
