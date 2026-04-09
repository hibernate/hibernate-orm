/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.MappingException;


/**
 * Utility class for mapping between sqltypes and hibernate type names.
 * 
 * @author max (based on parts from Sql2Java from Middlegen)
 *
 */
public final class JdbcToHibernateTypeHelper {

    private JdbcToHibernateTypeHelper() {

    }

    /** The Map containing the preferred conversion type values. */
    private static final Map<Integer, String[]> PREFERRED_HIBERNATETYPE_FOR_SQLTYPE = new HashMap<>();

    static {
        PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.TINYINT, new String[] { "byte", Byte.class.getName()} );
        PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.SMALLINT, new String[] { "short", Short.class.getName()} );
        PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.INTEGER, new String[] { "int", Integer.class.getName()} );
        PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.BIGINT, new String[] { "long", Long.class.getName()} );
        PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.REAL, new String[] { "float", Float.class.getName()} );
        PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.FLOAT, new String[] { "double", Double.class.getName()} );
        PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.DOUBLE, new String[] { "double", Double.class.getName()});
        PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.DECIMAL, new String[] { "big_decimal", "big_decimal" });
        PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.NUMERIC, new String[] { "big_decimal", "big_decimal" });
        PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.BIT, new String[] { "boolean", Boolean.class.getName()});
        PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.BOOLEAN, new String[] { "boolean", Boolean.class.getName()});
        PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.CHAR, new String[] { "char", Character.class.getName()});
        PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.VARCHAR, new String[] { "string", "string" });
        PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.LONGVARCHAR, new String[] { "string", "string" });
        PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.BINARY, new String[] { "binary", "binary" });
        PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.VARBINARY, new String[] { "binary", "binary" });
        PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.LONGVARBINARY, new String[] { "binary", "binary" });
        PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.DATE, new String[] { "date", "date" });
        PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.TIME, new String[] { "time", "time" });
        PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.TIMESTAMP, new String[] { "timestamp", "timestamp" });
        PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.CLOB, new String[] { "clob", "clob" });
        PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.BLOB, new String[] { "blob", "blob" });

        //Hibernate does not have any built-in Type for these:
        //preferredJavaTypeForSqlType.put(new Integer(Types.ARRAY), "java.sql.Array");
        //preferredJavaTypeForSqlType.put(new Integer(Types.REF), "java.sql.Ref");
        //preferredJavaTypeForSqlType.put(new Integer(Types.STRUCT), "java.lang.Object");
        //preferredJavaTypeForSqlType.put(new Integer(Types.JAVA_OBJECT), "java.lang.Object");
    }

    /* (non-Javadoc)
     * @see org.hibernate.cfg.JDBCTypeToHibernateTypesStrategy#getPreferredHibernateType(int, int, int, int)
     */
    public static String getPreferredHibernateType(int sqlType, int size, int precision, int scale, boolean nullable, boolean generatedIdentifier) {
        boolean returnNullable = nullable || generatedIdentifier;
        if ( (sqlType == Types.DECIMAL || sqlType == Types.NUMERIC) && scale <= 0) { // <=
            if (precision == 1) {
                // NUMERIC(1) is a often used idiom for storing boolean thus providing it out of the box.
                return returnNullable?Boolean.class.getName():"boolean";
            }
            else if (precision < 3) {
                return returnNullable?Byte.class.getName():"byte";
            }
            else if (precision < 5) {
                return returnNullable?Short.class.getName():"short";
            }
            else if (precision < 10) {
                return returnNullable?Integer.class.getName():"int";
            }
            else if (precision < 19) {
                return returnNullable?Long.class.getName():"long";
            }
            else {
                return "big_integer";
            }
        }

        if ( sqlType == Types.CHAR && size>1 ) {
            return "string";
        }

        String[] result = PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.get( sqlType );

        if(result==null) {
            return null;
        }
        else if(returnNullable) {
            return result[1];
        }
        else {
            return result[0];
        }
    }

    static Map<String, Integer> jdbcTypes; // Name to value
    static Map<Integer, String> jdbcTypeValues; // value to Name

    public static String[] getJDBCTypes() {
        checkTypes();

        return jdbcTypes.keySet().toArray( new String[0] );
    }

    public static int getJDBCType(String value) {
        checkTypes();

        Integer number = jdbcTypes.get(value);

        if(number==null) {
            try {
                return Integer.parseInt(value);
            }
            catch (NumberFormatException nfe) {
                throw new MappingException("jdbc-type: " + value + " is not a known JDBC Type nor a valid number");
            }
        }
        else {
            return number;
        }
    }

    private static void checkTypes() {
        if(jdbcTypes==null) {
            jdbcTypes = new HashMap<>();
            Field[] fields = Types.class.getFields();
            for ( Field field : fields ) {
                if ( Modifier.isStatic( field.getModifiers() ) ) {
                    try {
                        jdbcTypes.put( field.getName(), (Integer) field.get( Types.class ) );
                    }
                    catch (IllegalArgumentException | IllegalAccessException e) {
                        // ignore
                    }
                }
            }
        }
    }

    public static String getJDBCTypeName(int value) {
        if(jdbcTypeValues==null) {
            jdbcTypeValues = new HashMap<>();
            Field[] fields = Types.class.getFields();
            for ( Field field : fields ) {
                if ( Modifier.isStatic( field.getModifiers() ) ) {
                    try {
                        jdbcTypeValues.put( (Integer) field.get( Types.class ), field.getName() );
                    }
                    catch (IllegalArgumentException | IllegalAccessException e) {
                        // ignore
                    }
                }
            }
        }

        String name = jdbcTypeValues.get( value );

        if(name!=null) {
            return name;
        }
        else {
            return ""+value;
        }
    }

    // scale is for non floating point numeric columns
    public static boolean typeHasScale(int sqlType) {
        return (sqlType == Types.DECIMAL || sqlType == Types.NUMERIC);
    }

    // precision is for numeric columns
    public static boolean typeHasPrecision(int sqlType) {
        return (sqlType == Types.DECIMAL || sqlType == Types.NUMERIC
                || sqlType == Types.REAL || sqlType == Types.FLOAT || sqlType == Types.DOUBLE);
    }

    public static boolean typeHasScaleAndPrecision(int sqlType) {
        return typeHasScale(sqlType) && typeHasPrecision(sqlType);
    }

    // length is for string columns
    public static boolean typeHasLength(int sqlType) {
        return (sqlType == Types.CHAR || sqlType == Types.DATE
                || sqlType == Types.LONGVARCHAR || sqlType == Types.TIME || sqlType == Types.TIMESTAMP
                || sqlType == Types.VARCHAR );
    }
}
