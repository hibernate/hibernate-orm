/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.hibernate.spatial.dialect.h2gis.H2GISWkb;

import org.geolatte.geom.Geometry;

/**
 * @author Steve Ebersole
 */
@Converter(autoApply = true)
public class GeometryConverter implements AttributeConverter<Geometry, byte[]> {
	@Override
	public byte[] convertToDatabaseColumn(Geometry attribute) {
		if ( attribute == null ) {
			return null;
		}
		return H2GISWkb.to( attribute );
	}

	@Override
	public Geometry convertToEntityAttribute(byte[] dbData) {
		if ( dbData == null ) {
			return null;
		}
		return H2GISWkb.from( dbData );
	}
}
