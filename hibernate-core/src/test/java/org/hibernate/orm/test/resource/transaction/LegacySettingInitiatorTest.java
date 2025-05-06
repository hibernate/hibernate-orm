/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.resource.transaction;

import java.util.Collections;

import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.BootstrapServiceRegistryImpl;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.internal.TransactionCoordinatorBuilderInitiator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * @author Steve Ebersole
 */
public class LegacySettingInitiatorTest extends BaseUnitTestCase {
	private BootstrapServiceRegistryImpl bsr;

	@Before
	public void before() {
		bsr = (BootstrapServiceRegistryImpl) new BootstrapServiceRegistryBuilder().build();
	}

	@After
	public void after() {
		if ( bsr != null ) {
			bsr.destroy();
		}
	}

	@Test
	public void testLegacySettingSelection() {
		final TransactionCoordinatorBuilderInitiator initiator = new TransactionCoordinatorBuilderInitiator();

		TransactionCoordinatorBuilder builder = initiator.initiateService(
				Collections.singletonMap(
						TransactionCoordinatorBuilderInitiator.LEGACY_SETTING_NAME,
						"org.hibernate.transaction.JDBCTransactionFactory"
				),
				bsr
		);
		assertThat( builder, instanceOf( JdbcResourceLocalTransactionCoordinatorBuilderImpl.class ) );

		builder = initiator.initiateService(
				Collections.singletonMap(
						TransactionCoordinatorBuilderInitiator.LEGACY_SETTING_NAME,
						"org.hibernate.transaction.JTATransactionFactory"
				),
				bsr
		);
		assertThat( builder, instanceOf( JtaTransactionCoordinatorBuilderImpl.class ) );

		builder = initiator.initiateService(
				Collections.singletonMap(
						TransactionCoordinatorBuilderInitiator.LEGACY_SETTING_NAME,
						"org.hibernate.transaction.CMTTransactionFactory"
				),
				bsr
		);
		assertThat( builder, instanceOf( JtaTransactionCoordinatorBuilderImpl.class ) );
	}
}
