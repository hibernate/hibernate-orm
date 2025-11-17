/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter.subclass;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.SharedSessionContract;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.orm.test.filter.AbstractStatefulStatelessFilterTest;

import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class SubClassTest extends AbstractStatefulStatelessFilterTest {

	@BeforeEach
	void prepareTest() {
		scope.inTransaction( this::persistTestData );
	}

	protected abstract void persistTestData(SessionImplementor session);

	@AfterEach
	protected void cleanupTest() throws Exception {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	public void testIqFilter(BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		inTransaction.accept( scope, session -> {
			assertCount( session, 3 );
			session.enableFilter( "iqRange" ).setParameter( "min", 101 ).setParameter( "max", 140 );
			assertCount( session, 1 );
		} );
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	public void testPregnantFilter(BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		inTransaction.accept( scope, session -> {
			assertCount( session, 3 );
			session.enableFilter( "pregnantOnly" );
			assertCount( session, 1 );
		} );
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	public void testNonHumanFilter(BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		inTransaction.accept( scope, session -> {
			assertCount( session, 3 );
			session.enableFilter( "ignoreSome" ).setParameter( "name", "Homo Sapiens" );
			assertCount( session, 0 );
		} );
	}

	private void assertCount(SharedSessionContract session, long expected) {
		long count = (Long) session.createQuery( "select count(h) from Human h" ).uniqueResult();
		assertEquals( expected, count );
	}
}
