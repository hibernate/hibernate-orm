/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.bytecode;

import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Emmanuel Bernard
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {Tool.class, Hammer.class})
@SessionFactory
public class ProxyBreakingTest {
	@Test
	public void testProxiedBridgeMethod(SessionFactoryScope factoryScope) {
		final Hammer persisted = factoryScope.fromTransaction( (session) -> {
			final Hammer hammer = new Hammer();
			session.persist( hammer );
			return hammer;
		} );

		assertThat( persisted.getId() ).isNotNull();

		factoryScope.inTransaction( (session) -> {
			final Hammer reference = session.getReference( Hammer.class, persisted.getId() );
			assertThat( Hibernate.isInitialized( reference ) ).isFalse();
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}
}
