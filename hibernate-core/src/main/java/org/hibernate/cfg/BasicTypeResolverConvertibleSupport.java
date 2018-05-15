/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cfg;

import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;

/**
 * Standard support for BasicTypeResolver impls that represent
 * convertible (as in AttributeConverter) values.
 *
 * @author Steve Ebersole
 */
public abstract class BasicTypeResolverConvertibleSupport
		extends BasicTypeResolverSupport
		implements JdbcRecommendedSqlTypeMappingContext {
	private final ConverterDescriptor converterDescriptor;

	public BasicTypeResolverConvertibleSupport(
			MetadataBuildingContext buildingContext,
			ConverterDescriptor converterDescriptor) {
		super( buildingContext );
		this.converterDescriptor = converterDescriptor;
	}

	@Override
	public ConverterDescriptor getAttributeConverterDescriptor() {
		return converterDescriptor;

	}
}
