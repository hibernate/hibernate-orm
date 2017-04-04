/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.replicate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * Test trying to replicate HHH-11514
 *
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-11514")
@RequiresDialectFeature(DialectChecks.SupportsIdentityColumns.class)
@FailureExpected( jiraKey = "HHH-11514" )
public class ReplicateTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				City.class,
		};
	}

	@Test
	public void refreshTest() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			City city = new City();
			city.setId( 100L );
			city.setName( "Cluj-Napoca" );
			entityManager.unwrap(Session.class).replicate( city, ReplicationMode.OVERWRITE );
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			City city = entityManager.find( City.class, 100L);
			assertEquals("Cluj-Napoca", city.getName());
		} );
	}

	@Entity(name = "City" )
	public static class City {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
