/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytoone;



import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
		scope.getSessionFactory().getSchemaManager().truncate();
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
