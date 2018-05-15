/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.merge;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNotSame;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-12592")
@RunWith(BytecodeEnhancerRunner.class)
public class MergeEnhancedDetachedOrphanRemovalTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Leaf.class, Root.class };
	}

	@Test
	public void testMergeDetachedOrphanRemoval() {
		final Root entity = doInHibernate( this::sessionFactory, session -> {
			Root root = new Root();
			root.setName( "new" );
			session.save( root );
			return root;
		} );

		doInHibernate( this::sessionFactory, session -> {
			entity.setName( "updated" );
			Root entityMerged = (Root) session.merge( entity );
			assertNotSame( entity, entityMerged );
			assertNotSame( entity.getLeaves(), entityMerged.getLeaves() );
		} );
	}

	@Test
	public void testMergeDetachedNonEmptyCollection() {
		final Root entity = doInHibernate( this::sessionFactory, session -> {
			Root root = new Root();
			root.setName( "new" );
			Leaf leaf = new Leaf();
			leaf.setRoot( root );
			root.getLeaves().add( leaf );
			session.save( root );
			return root;
		} );

		doInHibernate( this::sessionFactory, session -> {
			entity.setName( "updated" );
			Root entityMerged = (Root) session.merge( entity );
			assertNotSame( entity, entityMerged );
			assertNotSame( entity.getLeaves(), entityMerged.getLeaves() );
		} );
	}
}
