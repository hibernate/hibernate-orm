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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class AcmeCorpPhysicalNamingStrategyTest {
	private AcmeCorpPhysicalNamingStrategy strategy = new AcmeCorpPhysicalNamingStrategy();
	private StandardServiceRegistry serviceRegistry;

	@Before
	public void prepareServiceRegistry() {
		serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder()
				.build();
	}

	@After
	public void releaseServiceRegistry() {
		if (serviceRegistry != null) {
			StandardServiceRegistryBuilder.destroy(serviceRegistry);
		}
	}

	@Test
	public void testTableNaming() {
		{
			Identifier in = Identifier.toIdentifier("accountNumber");
			Identifier out = strategy.toPhysicalTableName(in, serviceRegistry.getService(JdbcEnvironment.class));
			assertThat(out.getText(), equalTo("acct_num"));
		}
	}
}
