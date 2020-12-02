/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.cascade.multilevel;

import org.junit.jupiter.api.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@DomainModel(annotatedClasses = {
		Top.class,
		Middle.class,
		Bottom.class
})
@SessionFactory
public class MultiLevelCascadeTest {

	@TestForIssue(jiraKey = "HHH-5299")
	@Test
	public void test(SessionFactoryScope scope) {
		final Top top = new Top();

		scope.inTransaction(
				session -> {
					session.persist( top );
					// Flush 1
					session.flush();

					Middle middle = new Middle( 1l );
					top.addMiddle( middle );
					middle.setTop( top );
					Bottom bottom = new Bottom();
					middle.setBottom( bottom );
					bottom.setMiddle( middle );

					Middle middle2 = new Middle( 2l );
					top.addMiddle( middle2 );
					middle2.setTop( top );
					Bottom bottom2 = new Bottom();
					middle2.setBottom( bottom2 );
					bottom2.setMiddle( middle2 );
					// Flush 2
					session.flush();
				}
		);

		scope.inTransaction(
				session -> {
					Top found = session.find( Top.class, top.getId() );

					assertEquals( 2, found.getMiddles().size() );
					for ( Middle loadedMiddle : found.getMiddles() ) {
						assertSame( found, loadedMiddle.getTop() );
						assertNotNull( loadedMiddle.getBottom() );
					}
					session.remove( found );
				}
		);
	}
}
