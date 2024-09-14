/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.Session;

import org.hibernate.orm.test.jpa.model.AbstractJPATest;
import org.junit.jupiter.api.Test;

/**
 * Test use of the JPA 2.1 FUNCTION keyword.
 *
 * @author Steve Ebersole
 */
public class FunctionKeywordTest extends AbstractJPATest {

	@Test
	public void basicFixture() {
		try (Session session = sessionFactoryScope().getSessionFactory().openSession()) {
			session.createQuery( "select i from Item i where substring( i.name, 1, 3 ) = 'abc'" )
					.list();
		}
	}

	@Test
	public void basicTest() {
		try (Session session = sessionFactoryScope().getSessionFactory().openSession()) {
			session.createQuery( "select i from Item i where function( 'substring', i.name, 1, 3 ) = 'abc'" )
					.list();
		}
	}
}
