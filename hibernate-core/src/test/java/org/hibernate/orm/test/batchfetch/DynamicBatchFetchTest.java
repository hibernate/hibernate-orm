/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batchfetch;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.collections.CollectionHelper;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		annotatedClasses = { A.class, B.class }
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false")
)
public class DynamicBatchFetchTest {
	private static int currentId = 1;

	@Test
	public void testDynamicBatchFetch(SessionFactoryScope scope) {
		currentId = 1;

		Integer aId1 = createAAndB( "foo_1", scope );
		Integer aId2 = createAAndB( "foo_2", scope );

		scope.inTransaction(
				session -> {
					List resultList = session.createQuery( "from A where id in (" + aId1 + "," + aId2 + ") order by id" )
							.list();
					assertThat( resultList ).isNotEmpty();
					A a1 = (A) resultList.get( 0 );
					A a2 = (A) resultList.get( 1 );
					assertEquals( aId1, a1.getId() );
					assertEquals( aId2, a2.getId() );
					assertFalse( Hibernate.isInitialized( a1.getB() ) );
					assertFalse( Hibernate.isInitialized( a2.getB() ) );
					B b = a1.getB();
					assertFalse( Hibernate.isInitialized( b ) );
					assertEquals( "foo_1", b.getOtherProperty() );
					assertTrue( Hibernate.isInitialized( a1.getB() ) );
					assertTrue( Hibernate.isInitialized( a2.getB() ) );
					assertEquals( "foo_2", a2.getB().getOtherProperty() );
				}
		);
	}

	private int createAAndB(String otherProperty, SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					B b = new B( currentId, currentId );
					b.setOtherProperty( otherProperty );
					session.persist( b );

					A a = new A( currentId );
					a.setB( b );

					session.persist( a );
				}
		);

		currentId++;

		return currentId - 1;
	}

	/**
	 * Tests the handling of multi-loads with regard to BatchLoadSizingStrategy and correlation
	 * to batch fetching.  Show that both suffer from the previous behavior of always using the
	 * mapped {@link BatchSize#size()} which leads to predicates with too many parameters with
	 * large batch-sizes
	 */
	@Test
	public void testMultiLoad(SessionFactoryScope scope) {
		final List<BId> bIdList = CollectionHelper.arrayList( 2000 );
		scope.inTransaction( (session) -> {
			for ( int i = 0; i < 2000; i++ ) {
				bIdList.add( new BId( i, i ) );
				final B b = new B( i, i );
				session.persist( b );

				A a = new A( i );
				a.setB( b );

				session.persist( b );
				session.persist( a );
			}
		} );

		scope.inTransaction( (session) -> {
			final List<B> list = session.byMultipleIds( B.class ).multiLoad( bIdList );
			assertThat( list ).isNotEmpty();
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
