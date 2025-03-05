/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Note: to reproduce the bug use a Dialect with {@link org.hibernate.query.sqm.mutation.internal.cte.CteMutationStrategy CTE}
 * multi table mutation strategy support (e.g. {@link org.hibernate.dialect.PostgreSQLDialect#getFallbackSqmMutationStrategy PostgreSQLDialect})
 *
 * @author Marco Belladelli
 */
@Jpa( annotatedClasses = {
		CriteriaBulkUpdateQuotedInheritanceTest.UserEntity.class,
		CriteriaBulkUpdateQuotedInheritanceTest.PatientEntity.class,
}, integrationSettings = {
		@Setting( name = AvailableSettings.DEFAULT_SCHEMA, value = "public" ),
		@Setting( name = AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, value = "true" ),
} )
@RequiresDialect( PostgreSQLDialect.class )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16792" )
public class CriteriaBulkUpdateQuotedInheritanceTest {
	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.persist( new PatientEntity( 1L, "patient1", 1 ) );
			entityManager.persist( new PatientEntity( 2L, "patient2", 2 ) );
		} );
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> em.createQuery( "delete from PatientEntity" ).executeUpdate() );
	}

	@Test
	public void testBulkUpdate(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			final CriteriaUpdate<PatientEntity> update = criteriaBuilder.createCriteriaUpdate( PatientEntity.class );
			final Root<PatientEntity> entityRoot = update.from( PatientEntity.class );
			update.set( entityRoot.get( "weight" ), 100 );
			update.where( criteriaBuilder.equal( entityRoot.get( "name" ), "patient1" ) );
			final int result = entityManager.createQuery( update ).executeUpdate();
			assertThat( result ).isEqualTo( 1 );
		} );
	}

	@Test
	public void testBulkDelete(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			final CriteriaDelete<PatientEntity> delete = criteriaBuilder.createCriteriaDelete( PatientEntity.class );
			final Root<PatientEntity> entityRoot = delete.from( PatientEntity.class );
			delete.where( criteriaBuilder.equal( entityRoot.get( "name" ), "patient2" ) );
			final int result = entityManager.createQuery( delete ).executeUpdate();
			assertThat( result ).isEqualTo( 1 );
		} );
	}

	@Entity( name = "UserEntity" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public static abstract class UserEntity {
		@Id
		private Long id;

		@Column
		private String name;

		public UserEntity() {
		}

		public UserEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity( name = "PatientEntity" )
	public static class PatientEntity extends UserEntity {
		private Integer weight;

		public PatientEntity() {
		}

		public PatientEntity(Long id, String name, Integer weight) {
			super( id, name );
			this.weight = weight;
		}
	}
}
