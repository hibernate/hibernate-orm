/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql.mutation;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-14251")
@DomainModel( annotatedClasses = UpdateEntityWithEmbeddedTest.Company.class )
@SessionFactory
public class UpdateEntityWithEmbeddedTest {
	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Logo logo = new Logo( "logo1", "png" );
					Company company = new Company( 1l, logo );
					session.persist( company );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Logo logo = new Logo( "logo2", "png" );
					session.createQuery( "UPDATE Company c SET c.logo = :logo" )
							.setParameter( "logo", logo )
							.executeUpdate();
				}
		);
	}

	@Test
	public void testUpdate2(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					session.createQuery(
							"UPDATE Company c SET c.logo.fileName = :filename, c.logo.fileExtension = :fileExtension" )
							.setParameter( "filename", "logo2" )
							.setParameter( "fileExtension", "png" )
							.executeUpdate();
				}
		);
	}

	@Entity(name = "Company")
	public static class Company {
		@Id
		private Long id;

		@Embedded
		private Logo logo;

		public Company() {
		}

		public Company(Long id, Logo logo) {
			this.id = id;
			this.logo = logo;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Logo getLogo() {
			return logo;
		}

		public void setLogo(Logo logo) {
			this.logo = logo;
		}
	}

	@Embeddable
	public static class Logo {
		String fileName;

		String fileExtension;

		public Logo() {
		}

		public Logo(String fileName, String fileExtension) {
			this.fileName = fileName;
			this.fileExtension = fileExtension;
		}

		public String getFileName() {
			return fileName;
		}

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		public String getFileExtension() {
			return fileExtension;
		}

		public void setFileExtension(String fileExtension) {
			this.fileExtension = fileExtension;
		}
	}
}
