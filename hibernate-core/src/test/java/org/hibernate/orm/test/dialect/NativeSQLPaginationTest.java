/*
 *
 *  * Hibernate, Relational Persistence for Idiomatic Java
 *  *
 *  * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 *  * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 *
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.dialect;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.cfg.Environment;
import org.hibernate.orm.test.sql.hand.Person;
import org.hibernate.query.spi.QueryImplementor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests of pagination features of native SQL queries.
 *
 * @author Francois Vanhille
 */
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.GENERATE_STATISTICS, value = "true")
		}
)
@DomainModel(
		xmlMappings = { "org/hibernate/orm/test/dialect/NativeSQLQueries.hbm.xml" }
)
@SessionFactory
public class NativeSQLPaginationTest {

	@Test
	@TestForIssue(jiraKey = "HHH-17700")
	@SuppressWarnings("rawtypes")
	public void testPaginatedNativeQueries(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					savePersons( 12, session );
					session.flush();
					for ( String queryName : new String[] {
							"getPersonNames",
							"getPersonNames2",
							"getPersonNames3",
							"getPersonNames4",
							"getPersonNames5",
							"getPersonNames6"
					} ) {
						QueryImplementor query = session.getNamedQuery( queryName );
						query.setFirstResult( 1 );
						query.setMaxResults( 5 );
						List result = query.getResultList();
						assertEquals( 5, result.size(), "expecting 5 result values" );
						String firstName = (String) result.get( 0 );
						assertEquals( "p10", firstName );
						String lastName = (String) result.get( 4 );
						assertEquals( "p3", lastName );
					}
				}
		);
	}

	private void savePersons(int n, Session session) {
		for ( int i = 1; i <= n; i++ ) {
			Person person = new Person( "p" + i );
			session.persist( person );
		}
	}
}
