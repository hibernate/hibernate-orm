/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.ops;

import org.hibernate.dialect.Oracle9iDialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = { Oracle9iDialect.class })
@TestForIssue( jiraKey = "HHH-13104" )
public class OracleNoColumnInsertTest extends BaseCoreFunctionalTestCase {

	public String[] getMappings() {
		return new String[] {
			"ops/Competition.hbm.xml"
		};
	}

	@Test
	public void test() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			Competition competition = new Competition();

			session.persist( competition );
		} );
	}
}

