/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cfg;

import org.hibernate.boot.model.type.spi.BasicTypeResolver;
import org.hibernate.boot.spi.AttributeConverterDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.spi.BasicTypeParameters;

/**
 * Standard support for BasicTypeResolver impls that represent
 * convertible (as in AttributeConverter) values.
 *
 * @author Steve Ebersole
 */
public abstract class BasicTypeResolverConvertibleSupport
		extends BasicTypeResolverSupport
		implements BasicTypeResolver, JdbcRecommendedSqlTypeMappingContext, BasicTypeParameters {
	private final AttributeConverterDescriptor converterDescriptor;

	public BasicTypeResolverConvertibleSupport(
			MetadataBuildingContext buildingContext,
			AttributeConverterDescriptor converterDescriptor) {
		super( buildingContext );
		this.converterDescriptor = converterDescriptor;
	}

	@Override
	public AttributeConverterDefinition getAttributeConverterDefinition() {
		return converterDescriptor;

	}
}
