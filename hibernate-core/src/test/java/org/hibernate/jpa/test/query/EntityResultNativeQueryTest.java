/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.query;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Query;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class EntityResultNativeQueryTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SimpleEntity.class };
	}

	@Before
	public void setUp() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.persist( new SimpleEntity( "Hibernate" ) );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12776")
	public void testNativeQueryResultHandling() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Query query = entityManager.createNativeQuery( "select * from SimpleEntity", SimpleEntity.class );
			SimpleEntity result = (SimpleEntity) query.getSingleResult();
			assertThat( result.getStringField(), is( "Hibernate" ) );
		} );
	}

	@Entity(name = "SimpleEntity")
	public static class SimpleEntity {
		@Id
		@GeneratedValue
		private Long id;

		private String stringField;

		public SimpleEntity() {
		}

		public SimpleEntity(String stringField) {
			this.stringField = stringField;
		}

		public Long getId() {
			return id;
		}

		public String getStringField() {
			return stringField;
		}

		public void setStringField(String stringField) {
			this.stringField = stringField;
		}
	}
}

