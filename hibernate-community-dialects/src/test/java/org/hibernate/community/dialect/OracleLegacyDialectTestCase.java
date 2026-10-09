/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@BaseUnitTest
public class OracleLegacyDialectTestCase {
	private StandardServiceRegistry serviceRegistry;

	@BeforeEach
	public void setUp() {
		serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder().build();
	}

	@AfterEach
	public void tearDown() {
		if ( serviceRegistry != null ) {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	@JiraKey(value = "HHH-20778")
	public void testBitDdlTypeRegistration() {
		final OracleLegacyDialect dialect = new OracleLegacyDialect();
		final TypeConfiguration typeConfiguration = new TypeConfiguration();
		dialect.contributeTypes( () -> typeConfiguration, serviceRegistry );

		assertThat( typeConfiguration.getDdlTypeRegistry().getDescriptor( SqlTypes.BIT ) ).isNotNull();
		assertThat( typeConfiguration.getDdlTypeRegistry().getTypeName( SqlTypes.BIT, dialect ) )
				.isEqualTo( "number(1,0)" );
	}
}
