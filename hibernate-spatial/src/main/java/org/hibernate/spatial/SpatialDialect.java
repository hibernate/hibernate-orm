/*
 * $Id: SpatialDialect.java 301 2011-05-07 18:00:18Z maesenka $
 *
 * This file is part of Hibernate Spatial, an extension to the
 * hibernate ORM solution for geographic data.
 *
 * Copyright © 2007-2010 Geovise BVBA
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
package org.hibernate.spatial;

import java.io.Serializable;

/**
 * Describes the features of a spatially enabled dialect.
 *
 * @author Karel Maesen
 */
public interface SpatialDialect extends Serializable {

    /**
     * Returns the SQL fragment for the SQL WHERE-clause when parsing
     * <code>org.hibernatespatial.criterion.SpatialRelateExpression</code>s
     * into prepared statements.
     * <p/>
     *
     * @param columnName      The name of the geometry-typed column to which the relation is
     *                        applied
     * @param spatialRelation The type of spatial relation (as defined in
     *                        <code>SpatialRelation</code>).
     * @return SQL fragment for use in the SQL WHERE-clause.
     */
    public String getSpatialRelateSQL(String columnName, int spatialRelation);

    /**
     * Returns the SQL fragment for the SQL WHERE-expression when parsing
     * <code>org.hibernate.spatial.criterion.SpatialFilterExpression</code>s
     * into prepared statements.
     *
     * @param columnName- the name of the geometry-typed column to which the filter is
     *                    be applied.
     * @return
     */
    public String getSpatialFilterExpression(String columnName);

    /**
     * @param columnName  the name of the Geometry property
     * @param aggregation the type of <code>SpatialAggregate</code>
     * @return the SQL fragment for the projection
     */
    public String getSpatialAggregateSQL(String columnName, int aggregation);

    /**
     * Returns the SQL fragment when parsing a <code>DWithinExpression</code>.
     *
     * @param columnName the geometry column to test against
     * @return
     */
    public String getDWithinSQL(String columnName);

    /**
     * Returns the SQL fragment when parsing an <code>HavingSridExpression</code>.
     *
     * @param columnName the geometry column to test against
     * @return
     */
    public String getHavingSridSQL(String columnName);


    /**
     * Returns the SQL fragment when parsing a <code>IsEmptyExpression</code> or
     * <code>IsNotEmpty</code> expression.
     *
     * @param columnName the geometry column
     * @param isEmpty    whether the geometry is tested for empty or non-empty
     * @return
     */
    public String getIsEmptySQL(String columnName, boolean isEmpty);

    /**
     * Returns true if this <code>SpatialDialect</code> supports a specific filtering function.
     * <p/>
     * This is intended to signal DB-support for fast window queries, or MBR-overlap queries
     */
    public boolean supportsFiltering();

    /**
     * Does this dialect supports the specified <code>SpatialFunction</code>.
     *
     * @param function <code>SpatialFunction</code>
     * @return true if this <code>SpatialDialect</code> supports the spatial function specified by the function parameter.
     */
    public boolean supports(SpatialFunction function);

}
