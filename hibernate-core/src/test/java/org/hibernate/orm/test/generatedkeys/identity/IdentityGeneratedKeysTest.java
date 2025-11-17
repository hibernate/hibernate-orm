/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.generatedkeys.identity;

import jakarta.persistence.TransactionRequiredException;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/generatedkeys/identity/MyEntity.hbm.xml"
)
@SessionFactory(
		generateStatistics = true
)
public class IdentityGeneratedKeysTest {

	@Test
	public void testIdentityColumnGeneratedIds(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					MyEntity myEntity = new MyEntity( "test" );
					session.persist( myEntity );
					assertThat( myEntity.getId() )
							.describedAs( "identity column did not force immediate insert" )
							.isNotNull();
					session.remove( myEntity );
				}
		);
	}

	@Test
	public void testPersistOutsideTransaction(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					try {
						StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
						long initialInsertCount = statistics.getEntityInsertCount();
						MyEntity myEntity2 = new MyEntity( "test-persist" );
						session.persist( myEntity2 );
						assertThat( statistics.getEntityInsertCount() )
								.describedAs( "persist on identity column not delayed" )
								.isEqualTo( initialInsertCount );
						assertThat( myEntity2.getId() ).isNull();

						// an explicit flush should cause execution of the delayed insertion
						session.flush();
						fail( "TransactionRequiredException required upon flush" );
					}
					catch (TransactionRequiredException ex) {
						// expected
					}
				}
		);
	}

	@Test
	public void testPersistOutsideTransactionCascadedToNonInverseCollection(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();

		long initialInsertCount = statistics.getEntityInsertCount();
		scope.inSession(
				session -> {
					try {
						MyEntity myEntity = new MyEntity( "test-persist" );
						myEntity.getNonInverseChildren().add( new MyChild( "test-child-persist-non-inverse" ) );
						session.persist( myEntity );
						assertThat( statistics.getEntityInsertCount() )
								.describedAs( "persist on identity column not delayed" )
								.isEqualTo( initialInsertCount );
						assertThat( myEntity.getId() ).isNull();
						session.flush();
						fail( "TransactionRequiredException required upon flush" );
					}
					catch (TransactionRequiredException ex) {
						// expected
					}
				}
		);
	}

	@Test
	public void testPersistOutsideTransactionCascadedToInverseCollection(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();

		long initialInsertCount = statistics.getEntityInsertCount();
		scope.inSession(
				session -> {
					try {
						MyEntity myEntity2 = new MyEntity( "test-persist-2" );
						MyChild child = new MyChild( "test-child-persist-inverse" );
						myEntity2.getInverseChildren().add( child );
						child.setInverseParent( myEntity2 );
						session.persist( myEntity2 );
						assertThat( statistics.getEntityInsertCount() )
								.describedAs( "persist on identity column not delayed" )
								.isEqualTo( initialInsertCount );
						assertThat( myEntity2.getId() ).isNull();
						session.flush();
						fail( "TransactionRequiredException expected upon flush." );
					}
					catch (TransactionRequiredException ex) {
						// expected
					}
				}
		);
	}

	@Test
	public void testPersistOutsideTransactionCascadedToManyToOne(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();

		long initialInsertCount = statistics.getEntityInsertCount();
		scope.inSession(
				session -> {
					try {
						MyEntity myEntity = new MyEntity( "test-persist" );
						myEntity.setSibling( new MySibling( "test-persist-sibling-out" ) );
						session.persist( myEntity );
						assertThat( statistics.getEntityInsertCount() )
								.describedAs( "persist on identity column not delayed" )
								.isEqualTo( initialInsertCount );
						assertThat( myEntity.getId() ).isNull();
						session.flush();
						fail( "TransactionRequiredException expected upon flush." );
					}
					catch (TransactionRequiredException ex) {
						// expected
					}
				}
		);
	}

	@Test
	public void testPersistOutsideTransactionCascadedFromManyToOne(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();

		long initialInsertCount = statistics.getEntityInsertCount();
		scope.inSession(
				session -> {
					try {
						MyEntity myEntity2 = new MyEntity( "test-persist-2" );
						MySibling sibling = new MySibling( "test-persist-sibling-in" );
						sibling.setEntity( myEntity2 );
						session.persist( sibling );
						assertThat( statistics.getEntityInsertCount() )
								.describedAs( "persist on identity column not delayed" )
								.isEqualTo( initialInsertCount );
						assertThat( myEntity2.getId() ).isNull();
						session.flush();
						fail( "TransactionRequiredException expected upon flush." );
					}
					catch (TransactionRequiredException ex) {
						// expected
					}
				}
		);
	}
}
