/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.cascade.multilevel;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@Jpa(annotatedClasses = {
		Top.class,
		Middle.class,
		Bottom.class
})
public class MultiLevelCascadeTest {

	@TestForIssue(jiraKey = "HHH-5299")
	@Test
	public void test(EntityManagerFactoryScope scope) {
		final Top top = new Top();

		scope.inTransaction(
				entityManager -> {
					entityManager.persist( top );
					// Flush 1
					entityManager.flush();

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
					entityManager.flush();
				}
		);

		scope.inTransaction(
				entityManager -> {
					Top found = entityManager.find( Top.class, top.getId() );

					assertEquals( 2, found.getMiddles().size() );
					for ( Middle loadedMiddle : found.getMiddles() ) {
						assertSame( found, loadedMiddle.getTop() );
						assertNotNull( loadedMiddle.getBottom() );
					}
					entityManager.remove( found );
				}
		);
	}
}
