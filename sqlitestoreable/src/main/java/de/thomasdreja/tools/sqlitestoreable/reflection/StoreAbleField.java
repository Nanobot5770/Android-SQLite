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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This Annotation denotes fields and methods that the StoreAble implementation will store into and restore from a database.
 * @see TableInformation
 * @see de.thomasdreja.tools.sqlitestoreable.template.TableWrapper
 * @see de.thomasdreja.tools.sqlitestoreable.template.StoreAble
 * @see de.thomasdreja.tools.sqlitestoreable.template.StoreAbleOpenHelper
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface StoreAbleField {

    /**
     * This field overrides the default name used in the database.
     * Fields in a class will usually be named with the variable name.
     * Methods should always set this parameter otherwise they will be ignored
     * @return Name of the field or empty String if the default name should be used
     * @see FieldColumn
     * @see MethodColumn
     */
    String fieldName() default "";
}
