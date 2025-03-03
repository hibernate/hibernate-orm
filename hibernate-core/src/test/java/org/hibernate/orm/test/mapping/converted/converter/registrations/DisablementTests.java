/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.registrations;

import java.sql.Types;
import java.util.Objects;

import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.junit.jupiter.api.Test;

import org.assertj.core.api.Condition;

import static java.sql.Types.LONGVARBINARY;
import static java.sql.Types.VARBINARY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for registering converters enabled for auto-apply
 *
 * @implNote Without conversion, `thing1` and `thing2` will be treated
 * as binary data via serialization
 *
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = TheEntity2.class )
public class DisablementTests {
	@Test
	public void verifyMapping(DomainModelScope scope) {
		scope.withHierarchy( TheEntity2.class, (descriptor) -> {
			{
				final Property property = descriptor.getProperty( "thing1" );
				final BasicValue valueMapping = (BasicValue) property.getValue();
				final ConverterDescriptor converterDescriptor = valueMapping.getJpaAttributeConverterDescriptor();
				assertThat( converterDescriptor ).isNull();
				assertThat( valueMapping.resolve().getJdbcType().getJdbcTypeCode() ).is( oneOf(
						Types.BINARY, VARBINARY, LONGVARBINARY
				) );
			}
			{
				final Property property = descriptor.getProperty( "thing2" );
				final BasicValue valueMapping = (BasicValue) property.getValue();
				final ConverterDescriptor converterDescriptor = valueMapping.getJpaAttributeConverterDescriptor();
				assertThat( converterDescriptor ).isNull();
				assertThat( valueMapping.resolve().getJdbcType().getJdbcTypeCode() ).is( oneOf( VARBINARY, LONGVARBINARY ) );
			}
		} );
	}

	private static <X> Condition<X> oneOf(X... values) {
		return new OneOfCondition<>( values );
	}

	private static class OneOfCondition<X> extends Condition<X> {
		public OneOfCondition(X... values) {
			super(
					(value) -> {
						for ( int i = 0; i < values.length; i++ ) {
							if ( Objects.equals( value, values[i] ) ) {
								return true;
							}
						}
						return false;
					},
					"one of"
			);
		}
	}
}
