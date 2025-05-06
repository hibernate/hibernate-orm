/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.attributebinder;

import java.sql.Types;

import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = { System.class, YesNo.class } )
@SessionFactory
public class SimpleAttributeBinderTests {
	@Test
	public void verifyBootModel(DomainModelScope scope) {
		scope.withHierarchy( System.class, (descriptor) -> {
			final Property activeProp = descriptor.getProperty( "active" );
			final BasicValue activeMapping = (BasicValue) activeProp.getValue();

			assertThat( activeMapping.getJpaAttributeConverterDescriptor() ).isNotNull();

			final BasicValue.Resolution<?> resolution = activeMapping.resolve();
			final JdbcTypeRegistry jdbcTypeRegistry = activeMapping.getBuildingContext()
					.getBuildingOptions()
					.getTypeConfiguration()
					.getJdbcTypeRegistry();

			assertThat( resolution.getDomainJavaType().getJavaType() ).isEqualTo( Boolean.class );
			assertThat( resolution.getRelationalJavaType().getJavaType() ).isEqualTo( Character.class );
			assertThat( resolution.getJdbcType() ).isEqualTo( jdbcTypeRegistry.getDescriptor( Types.CHAR ) );
			assertThat( resolution.getValueConverter() ).isNotNull();
		} );
	}

	@Test
	public void basicTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "from System" ).list();
		} );
	}
}
