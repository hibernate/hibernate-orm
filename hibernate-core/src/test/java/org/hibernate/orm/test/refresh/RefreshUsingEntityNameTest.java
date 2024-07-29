/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.refresh;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11217")
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/refresh/Customer.hbm.xml"
)
@SessionFactory
public class RefreshUsingEntityNameTest {
	private Customer customer;

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		customer = new Customer();
		scope.inTransaction(
				session ->
						session.persist( "CustomName", customer )
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createQuery( "delete from CustomName" ).executeUpdate()
		);
	}

	@Test
	public void testRefreshUsingEntityName(SessionFactoryScope scope) {
		scope.inSession(
				session ->
						session.refresh( customer )
		);
	}
}
