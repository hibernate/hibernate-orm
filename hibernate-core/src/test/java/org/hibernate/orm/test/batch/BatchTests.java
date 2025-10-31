/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.testing.orm.domain.userguide.Account;
import org.hibernate.testing.orm.domain.userguide.Call;
import org.hibernate.testing.orm.domain.userguide.Partner;
import org.hibernate.testing.orm.domain.userguide.Payment;
import org.hibernate.testing.orm.domain.userguide.Person;
import org.hibernate.testing.orm.domain.userguide.Phone;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = {
				Person.class,
				Phone.class,
				Call.class,
				Account.class,
				Payment.class,
				Partner.class
		}
)
public class BatchTests {

	@Test
	public void testScroll(EntityManagerFactoryScope scope) {
		withScroll( scope );
	}

	@Test
	public void testStatelessSession(EntityManagerFactoryScope scope) {
		withStatelessSession( scope );
	}

	@Test
	public void testBulk(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.persist( new Person( "Vlad" ) );
			entityManager.persist( new Person( "Mihalcea" ) );
		} );
		scope.inTransaction( entityManager -> {
			String oldName = "Vlad";
			String newName = "Alexandru";
			//tag::batch-session-jdbc-batch-size-example[]
			entityManager
					.unwrap( Session.class )
					.setJdbcBatchSize( 10 );
			//end::batch-session-jdbc-batch-size-example[]
		} );
		scope.inTransaction( entityManager -> {
			String oldName = "Vlad";
			String newName = "Alexandru";
			//tag::batch-bulk-jpql-update-example[]
			int updatedEntities = entityManager.createQuery(
							"update Person p " +
							"set p.name = :newName " +
							"where p.name = :oldName" )
					.setParameter( "oldName", oldName )
					.setParameter( "newName", newName )
					.executeUpdate();
			//end::batch-bulk-jpql-update-example[]
			assertThat( updatedEntities ).isEqualTo( 1 );
		} );

		scope.inTransaction( entityManager -> {
			String oldName = "Alexandru";
			String newName = "Vlad";

			Session session = entityManager.unwrap( Session.class );
			//tag::batch-bulk-hql-update-example[]
			int updatedEntities = session.createMutationQuery(
							"update Person " +
							"set name = :newName " +
							"where name = :oldName" )
					.setParameter( "oldName", oldName )
					.setParameter( "newName", newName )
					.executeUpdate();
			//end::batch-bulk-hql-update-example[]
			assertThat( updatedEntities ).isEqualTo( 1 );
		} );

		scope.inTransaction( entityManager -> {
			String oldName = "Vlad";
			String newName = "Alexandru";

			Session session = entityManager.unwrap( Session.class );
			//tag::batch-bulk-hql-update-version-example[]
			int updatedEntities = session.createMutationQuery(
							"update versioned Person " +
							"set name = :newName " +
							"where name = :oldName" )
					.setParameter( "oldName", oldName )
					.setParameter( "newName", newName )
					.executeUpdate();
			//end::batch-bulk-hql-update-version-example[]
			assertThat( updatedEntities ).isEqualTo( 1 );
		} );

		scope.inTransaction( entityManager -> {
			String name = "Alexandru";

			//tag::batch-bulk-jpql-delete-example[]
			int deletedEntities = entityManager.createQuery(
							"delete Person p " +
							"where p.name = :name" )
					.setParameter( "name", name )
					.executeUpdate();
			//end::batch-bulk-jpql-delete-example[]
			assertThat( deletedEntities ).isEqualTo( 1 );
		} );

		scope.inTransaction( entityManager -> {

			Session session = entityManager.unwrap( Session.class );
			//tag::batch-bulk-hql-insert-example[]
			int insertedEntities = session.createMutationQuery(
							"insert into Partner (id, name) " +
							"select p.id, p.name " +
							"from Person p " )
					.executeUpdate();
			//end::batch-bulk-hql-insert-example[]
			assertThat( insertedEntities ).isEqualTo( 1 );
		} );

		scope.inTransaction( entityManager -> {
			String name = "Mihalcea";

			Session session = entityManager.unwrap( Session.class );
			//tag::batch-bulk-hql-delete-example[]
			int deletedEntities = session.createMutationQuery(
							"delete Person " +
							"where name = :name" )
					.setParameter( "name", name )
					.executeUpdate();
			//end::batch-bulk-hql-delete-example[]
			assertThat( deletedEntities ).isEqualTo( 1 );
		} );
	}

	private void withoutBatch(EntityManagerFactoryScope scope) {
		//tag::batch-session-batch-example[]
		scope.inTransaction(
				entityManager -> {
					for ( int i = 0; i < 100_000; i++ ) {
						Person Person = new Person( String.format( "Person %d", i ) );
						entityManager.persist( Person );
					}
				}
		);
		//end::batch-session-batch-example[]
	}

	private void withBatch(EntityManagerFactoryScope scope) {
		int entityCount = 100;
		int batchSize = 25;
		//tag::batch-session-batch-insert-example[]
		scope.inTransaction(
				entityManager -> {
					for ( int i = 0; i < entityCount; i++ ) {
						if ( i > 0 && i % batchSize == 0 ) {
							//flush a batch of inserts and release memory
							entityManager.flush();
							entityManager.clear();
						}

						Person Person = new Person( String.format( "Person %d", i ) );
						entityManager.persist( Person );
					}
				}
		);
		//end::batch-session-batch-insert-example[]
	}

	private void withScroll(EntityManagerFactoryScope scope) {
		withBatch( scope );
		int batchSize = 25;

		//tag::batch-session-scroll-example[]
		scope.inTransaction(
				entityManager -> {
					ScrollableResults<Person> scrollableResults = null;
					try {
						scrollableResults = entityManager.unwrap( Session.class )
								.createSelectionQuery( "select p from Person p", Person.class )
								.setCacheMode( CacheMode.IGNORE )
								.scroll( ScrollMode.FORWARD_ONLY );

						int count = 0;
						while ( scrollableResults.next() ) {
							Person Person = (Person) scrollableResults.get();
							processPerson( Person );
							if ( ++count % batchSize == 0 ) {
								//flush a batch of updates and release memory:
								entityManager.flush();
								entityManager.clear();
							}
						}
					}
					finally {
						if ( scrollableResults != null ) {
							scrollableResults.close();
						}
					}
				}
		);
		//end::batch-session-scroll-example[]
	}

	private void withStatelessSession(EntityManagerFactoryScope scope) {
		withBatch( scope );
		SessionFactory sessionFactory = scope.getEntityManagerFactory().unwrap( SessionFactory.class );
		//tag::batch-stateless-session-example[]
		StatelessSession statelessSession = null;
		ScrollableResults<?> scrollableResults = null;
		try {
			statelessSession = sessionFactory.openStatelessSession();
			statelessSession.beginTransaction();
			scrollableResults =
					statelessSession.createSelectionQuery( "select p from Person p", Person.class )
							.scroll( ScrollMode.FORWARD_ONLY );

			while ( scrollableResults.next() ) {
				Person Person = (Person) scrollableResults.get();
				processPerson( Person );
				statelessSession.update( Person );
			}
			statelessSession.getTransaction().commit();
		}
		finally {
			try {
				if ( scrollableResults != null ) {
					scrollableResults.close();
				}

				if ( statelessSession != null ) {
					if ( statelessSession.getTransaction().isActive() ) {
						statelessSession.getTransaction().rollback();
					}
				}
			}
			finally {
				if ( statelessSession != null ) {
					statelessSession.close();
				}
			}
		}
		//end::batch-stateless-session-example[]
	}

	private void processPerson(Person Person) {
		if ( Person.getId() % 1000 == 0 ) {
			Person.getName();
		}
	}

}
