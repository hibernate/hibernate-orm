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
package org.hibernate.test.hql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Filter;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-7757" )
public class TupleSupportTest extends BaseUnitTestCase {
	@Entity( name = "TheEntity" )
	public static class TheEntity {
		@Id
		private Long id;
		@Embedded
		private TheComposite compositeValue;
	}

	@Embeddable
	public static class TheComposite {
		private String thing1;
		private String thing2;

		public TheComposite() {
		}

		public TheComposite(String thing1, String thing2) {
			this.thing1 = thing1;
			this.thing2 = thing2;
		}
	}

	private SessionFactory sessionFactory;

	@Before
	public void buildSessionFactory() {
		Configuration cfg = new Configuration()
				.addAnnotatedClass( TheEntity.class );
		cfg.getProperties().put( AvailableSettings.DIALECT, NoTupleSupportDialect.class.getName() );
		cfg.getProperties().put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
		sessionFactory = cfg.buildSessionFactory();
	}

	@After
	public void releaseSessionFactory() {
		sessionFactory.close();
	}

	@Test
	public void testImplicitTupleNotEquals() {
		final String hql = "from TheEntity e where e.compositeValue <> :p1";
		HQLQueryPlan queryPlan = ( (SessionFactoryImplementor) sessionFactory ).getQueryPlanCache()
				.getHQLQueryPlan( hql, false, Collections.<String,Filter>emptyMap() );

		assertEquals( 1, queryPlan.getSqlStrings().length );
		System.out.println( " SQL : " + queryPlan.getSqlStrings()[0] );
		assertTrue( queryPlan.getSqlStrings()[0].contains( "<>" ) );
	}

	@Test
	public void testImplicitTupleNotInList() {
		final String hql = "from TheEntity e where e.compositeValue not in (:p1,:p2)";
		HQLQueryPlan queryPlan = ( (SessionFactoryImplementor) sessionFactory ).getQueryPlanCache()
				.getHQLQueryPlan( hql, false, Collections.<String,Filter>emptyMap() );

		assertEquals( 1, queryPlan.getSqlStrings().length );
		System.out.println( " SQL : " + queryPlan.getSqlStrings()[0] );
		assertTrue( queryPlan.getSqlStrings()[0].contains( "<>" ) );
	}

	public static class NoTupleSupportDialect extends H2Dialect {
		@Override
		public boolean supportsRowValueConstructorSyntax() {
			return false;
		}

		@Override
		public boolean supportsRowValueConstructorSyntaxInInList() {
			return false;
		}
	}
}
