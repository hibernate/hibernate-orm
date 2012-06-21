/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.jpa.ql;

import org.hibernate.Session;

import org.junit.Test;

import org.hibernate.test.jpa.AbstractJPATest;

/**
 * Test use of the JPA 2.1 FUNCTION keyword.
 *
 * @author Steve Ebersole
 */
public class FunctionKeywordTest extends AbstractJPATest {

	@Test
	public void basicFixture() {
		Session s = openSession();
		s.createQuery( "select i from Item i where substring( i.name, 1, 3 ) = 'abc'" )
				.list();
		s.close();
	}

	@Test
	public void basicTest() {
		Session s = openSession();
		s.createQuery( "select i from Item i where function( 'substring', i.name, 1, 3 ) = 'abc'" )
				.list();
		s.close();
	}
}
