/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.resource.transaction;

import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.BootstrapServiceRegistryImpl;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.internal.TransactionCoordinatorBuilderInitiator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class LegacySettingInitiatorTest {
	private BootstrapServiceRegistryImpl bsr;

	@BeforeEach
	public void before() {
		bsr = (BootstrapServiceRegistryImpl) new BootstrapServiceRegistryBuilder().build();
	}

	@AfterEach
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
