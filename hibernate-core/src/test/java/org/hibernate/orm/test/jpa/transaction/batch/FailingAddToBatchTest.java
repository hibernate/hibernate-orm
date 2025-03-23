/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.transaction.batch;

import org.hibernate.cfg.BatchSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.NotImplementedYet;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceException;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey(value = "HHH-15082")
@Jpa(
		annotatedClasses = {
				FailingAddToBatchTest.MyEntity.class
		},
		integrationSettings = {
				@Setting(name = BatchSettings.STATEMENT_BATCH_SIZE, value = "50")
		},
		settingProviders = {
				@SettingProvider(
						settingName = BatchSettings.BUILDER,
						provider = AbstractBatchingTest.ErrorBatch2BuilderSettingProvider.class
				)
		}
)
public class FailingAddToBatchTest extends AbstractBatchingTest {

	@Test
	public void testInsert(EntityManagerFactoryScope scope) {
		try {
			scope.inTransaction( em -> {
				final MyEntity entity = new MyEntity();
				entity.setText( "initial" );
				em.persist( entity );
			} );
		}
		catch (PersistenceException e) {
			assertThat( batchWrapper.getStatementGroup().getNumberOfActiveStatements() ).isEqualTo( 0 );
			assertThat( batchWrapper.getNumberOfBatches() ).isEqualTo( 1 );
			assertThat( batchWrapper.getNumberOfSuccessfulBatches() ).isEqualTo( 0 );
		}
	}

	@Test
	@NotImplementedYet( reason = "Still need to work on entity update executors" )
	public void testUpdate(EntityManagerFactoryScope scope) {
		throw new RuntimeException();
//		final Long id = scope.fromTransaction( (em) -> {
//			MyEntity entity = new MyEntity();
//			entity.setText( "initial" );
//			em.persist( entity );
//			return entity.getId();
//		} );
//
//		scope.inEntityManager( (em) -> {
//			final MyEntity managed = scope.fromTransaction( em, (e) -> em.find( MyEntity.class, id ) );
//
//			scope.inTransaction( em, (e) -> {
//				managed.setText( "updated" );
//				assertThat( batchWrapper.getStatementGroup().getNumberOfStatements() ).isEqualTo( 1 );
//				assertThat( batchWrapper.getNumberOfBatches() ).isEqualTo( 1 );
//				assertThat( batchWrapper.getNumberOfSuccessfulBatches() ).isEqualTo( 0 );
//			} );
//		} );
	}

	@Test
	@NotImplementedYet( reason = "Still need to work on entity delete executors" )
	public void testRemove(EntityManagerFactoryScope scope) {
		throw new RuntimeException();
//		Long id = scope.fromTransaction( em -> {
//			MyEntity entity = new MyEntity();
//			entity.setText( "initial" );
//			em.persist( entity );
//			return entity.getId();
//		} );
//
//		RuntimeException simulatedAddToBatchFailure = new RuntimeException( "Simulated RuntimeException" );
//
//		scope.inTransaction( em -> {
//			assertThatThrownBy( () -> {
//				MyEntity entity = em.find( MyEntity.class, id );
////				TestBatch.nextAddToBatchFailure.set( simulatedAddToBatchFailure );
//				em.remove( entity );
//				em.flush();
//			} )
//					.isSameAs( simulatedAddToBatchFailure );
//
////			assertAllStatementsAreClosed( testBatch.createdStatements );
//		} );
	}

	@Entity(name = "MyEntity")
	public static class MyEntity {
		@Id
		@GeneratedValue
		private Long id;
		private String text;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

}
