/*
 * $Id: SpatialDialectProvider.java 200 2010-03-31 19:52:12Z maesenka $
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
package org.hibernate.spatial.spi;

import org.hibernate.spatial.SpatialDialect;

/**
 * Interface that is implemented by a SpatialDialect Provider.
 * <p/>
 * A <class>SpatialDialectProvider</class> creates a SpatialDialect for one or
 * more database systems. These databases are identified by a dialect string.
 * Usually this is the fully qualified class name of a
 * <code>org.hibernate.dialect.Dialect</code> or <code>SpatialDialect</code>
 * implementation
 *
 * @author Karel Maesen, Geovise BVBA
 */

public interface SpatialDialectProvider {

    /**
     * create Spatial Dialect with the provided name.
     *
     * @param dialect Name of the dialect to create.
     * @return a SpatialDialect
     */
    public SpatialDialect createSpatialDialect(String dialect);

    /**
     * Returns the default dialect for this provider.
     *
     * @return The Default Dialect provided by the implementation.
     *         <p/>
     *         Implementations should never return null for this method.
     */
    public SpatialDialect getDefaultDialect();

    /**
     * Returns the Dialect names
     * <p/>
     * This method must return the canonical class names of the Spatialdialect
     * implementations that this provider provides.
     *
     * @return array of dialect names.
     */
    public String[] getSupportedDialects();

}
