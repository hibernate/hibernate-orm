/**
 * This file is part of Hibernate Spatial, an extension to the 
 * hibernate ORM solution for geographic data. 
 *
 * Copyright © 2007 Geovise BVBA
 * Copyright © 2007 K.U. Leuven LRD, Spatial Applications Division, Belgium
 *
 * This work was partially supported by the European Commission, 
 * under the 6th Framework Programme, contract IST-2-004688-STP.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, visit: http://www.hibernatespatial.org/
 */
package org.hibernate.spatial.dialect.mysql;

import org.hibernate.HibernateException;
import org.hibernate.dialect.MySQLInnoDBDialect;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import java.util.Iterator;
import java.util.Map;

/**
 * Extends the MySQLInnoDBDialect by also including information on spatial operators,
 * constructors and processing functions. This is a mere wrapper class for dialect
 * functionality defined in MySQLSpatialDialect
 *
 * @author Boni Gopalan
 */
public class MySQLSpatialInnoDBDialect extends MySQLInnoDBDialect implements SpatialDialect {


    private MySQLSpatialDialect dialectDelegate = new MySQLSpatialDialect();


    public MySQLSpatialInnoDBDialect() {
        super();
                Map<String, StandardSQLFunction> functionsToRegister = dialectDelegate.getFunctionsToRegister();
        Map<String, Integer> columnTypes = dialectDelegate.getColumnTypesToRegister();
        if (null != columnTypes) {
            Iterator<String> keys = columnTypes.keySet().iterator();
            while (keys.hasNext()) {
                String aKey = keys.next();
                registerColumnType(columnTypes.get(aKey), aKey);
            }
        }

        if (null != functionsToRegister) {
            Iterator<String> keys = functionsToRegister.keySet().iterator();
            while (keys.hasNext()) {
                String aKey = keys.next();
                registerFunction(aKey, functionsToRegister.get(aKey));

            }
        }
    }

    @Override
    public SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
        return dialectDelegate.remapSqlTypeDescriptor(sqlTypeDescriptor);
    }

    @Override
    public String getTypeName(int code, long length, int precision, int scale) throws HibernateException {
        return dialectDelegate.getTypeName(code, length, precision, scale);
    }

    public String getSpatialRelateSQL(String columnName, int spatialRelation) {
        return dialectDelegate.getSpatialRelateSQL(columnName, spatialRelation);
    }

    public String getSpatialFilterExpression(String columnName) {
        return dialectDelegate.getSpatialFilterExpression(columnName);
    }

    public String getSpatialAggregateSQL(String columnName, int aggregation) {
        return dialectDelegate.getSpatialAggregateSQL(columnName, aggregation);
    }

    public String getDWithinSQL(String columnName) {
        return dialectDelegate.getDWithinSQL(columnName);
    }

    public String getHavingSridSQL(String columnName) {
        return dialectDelegate.getHavingSridSQL(columnName);
    }

    public String getIsEmptySQL(String columnName, boolean isEmpty) {
        return dialectDelegate.getIsEmptySQL(columnName, isEmpty);
    }

    public String getDbGeometryTypeName() {
        return dialectDelegate.getDbGeometryTypeName();
    }

    public boolean supportsFiltering() {
        return dialectDelegate.supportsFiltering();
    }

    public boolean supports(SpatialFunction function) {
        return dialectDelegate.supports(function);
    }

}
