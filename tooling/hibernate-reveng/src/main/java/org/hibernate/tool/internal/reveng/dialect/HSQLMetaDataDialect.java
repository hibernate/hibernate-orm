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
package org.hibernate.tool.internal.reveng.dialect;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.tool.internal.util.TableNameQualifier;

/**
 * @author Dmitry Geraskov
 *
 */
public class HSQLMetaDataDialect extends JDBCMetaDataDialect {

    private String quote(String columnName) {
        if(columnName==null) return null;
        if(needQuote(columnName)) {
            if(columnName.length()>1 && columnName.charAt(0)=='\"' && columnName.charAt(columnName.length()-1)=='\"') {
                return columnName; // avoid double quoting
            }
            return "\"" + columnName + "\"";
        }
        else {
            return columnName;
        }
    }

    public Iterator<Map<String, Object>> getSuggestedPrimaryKeyStrategyName(String catalog, String schema, String table) {
        try {
            catalog = caseForSearch( catalog );
            schema = caseForSearch( schema );
            table = caseForSearch( table );

            //log.debug("geSuggestedPrimaryKeyStrategyName(" + catalog + "." + schema + "." + table + ")");

            final String sc = schema;
            final String cat = catalog;
            return new ResultSetIterator(getMetaData().getTables(catalog, schema, table, new String[]{"TABLE"})) {

                final Map<String, Object> element = new HashMap<>();
                protected Map<String, Object> convertRow(ResultSet tableRs) throws SQLException{
                    String table = tableRs.getString("TABLE_NAME");
                    String fullTableName = TableNameQualifier.qualify(quote(cat), quote(sc), quote(table));

                    String sql ="SELECT * FROM " + fullTableName + " WHERE 0>1"; // can't use FALSE constant since it would not work with older HSQL versions. (JBIDE-5957)
                    boolean isAutoIncrement = false;

                    try (PreparedStatement statement = getConnection().prepareStatement( sql )) {
                        element.clear();
                        element.put( "TABLE_NAME", table );
                        element.put( "TABLE_SCHEM", sc );
                        element.put( "TABLE_CAT", null );

                        ResultSet rs = statement.executeQuery();
                        ResultSetMetaData rsmd = rs.getMetaData();
                        for ( int i = 0; i < rsmd.getColumnCount(); i++ ) {
                            isAutoIncrement = rsmd.isAutoIncrement( i + 1 );
                            if ( isAutoIncrement ) break;
                        }

                    }
                    catch (SQLException e) {
                        //log error and set HIBERNATE_STRATEGY to null
                        log.debug(
                                "Error while getting suggested primary key strategy for " + fullTableName + ". Falling back to default strategy.",
                                e );
                    }

                    if(isAutoIncrement) {
                        element.put("HIBERNATE_STRATEGY", "identity");
                    }
                    else {
                        element.put("HIBERNATE_STRATEGY", null);
                    }
                    return element;
                }
                protected Throwable handleSQLException(SQLException e) {
                    // schemaRs and catalogRs are only used for error reporting if
                    // we get an exception
                    throw new RuntimeException(
                            "Could not get list of suggested identity strategies from database. Probably a JDBC driver problem. ", e);
                }
            };
        }
        catch (SQLException e) {
            throw new RuntimeException("Could not get list of suggested identity strategies from database. Probably a JDBC driver problem. ", e);
        }
    }
}
