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
package org.hibernate.test.criterion;

import org.hibernate.Criteria;
import org.hibernate.IrrelevantEntity;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.loader.criteria.CriteriaQueryTranslator;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class CriterionTest extends BaseUnitTestCase {
	@Test
	public void testIlikeRendering() {
		SessionFactory sf = new Configuration()
				.addAnnotatedClass( IrrelevantEntity.class )
				.setProperty( AvailableSettings.DIALECT, IlikeSupportingDialect.class.getName() )
				.setProperty( Environment.HBM2DDL_AUTO, "create-drop" )
				.buildSessionFactory();
		try {
			final Criteria criteria = sf.openSession().createCriteria( IrrelevantEntity.class );
			final CriteriaQueryTranslator translator = new CriteriaQueryTranslator(
					(SessionFactoryImplementor) sf,
					(CriteriaImpl) criteria,
					IrrelevantEntity.class.getName(),
					"a"
			);
			final Criterion ilikeExpression = Restrictions.ilike( "name", "abc" );
			final String ilikeExpressionSqlFragment = ilikeExpression.toSqlString( criteria, translator );
			assertEquals( "a.name insensitiveLike ?", ilikeExpressionSqlFragment );
		}
		finally {
			sf.close();
		}
	}

	@Test
	public void testIlikeMimicing() {
		SessionFactory sf = new Configuration()
				.addAnnotatedClass( IrrelevantEntity.class )
				.setProperty( AvailableSettings.DIALECT, NonIlikeSupportingDialect.class.getName() )
				.setProperty( Environment.HBM2DDL_AUTO, "create-drop" )
				.buildSessionFactory();
		try {
			final Criteria criteria = sf.openSession().createCriteria( IrrelevantEntity.class );
			final CriteriaQueryTranslator translator = new CriteriaQueryTranslator(
					(SessionFactoryImplementor) sf,
					(CriteriaImpl) criteria,
					IrrelevantEntity.class.getName(),
					"a"
			);
			final Criterion ilikeExpression = Restrictions.ilike( "name", "abc" );
			final String ilikeExpressionSqlFragment = ilikeExpression.toSqlString( criteria, translator );
			assertEquals( "lowLowLow(a.name) like ?", ilikeExpressionSqlFragment );
		}
		finally {
			sf.close();
		}
	}

	public static class IlikeSupportingDialect extends Dialect {
		@Override
		public boolean supportsCaseInsensitiveLike() {
			return true;
		}

		@Override
		public String getCaseInsensitiveLike() {
			return "insensitiveLike";
		}
	}

	public static class NonIlikeSupportingDialect extends Dialect {
		@Override
		public boolean supportsCaseInsensitiveLike() {
			return false;
		}

		@Override
		public String getCaseInsensitiveLike() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getLowercaseFunction() {
			return "lowLowLow";
		}
	}
}
