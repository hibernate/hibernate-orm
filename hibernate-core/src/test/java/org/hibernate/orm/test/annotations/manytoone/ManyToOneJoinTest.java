/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.manytoone;


import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				BiggestForest.class,
				ForestType.class,
				TreeType.class
		}
)
@SessionFactory
public class ManyToOneJoinTest {

	@AfterEach
	public void teardDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.getSessionFactory().getSchemaManager().truncateMappedObjects()
		);
	}

	@Test
	public void testManyToOneJoinTable(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					ForestType forest = new ForestType();
					forest.setName( "Original forest" );
					session.persist( forest );

					TreeType tree = new TreeType();
					tree.setForestType( forest );
					tree.setAlternativeForestType( forest );
					tree.setName( "just a tree" );
					session.persist( tree );

					session.flush();
					session.clear();

					tree = session.get( TreeType.class, tree.getId() );
					assertNotNull( tree.getForestType() );
					assertNotNull( tree.getAlternativeForestType() );
					session.clear();

					forest = session.get( ForestType.class, forest.getId() );
					assertEquals( 1, forest.getTrees().size() );
					assertEquals( tree.getId(), forest.getTrees().iterator().next().getId() );
				}
		);
	}

	@Test
	public void testOneToOneJoinTable(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					ForestType forest = new ForestType();
					forest.setName( "Original forest" );
					session.persist( forest );

					BiggestForest forestRepr = new BiggestForest();
					forestRepr.setType( forest );
					forest.setBiggestRepresentative( forestRepr );
					session.persist( forestRepr );
					session.flush();
					session.clear();

					forest = session.get( ForestType.class, forest.getId() );
					assertNotNull( forest.getBiggestRepresentative() );
					assertEquals( forest, forest.getBiggestRepresentative().getType() );
				}
		);
	}

	@Test
	public void testOneToOneJoinTable2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {

					BiggestForest forestRepr = new BiggestForest();
					session.persist( forestRepr );
					session.flush();
					session.clear();

					BiggestForest biggestForest = session.get( BiggestForest.class, forestRepr.getId() );
					assertNotNull( biggestForest );
					assertNull( biggestForest.getType() );
				}
		);
	}

}
