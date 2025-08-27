/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.joinfetch;

import org.hibernate.Hibernate;
import org.hibernate.query.SemanticException;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		JoinFetchImplicitOwnerTest.ImageLog.class,
		JoinFetchImplicitOwnerTest.LateralEntity.class,
		JoinFetchImplicitOwnerTest.Project.class,
		JoinFetchImplicitOwnerTest.Company.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16362" )
public class JoinFetchImplicitOwnerTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Company company = new Company( 1L, "test_company" );
			session.persist( company );
			final Project project = new Project( 2L, company );
			session.persist( project );
			final LateralEntity lateralEntity = new LateralEntity( 3L, project );
			session.persist( lateralEntity );
			session.persist( new ImageLog( 4L, lateralEntity ) );
		} );
	}

	@Test
	public void testExplicitJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final LateralEntity result = session.createQuery(
					"select le from LateralEntity le " +
					"join fetch le.project p " +
					"join fetch p.company",
					LateralEntity.class
			).getSingleResult();
			assertThat( Hibernate.isInitialized( result.getProject().getCompany() ) ).isTrue();
			assertThat( result.getProject().getCompany().getName() ).isEqualTo( "test_company" );
		} );
	}

	@Test
	public void testImplicitJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Project result = session.createQuery(
					"select le.project from LateralEntity le " +
					"join fetch le.project.company",
					Project.class
			).getSingleResult();
			assertThat( Hibernate.isInitialized( result.getCompany() ) ).isTrue();
			assertThat( result.getCompany().getName() ).isEqualTo( "test_company" );
		} );
	}

	@Test
	public void testImplicitNestedJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Project result = session.createQuery(
					"select ilog.lateralEntity.project from ImageLog ilog " +
					"join fetch ilog.lateralEntity.project.company",
					Project.class
			).getSingleResult();
			assertThat( Hibernate.isInitialized( result.getCompany() ) ).isTrue();
			assertThat( result.getCompany().getName() ).isEqualTo( "test_company" );
		} );
	}

	@Test
	public void testInvalidExplicitJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThatThrownBy( () -> session.createQuery(
				"select p from LateralEntity le "
				+ "join fetch le.project p "
				+ "join fetch p.company",
				Project.class
		) ).hasCauseInstanceOf( SemanticException.class )
				.hasMessageContaining("the owner of the fetched association was not present in the select list") );
	}

	@Test
	public void testInvalidImplicitJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThatThrownBy( () -> session.createQuery(
				"select le from LateralEntity le " +
				"join fetch le.project.company",
				Project.class
		) ).hasCauseInstanceOf( SemanticException.class )
				.hasMessageContaining("the owner of the fetched association was not present in the select list") );
	}

	@Test
	public void testInvalidImplicitNestedJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThatThrownBy( () -> session.createQuery(
				"select ilog.lateralEntity from ImageLog ilog " +
				"join fetch ilog.lateralEntity.project.company",
				Project.class
		) ).hasCauseInstanceOf( SemanticException.class )
				.hasMessageContaining("the owner of the fetched association was not present in the select list") );
	}

	@Entity( name = "ImageLog" )
	@Table( name = "image_logs" )
	public static class ImageLog {
		@Id
		public Long id;
		@ManyToOne( fetch = FetchType.LAZY )
		@JoinColumn( name = "lateral_id" )
		public LateralEntity lateralEntity;

		public ImageLog() {
		}

		public ImageLog(Long id, LateralEntity lateralEntity) {
			this.id = id;
			this.lateralEntity = lateralEntity;
		}

		public Long getId() {
			return id;
		}

		public LateralEntity getLateralEntity() {
			return lateralEntity;
		}
	}

	@Entity( name = "LateralEntity" )
	@Table( name = "laterals" )
	public static class LateralEntity {
		@Id
		public Long id;
		@ManyToOne( fetch = FetchType.LAZY )
		@JoinColumn( name = "project_id" )
		public Project project;

		public LateralEntity() {
		}

		public LateralEntity(Long id, Project project) {
			this.id = id;
			this.project = project;
		}

		public Long getId() {
			return id;
		}

		public Project getProject() {
			return project;
		}
	}

	@Entity( name = "Project" )
	@Table( name = "projects" )
	public static class Project {
		@Id
		public Long id;
		@ManyToOne( fetch = FetchType.LAZY )
		@JoinColumn( name = "company_id" )
		public Company company;

		public Project() {
		}

		public Project(Long id, Company company) {
			this.id = id;
			this.company = company;
		}

		public Long getId() {
			return id;
		}

		public Company getCompany() {
			return company;
		}
	}

	@Entity
	@Table( name = "companies" )
	public static class Company {
		@Id
		private long id;
		private String name;

		public Company() {
		}

		public Company(long id, String name) {
			this.id = id;
			this.name = name;
		}

		public long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
