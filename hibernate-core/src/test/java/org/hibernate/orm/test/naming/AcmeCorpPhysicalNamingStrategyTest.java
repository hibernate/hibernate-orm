/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.naming;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class AcmeCorpPhysicalNamingStrategyTest {
	private AcmeCorpPhysicalNamingStrategy strategy = new AcmeCorpPhysicalNamingStrategy();
	private StandardServiceRegistry serviceRegistry;

	@BeforeEach
	public void prepareServiceRegistry() {
		serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder()
				.build();
	}

	@AfterEach
	public void releaseServiceRegistry() {
		if ( serviceRegistry != null ) {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	public void testTableNaming() {
		{
			Identifier in = Identifier.toIdentifier( "accountNumber" );
			Identifier out = strategy.toPhysicalTableName( in, serviceRegistry.getService( JdbcEnvironment.class ) );
			assertThat( out.getText() ).isEqualTo( "acct_num" );
		}
	}
}
