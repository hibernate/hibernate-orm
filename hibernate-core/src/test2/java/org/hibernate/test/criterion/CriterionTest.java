/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

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
