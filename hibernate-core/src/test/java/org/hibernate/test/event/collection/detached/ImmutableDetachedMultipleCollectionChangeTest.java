/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.event.collection.detached;

import org.hibernate.HibernateException;
import org.hibernate.event.spi.AbstractCollectionEvent;
import org.hibernate.event.spi.InitializeCollectionEvent;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * HHH-3007 - Unchanged persistent set gets marked dirty on session.merge()
 * <p/>
 * Description: "Persistent sets are marked dirty on session.merge() even if there have been no changes to the
 * collection. This is especially painful when the collection is immutable and results in an 'changed an immutable
 * collection instance' exception on flush."
 *
 * @see org.hibernate.test.event.collection.detached.DetachedMultipleCollectionChangeTest#testCollectionIsNotMarkedDirtyUnlessModified()
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
@TestForIssue(jiraKey = "HHH-3007")
public class ImmutableDetachedMultipleCollectionChangeTest extends BaseCoreFunctionalTestCase {

	private MultipleCollectionEntity detachedEntity;

	@Override
	protected String[] getMappings() {
		return new String[] {"event/collection/detached/ImmutableMultipleCollectionBagMapping.hbm.xml"};
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Override
	protected void prepareTest() throws Exception {
		openSession();
		session.beginTransaction();

		final MultipleCollectionEntity mce = new MultipleCollectionEntity();
		mce.setText( "MultipleCollectionEntity-1" );

		final MultipleCollectionRefEntity1 re1n1 = new MultipleCollectionRefEntity1();
		re1n1.setText( "MultipleCollectionRefEntity1-1" );
		re1n1.setMultipleCollectionEntity( mce );

		final MultipleCollectionRefEntity1 re1n2 = new MultipleCollectionRefEntity1();
		re1n2.setText( "MultipleCollectionRefEntity1-2" );
		re1n2.setMultipleCollectionEntity( mce );

		mce.addRefEntity1( re1n1 );
		mce.addRefEntity1( re1n2 );

		session.save( mce );
		session.getTransaction().commit();
		session.close();

		detachedEntity = mce.deepCopy();
	}

	@Test
	public void testMergeDetachedEntityWithUnmodifiedImmutableCollections() {
		final List<AbstractCollectionEvent> events = new MultipleCollectionListeners( sessionFactory() ).getEvents();
		assertEquals( 0, events.size() );

		openSession();
		session.beginTransaction();
		session.merge( detachedEntity );
		session.getTransaction().commit();
		session.close();

		assertEquals( 1, events.size() );
		assertTrue( events.get( 0 ) instanceof InitializeCollectionEvent );
	}

	@Test
	public void testMergeDetachedEntityWithModifiedNonEmptyImmutableCollection() {
		final MultipleCollectionRefEntity1 re1n1 = detachedEntity.getRefEntities1().get( 0 );
		detachedEntity.removeRefEntity1( re1n1 );

		openSession();
		session.beginTransaction();
		try {
			session.merge( detachedEntity );
			session.getTransaction().commit();
			fail( "should have failed because of immutable collection change" );
		}
		catch (HibernateException e) {
			// expected
		}
		finally {
			session.close();
		}
	}

	@Test
	public void testMergeDetachedEntityWithModifiedEmptyImmutableCollection() {
		final MultipleCollectionRefEntity2 re2n1 = new MultipleCollectionRefEntity2();
		re2n1.setText( "MultipleCollectionRefEntity2-1" );
		re2n1.setMultipleCollectionEntity( detachedEntity );
		detachedEntity.addRefEntity2( re2n1 );

		openSession();
		session.beginTransaction();
		try {
			session.merge( detachedEntity );
			session.getTransaction().commit();
			fail( "should have failed because of immutable collection change" );
		}
		catch (HibernateException e) {
			// expected
		}
		finally {
			session.close();
		}
	}
}
