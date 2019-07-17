/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.pagination;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.Session;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static java.lang.String.format;
import static org.junit.Assert.fail;

public class LimitWithExpreesionAndFetchJoinTest extends BaseNonConfigCoreFunctionalTestCase {
	/**
	 * @author Piotr Findeisen <piotr.findeisen@gmail.com>
	 */
	@Test
	@TestForIssue( jiraKey = "HHH-951" )
	@RequiresDialectFeature(
			value = DialectChecks.SupportLimitCheck.class,
			comment = "Dialect does not support limit"
	)
	public void testLimitWithExpreesionAndFetchJoin() {
		Session session = openSession();
		session.beginTransaction();

		String hql = "SELECT b, 1 FROM DataMetaPoint b inner join fetch b.dataPoint dp";
		session.createQuery(hql)
				.setMaxResults(3)
				// This should not fail
				.list();

		HQLQueryPlan queryPlan = new HQLQueryPlan( hql, false, Collections.EMPTY_MAP, sessionFactory());
		String sqlQuery = queryPlan.getTranslators()[0]
				.collectSqlStrings().get(0);

		session.getTransaction().commit();
		session.close();

		Matcher matcher = Pattern.compile(
				"(?is)\\b(?<column>\\w+\\.\\w+)\\s+as\\s+(?<alias>\\w+)\\b.*\\k<column>\\s+as\\s+\\k<alias>")
				.matcher(sqlQuery);
		if (matcher.find()) {
			fail(format("Column %s mapped to alias %s twice in generated SQL: %s", matcher.group("column"),
						matcher.group("alias"), sqlQuery));
		}
	}
}
