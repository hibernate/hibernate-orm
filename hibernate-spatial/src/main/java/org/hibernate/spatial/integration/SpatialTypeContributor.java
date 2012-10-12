/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2007-2012 Geovise BVBA
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.hibernate.spatial.integration;

import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.TypeContributor;
import org.hibernate.spatial.GeolatteGeometryType;
import org.hibernate.spatial.JTSGeometryType;
import org.hibernate.spatial.Log;
import org.hibernate.spatial.LogFactory;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 7/27/11
 */
public class SpatialTypeContributor implements TypeContributor {

	private static final Log LOG = LogFactory.make();

	public void contribute(MetadataImplementor builder) {
		LOG.info( "Registering JTSGeometryType" );
		builder.getTypeResolver().registerTypeOverride( JTSGeometryType.INSTANCE );
        LOG.info( "Registering GeolatteGeometryType" );
		builder.getTypeResolver().registerTypeOverride( GeolatteGeometryType.INSTANCE );
	}

}
