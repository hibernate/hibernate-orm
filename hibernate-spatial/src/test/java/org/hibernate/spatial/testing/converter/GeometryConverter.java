/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.spatial.testing.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.hibernate.spatial.dialect.h2geodb.GeoDbWkb;

import org.geolatte.geom.Geometry;

/**
 * @author Steve Ebersole
 */
@Converter( autoApply = true )
public class GeometryConverter implements AttributeConverter<Geometry,byte[]> {
	@Override
	public byte[] convertToDatabaseColumn(Geometry attribute) {
		if ( attribute == null ) {
			return null;
		}
		return GeoDbWkb.to( attribute );
	}

	@Override
	public Geometry convertToEntityAttribute(byte[] dbData) {
		if ( dbData == null ) {
			return null;
		}
		return GeoDbWkb.from( dbData );
	}
}
