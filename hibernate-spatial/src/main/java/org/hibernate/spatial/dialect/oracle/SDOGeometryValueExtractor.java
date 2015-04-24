/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.spatial.dialect.oracle;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;


import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.db.oracle.Decoders;
import org.geolatte.geom.codec.db.oracle.SDOGeometry;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicExtractor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;


//TODO -- requires cleanup and must be made package local

/**
 * ValueExtractor for SDO_GEOMETRY
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 8/22/11
 */
public class SDOGeometryValueExtractor<X> extends BasicExtractor<X> {

	/**
	 * Creates instance
	 *
	 * @param javaDescriptor the {@code JavaTypeDescriptor} to use
	 * @param sqlTypeDescriptor the {@code SqlTypeDescriptor} to use
	 */
	public SDOGeometryValueExtractor(JavaTypeDescriptor<X> javaDescriptor, SqlTypeDescriptor sqlTypeDescriptor) {
		super( javaDescriptor, sqlTypeDescriptor );
	}

	@Override
	protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
		final Object geomObj = rs.getObject( name );
		return getJavaDescriptor().wrap( convert( geomObj ), options );
	}

	@Override
	protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
		final Object geomObj = statement.getObject( index );
		return getJavaDescriptor().wrap( convert( geomObj ), options );
	}

	@Override
	protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
		final Object geomObj = statement.getObject( name );
		return getJavaDescriptor().wrap( convert( geomObj ), options );
	}

	/**
	 * Converts an oracle to a JTS Geometry
	 *
	 * @param struct The Oracle STRUCT representation of an SDO_GEOMETRY
	 *
	 * @return The JTS Geometry value
	 */
	public Geometry convert(Object struct) {
		if ( struct == null ) {
			return null;
		}
		final SDOGeometry sdogeom = SDOGeometry.load( (Struct) struct );
		return toGeomerty( sdogeom );
	}

	private Geometry toGeomerty(SDOGeometry sdoGeom) {
		return Decoders.decode( sdoGeom );
	}

}
