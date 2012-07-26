/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.hql;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.hql.internal.classic.ClassicQueryTranslatorFactory;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;

/**
 * Some simple test queries using the classic translator explicitly
 * to ensure that code is not broken in changes for the new translator.
 * <p/>
 * Only really checking translation and syntax, not results.
 *
 * @author Steve Ebersole
 */
@FailureExpectedWithNewMetamodel
public class ClassicTranslatorTest extends QueryTranslatorTestCase {
	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.QUERY_TRANSLATOR, ClassicQueryTranslatorFactory.class.getName() );
	}

	@Override
	public boolean createSchema() {
		return true;
	}

	@Override
	public boolean rebuildSessionFactoryOnError() {
		return true;
	}

	@Test
	public void testQueries() {
		Session session = openSession();
		session.beginTransaction();

		session.createQuery( "from Animal" ).list();

		session.createQuery( "select a from Animal as a" ).list();
		session.createQuery( "select a.mother from Animal as a" ).list();
		session.createQuery( "select m from Animal as a inner join a.mother as m" ).list();
		session.createQuery( "select a from Animal as a inner join fetch a.mother" ).list();

		session.createQuery( "from Animal as a where a.description = ?" ).setString( 0, "jj" ).list();
		session.createQuery( "from Animal as a where a.description = :desc" ).setString( "desc", "jr" ).list();
		session.createQuery( "from Animal as a where a.description = ? or a.description = :desc" )
				.setString( 0, "jj" )
				.setString( "desc", "jr" )
				.list();

		session.getTransaction().commit();
		session.close();
	}
}
