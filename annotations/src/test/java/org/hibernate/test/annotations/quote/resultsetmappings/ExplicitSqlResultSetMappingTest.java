/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.quote.resultsetmappings;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.test.annotations.TestCase;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class ExplicitSqlResultSetMappingTest extends TestCase {
	private String queryString = "select t.\"NAME\" as \"QuotEd_nAMe\" from \"MY_ENTITY_TABLE\" t";

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { MyEntity.class };
	}

	@Override
	protected void configure(Configuration cfg) {
		cfg.setProperty( Environment.GLOBALLY_QUOTED_IDENTIFIERS, "true" );
	}

	private void prepareTestData() {
		Session s = sfi().openSession();
		s.beginTransaction();
		s.save( new MyEntity( "mine" ) );
		s.getTransaction().commit();
		s.close();
	}

	private void cleanupTestData() {
		Session s = sfi().openSession();
		s.beginTransaction();
		s.createQuery( "delete MyEntity" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	public void testCompleteScalarAutoDiscovery() {
		prepareTestData();

		Session s = openSession();
		s.beginTransaction();
		s.createSQLQuery( queryString )
				.list();
		s.getTransaction().commit();
		s.close();

		cleanupTestData();
	}

	public void testPartialScalarAutoDiscovery() {
		prepareTestData();

		Session s = openSession();
		s.beginTransaction();
		s.createSQLQuery( queryString )
				.setResultSetMapping( "explicitScalarResultSetMapping" )
				.list();
		s.getTransaction().commit();
		s.close();

		cleanupTestData();
	}
}
