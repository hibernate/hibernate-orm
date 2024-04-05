/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.merge;


import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;


/**
 * @author Chris Cranford
 */
@JiraKey("HHH-12592")
@DomainModel(
		annotatedClasses = {
				Leaf.class, Root.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class MergeEnhancedDetachedOrphanRemovalTest {

	@Test
	public void testMergeDetachedOrphanRemoval(SessionFactoryScope scope) {
		final Root entity = scope.fromTransaction( session -> {
			Root root = new Root();
			root.setName( "new" );
			session.save( root );
			return root;
		} );

		scope.inTransaction( session -> {
			entity.setName( "updated" );
			Root entityMerged = session.merge( entity );
			assertNotSame( entity, entityMerged );
			assertNotSame( entity.getLeaves(), entityMerged.getLeaves() );
		} );
	}

	@Test
	public void testMergeDetachedNonEmptyCollection(SessionFactoryScope scope) {
		final Root entity = scope.fromTransaction( session -> {
			Root root = new Root();
			root.setName( "new" );
			Leaf leaf = new Leaf();
			leaf.setRoot( root );
			root.getLeaves().add( leaf );
			session.save( root );
			return root;
		} );

		scope.inTransaction( session -> {
			entity.setName( "updated" );
			Root entityMerged = session.merge( entity );
			assertNotSame( entity, entityMerged );
			assertNotSame( entity.getLeaves(), entityMerged.getLeaves() );
		} );
	}
}
