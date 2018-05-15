/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.Session;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
@TestForIssue( jiraKey = "HHH-11397")
public class QueryParametersValidationTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {TestEntity.class};
	}

	@Test
	public void setParameterWithWrongTypeShouldNotThrowIllegalArgumentException() {
		try (Session session = openSession()) {
			session.createQuery( "select e from TestEntity e where e.id = :id" ).setParameter( "id", 1 );
		}
	}

	@Entity(name = "TestEntity")
	public class TestEntity {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;
	}
}
