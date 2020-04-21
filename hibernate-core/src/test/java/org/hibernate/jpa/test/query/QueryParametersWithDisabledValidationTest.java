/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.query;

import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11579")
public class QueryParametersWithDisabledValidationTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {QueryParametersValidationTest.TestEntity.class};
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.VALIDATE_QUERY_PARAMETERS, false );
	}

	@Test
	public void setParameterWithWrongTypeShouldNotThrowIllegalArgumentException() {
		final EntityManager entityManager = entityManagerFactory().createEntityManager();
		try {
			entityManager.createQuery( "select e from TestEntity e where e.id = :id" ).setParameter( "id", 1 );
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	public void setParameterWithCorrectTypeShouldNotThrowIllegalArgumentException() {
		final EntityManager entityManager = entityManagerFactory().createEntityManager();
		try {
			entityManager.createQuery( "select e from TestEntity e where e.id = :id" ).setParameter( "id", 1L );
		}
		finally {
			entityManager.close();
		}
	}

	@Entity(name = "TestEntity")
	public class TestEntity {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;
	}
}