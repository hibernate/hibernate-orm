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
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = AnotherEntity.class )
public class ExplicitDomainTypeTests {
	@Test
	public void verifyMapping(DomainModelScope domainModelScope) {
		domainModelScope.withHierarchy( AnotherEntity.class, (descriptor) -> {
			final Property property = descriptor.getProperty( "thing" );
			final BasicValue valueMapping = (BasicValue) property.getValue();
			final ConverterDescriptor converterDescriptor = valueMapping.getJpaAttributeConverterDescriptor();
			assertThat( converterDescriptor ).isNotNull();
			assertThat( converterDescriptor.getAttributeConverterClass() ).isEqualTo( Thing1Converter.class );
			assertThat( converterDescriptor.getDomainValueResolvedType().getErasedType() ).isEqualTo( Thing.class );
		} );
	}
}
