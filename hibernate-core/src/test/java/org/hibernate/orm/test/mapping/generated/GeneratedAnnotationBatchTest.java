/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import java.time.Instant;
import java.util.List;

import org.hibernate.HibernateError;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.generator.EventType.UPDATE;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel(annotatedClasses = GeneratedAnnotationBatchTest.GeneratedEntity.class)
@ServiceRegistry(settings = @Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "3"))
public class GeneratedAnnotationBatchTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// generate more entities than batch_size to trigger both
			// implicit and explicit batch execution
			session.persist( new GeneratedEntity( "new_1" ) );
			session.persist( new GeneratedEntity( "new_2" ) );
			session.persist( new GeneratedEntity( "new_3" ) );
			session.persist( new GeneratedEntity( "new_4" ) );
			session.persist( new GeneratedEntity( "new_5" ) );
		} );
	}

	@AfterAll
	public void tearDOwn(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from GeneratedEntity" ).executeUpdate() );
	}

	@Test
	public void testInsert(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Integer> resultList = session.createQuery(
							"select generatedProp from GeneratedEntity",
							Integer.class
					)
					.getResultList();
			assertThat( resultList ).hasSize( 5 );
			resultList.forEach( value -> assertThat( value ).isEqualTo( 1 ) );
		} );
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		final Instant originalInstant = scope.fromTransaction( session -> session.createQuery(
				"from GeneratedEntity where id = 1L",
				GeneratedEntity.class
		).getSingleResult().getUpdateTimestamp() );
		scope.inTransaction( session -> {
			final List<GeneratedEntity> entities = session.createQuery(
					"from GeneratedEntity",
					GeneratedEntity.class
			).getResultList();
			entities.forEach( ge -> ge.setName( "updated" ) );

			//We need to wait a little to make sure the timestamps produced are different
			waitALittle();
			session.flush(); // force update and retrieval of generated values

			entities.forEach( ge -> assertThat( ge.getName() ).isEqualTo( "updated" ) );
			entities.forEach( ge -> assertThat( ge.getUpdateTimestamp() ).isAfter( originalInstant ) );
		} );
	}

	@Entity(name = "GeneratedEntity")
	public static class GeneratedEntity {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@Generated(event = INSERT)
		@ColumnDefault("1")
		private Integer generatedProp;

		@CurrentTimestamp(event = { INSERT, UPDATE })
		private Instant updateTimestamp;

		public GeneratedEntity() {
		}

		public GeneratedEntity(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Integer getGeneratedProp() {
			return generatedProp;
		}

		public Instant getUpdateTimestamp() {
			return updateTimestamp;
		}
	}

	private static void waitALittle() {
		try {
			Thread.sleep( 10 );
		}
		catch (InterruptedException e) {
			throw new HibernateError( "Unexpected wakeup from test sleep" );
		}
	}
}
