/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FilterImplSerializationTest {

	@Test
	public void testValidateAfterDeserialize() throws Exception {
		final FilterDefinition definition = buildDefinition();
		final FilterImpl filter = new FilterImpl( definition );
		filter.setParameter( "param", "value" );
		filter.validate();

		final FilterImpl deserialized = (FilterImpl) SerializationHelper.deserialize(
				SerializationHelper.serialize( filter ),
				FilterImpl.class.getClassLoader()
		);

		assertThat( readValidated( deserialized ) ).isFalse();

		final SessionFactoryImplementor factory = mock( SessionFactoryImplementor.class );
		when( factory.getFilterDefinition( "testFilter" ) ).thenReturn( definition );

		deserialized.afterDeserialize( factory );

		assertThat( readValidated( deserialized ) ).isTrue();
		deserialized.setParameter( "param", "updated" );
		assertThat( readValidated( deserialized ) ).isFalse();
		deserialized.validate();
		assertThat( readValidated( deserialized ) ).isTrue();
		deserialized.setParameterList( "param", Arrays.asList( "list-value" ) );
		assertThat( readValidated( deserialized ) ).isFalse();
		deserialized.validate();
		assertThat( readValidated( deserialized ) ).isTrue();
	}

	private static FilterDefinition buildDefinition() {
		final TypeConfiguration typeConfiguration = new TypeConfiguration();
		final JdbcMapping mapping =
				typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING );
		final Map<String, JdbcMapping> paramMappings = new HashMap<>();
		paramMappings.put( "param", mapping );
		return new FilterDefinition( "testFilter", "col = :param", paramMappings );
	}

	private static boolean readValidated(FilterImpl filter) throws Exception {
		final Field validatedField = FilterImpl.class.getDeclaredField( "validated" );
		validatedField.setAccessible( true );
		return validatedField.getBoolean( filter );
	}

}
