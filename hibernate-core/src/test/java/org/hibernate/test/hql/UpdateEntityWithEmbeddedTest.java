/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-14251")
public class UpdateEntityWithEmbeddedTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Company.class };
	}

	@Before
	public void setUp() {
		inTransaction(
				session -> {
					Logo logo = new Logo( "logo1", "png" );
					Company company = new Company( 1l, logo );
					session.save( company );
				}
		);
	}

	@After
	public void tearDown() {
		inTransaction(
				session -> {
					session.createQuery( "delete from Company" ).executeUpdate();
				}
		);
	}

	@Test
	public void testUpdate() {
		inTransaction(
				session -> {
					Logo logo = new Logo( "logo2", "png" );
					session.createQuery( "UPDATE Company c SET c.logo = :logo" )
							.setParameter( "logo", logo )
							.executeUpdate();
				}
		);
	}

	@Test
	public void testUpdate2() {
		inTransaction(
				session -> {
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
