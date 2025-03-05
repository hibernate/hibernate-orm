/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.registrations;

import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for registering converters enabled for auto-apply
 *
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = TheEntity.class )
public class EnablementTests {
	@Test
	public void verifyMapping(DomainModelScope scope) {
		scope.withHierarchy( TheEntity.class, (descriptor) -> {
			{
				final Property property = descriptor.getProperty( "thing1" );
				final BasicValue valueMapping = (BasicValue) property.getValue();
				final ConverterDescriptor converterDescriptor = valueMapping.getJpaAttributeConverterDescriptor();
				assertThat( converterDescriptor ).isNotNull();
				assertThat( converterDescriptor.getAttributeConverterClass() ).isEqualTo( Thing1Converter.class );
				assertThat( converterDescriptor.getDomainValueResolvedType().getErasedType() ).isEqualTo( Thing1.class );
			}
			{
				final Property property = descriptor.getProperty( "thing2" );
				final BasicValue valueMapping = (BasicValue) property.getValue();
				final ConverterDescriptor converterDescriptor = valueMapping.getJpaAttributeConverterDescriptor();
				assertThat( converterDescriptor ).isNotNull();
				assertThat( converterDescriptor.getAttributeConverterClass() ).isEqualTo( Thing2Converter.class );
				assertThat( converterDescriptor.getDomainValueResolvedType().getErasedType() ).isEqualTo( Thing2.class );
			}
		} );
	}
}
