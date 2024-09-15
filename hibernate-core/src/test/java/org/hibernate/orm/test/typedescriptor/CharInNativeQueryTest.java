/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.typedescriptor;


import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Strong Liu
 */
@DomainModel(
		annotatedClasses = Issue.class
)
@SessionFactory
public class CharInNativeQueryTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		Issue issue = new Issue();
		issue.setIssueNumber( "HHH-2304" );
		issue.setDescription( "Wrong type detection for sql type char(x) columns" );

		scope.inTransaction(
				session ->
						session.persist( issue )
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createQuery( "delete from Issue" ).executeUpdate()
		);
	}

	@Test
	@JiraKey(value = "HHH-2304")
	public void testNativeQuery(SessionFactoryScope scope) {


		Object issueNumber = scope.fromTransaction(
				session ->
						session.createNativeQuery( "select issue.issueNumber from Issue issue" ).uniqueResult()
		);

		assertNotNull( issueNumber );
		assertTrue( issueNumber instanceof String );
		assertEquals( "HHH-2304", issueNumber );
	}

}
