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

import static junit.framework.TestCase.fail;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-915")
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

	@org.junit.Test
	public void testWithFetchJoin() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();
		try {
			List<String> stateCodes = Arrays.asList( "DC", "CT" );
			Criteria crit = session.createCriteria( Person.class );
			crit.createCriteria( "states" ).add( Restrictions.in( "code", stateCodes ) );
			crit.setMaxResults( 10 );
			crit.list();

			transaction.commit();
		}
		catch (Exception e) {
			transaction.rollback();
			fail(e.getMessage());
		}
		finally {
			session.close();
		}
	}
}
