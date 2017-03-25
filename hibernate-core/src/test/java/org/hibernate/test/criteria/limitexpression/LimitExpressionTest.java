/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.criteria.limitexpression;

import java.util.Arrays;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

/**
 * @author Andrea Boriero
 */

@RequiresDialectFeature(
		value = DialectChecks.SupportLimitCheck.class,
		comment = "Dialect does not support limit"
)
public class LimitExpressionTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	public String[] getMappings() {
		return new String[] {"criteria/limitexpression/domain.hbm.xml"};
	}

	@Override
	public String getCacheConcurrencyStrategy() {
		return null;
	}

	@Test
	@TestForIssue(jiraKey = "HHH-915")
	public void testWithFetchJoin() {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			final List<String> stateCodes = Arrays.asList( "DC", "CT" );
			final Criteria crit = session.createCriteria( Person.class );
			crit.createCriteria( "states" ).add( Restrictions.in( "code", stateCodes ) );
			crit.setMaxResults( 10 );
			crit.list();
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11278")
	public void testAnEmptyListIsReturnedWhenSetMaxResultsToZero() {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			final Criteria crit = session.createCriteria( Person.class );
			crit.setMaxResults( 0 );
			final List list = crit.list();
			assertTrue( "The list should be empty with setMaxResults 0", list.isEmpty() );
		} );
	}

	@Override
	protected void prepareTest() throws Exception {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			Person p = new Person();
			session.save( p );
		});
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}
}
