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
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MySQLMetaDataDialect extends JDBCMetaDataDialect {

    /**
     * Based on info from <a href="http://dev.mysql.com/doc/refman/5.0/en/show-table-status.html">...</a>
     * Should work on pre-mysql 5 too since it uses the "old" SHOW TABLE command instead of SELECT from infotable.
     */
    public Iterator<Map<String, Object>> getSuggestedPrimaryKeyStrategyName(String catalog, String schema, String table) {
        String sql;
        try {
            catalog = caseForSearch( catalog );
            schema = caseForSearch( schema );
            table = caseForSearch( table );

            log.debug("geSuggestedPrimaryKeyStrategyName(" + catalog + "." + schema + "." + table + ")");

            sql = "show table status " + (catalog==null?"":" from " + catalog + " ") + (table==null?"":" like '" + table + "' ");
            PreparedStatement statement = getConnection().prepareStatement( sql );

            final String sc = schema;
            final String cat = catalog;
            return new ResultSetIterator(statement.executeQuery()) {

                final Map<String, Object> element = new HashMap<>();
                protected Map<String, Object> convertRow(ResultSet tableRs) throws SQLException {
                    element.clear();
                    element.put("TABLE_NAME", tableRs.getString("NAME"));
                    element.put("TABLE_SCHEM", sc);
                    element.put("TABLE_CAT", cat);

                    String string = tableRs.getString("AUTO_INCREMENT");
                    if(string==null) {
                        element.put("HIBERNATE_STRATEGY", null);
                    }
                    else {
                        element.put("HIBERNATE_STRATEGY", "identity");
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

    @Override
    public Iterator<Map<String,Object>> getTables(
            String xcatalog,
            String xschema,
            String xtable) {
        // MySql JDBC Driver doesn't like 'null' values for the table search pattern, use '%' instead
        return super.getTables(xcatalog, xschema, xtable != null ? xtable : "%");
    }

    public Iterator<Map<String, Object>> getColumns(
            String xcatalog,
            String xschema,
            String xtable,
            String xcolumn) {
        // MySql JDBC Driver doesn't like 'null' values for the table and column search patterns, use '%' instead
        return super.getColumns(
                xcatalog,
                xschema,
                xtable != null ? xtable : "%",
                xcolumn != null ? xcolumn : "%");
    }

}
