/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.event.collection.detached;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.event.spi.AbstractCollectionEvent;
import org.hibernate.event.spi.PostCollectionRecreateEvent;
import org.hibernate.event.spi.PreCollectionRemoveEvent;
import org.hibernate.event.spi.PreCollectionUpdateEvent;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * Test HHH-6361: Collection events may contain wrong stored snapshot afterQuery
 * merging a detached entity into the persistencecontext.
 * 
 * @author Erik-Berndt Scheper
 */
@TestForIssue( jiraKey = "HHH-6361" )
public class DetachedMultipleCollectionChangeTest extends BaseCoreFunctionalTestCase {

	@Override
	public String[] getMappings() {
		return new String[] { "event/collection/detached/MultipleCollectionBagMapping.hbm.xml" };
	}

	@Override
	protected void cleanupTest() {
		Session s = null;
		s = openSession();
		s.beginTransaction();
		s.createQuery("delete MultipleCollectionRefEntity1").executeUpdate();
		s.createQuery("delete MultipleCollectionRefEntity2").executeUpdate();
		s.createQuery("delete MultipleCollectionEntity").executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testMergeMultipleCollectionChangeEvents() {
		MultipleCollectionListeners listeners = new MultipleCollectionListeners(
				sessionFactory());
		listeners.clear();
		int eventCount = 0;

		List<MultipleCollectionRefEntity1> oldRefentities1
				= new ArrayList<MultipleCollectionRefEntity1>();
		List<MultipleCollectionRefEntity2> oldRefentities2
				= new ArrayList<MultipleCollectionRefEntity2>();

		Session s = openSession();
		s.beginTransaction();

		MultipleCollectionEntity mce = new MultipleCollectionEntity();
		mce.setText("MultipleCollectionEntity-1");

		s.save(mce);
		s.getTransaction().commit();

		checkListener(listeners, listeners.getPreCollectionRecreateListener(),
				mce, oldRefentities1, eventCount++);
		checkListener(listeners, listeners.getPostCollectionRecreateListener(),
				mce, oldRefentities1, eventCount++);
		checkListener(listeners, listeners.getPreCollectionRecreateListener(),
				mce, oldRefentities2, eventCount++);
		checkListener(listeners, listeners.getPostCollectionRecreateListener(),
				mce, oldRefentities2, eventCount++);
		checkEventCount(listeners, eventCount);

		s.close();

		Long mceId1 = mce.getId();
		assertNotNull(mceId1);

		// add new entities to both collections

		MultipleCollectionEntity prevMce = mce.deepCopy();
		oldRefentities1 = prevMce.getRefEntities1();
		oldRefentities2 = prevMce.getRefEntities2();

		listeners.clear();
		eventCount = 0;

		s = openSession();
		s.beginTransaction();

		MultipleCollectionRefEntity1 re1_1 = new MultipleCollectionRefEntity1();
		re1_1.setText("MultipleCollectionRefEntity1-1");
		re1_1.setMultipleCollectionEntity(mce);

		MultipleCollectionRefEntity1 re1_2 = new MultipleCollectionRefEntity1();
		re1_2.setText("MultipleCollectionRefEntity1-2");
		re1_2.setMultipleCollectionEntity(mce);

		mce.addRefEntity1(re1_1);
		mce.addRefEntity1(re1_2);

		mce = (MultipleCollectionEntity) s.merge(mce);

		s.getTransaction().commit();
		s.close();

		checkListener(listeners, listeners.getInitializeCollectionListener(),
				mce, null, eventCount++);
		checkListener(listeners, listeners.getPreCollectionUpdateListener(),
				mce, oldRefentities1, eventCount++);
		checkListener(listeners, listeners.getPostCollectionUpdateListener(),
				mce, mce.getRefEntities1(), eventCount++);

		s = openSession();
		s.beginTransaction();

		MultipleCollectionRefEntity2 re2_1 = new MultipleCollectionRefEntity2();
		re2_1.setText("MultipleCollectionRefEntity2-1");
		re2_1.setMultipleCollectionEntity(mce);

		MultipleCollectionRefEntity2 re2_2 = new MultipleCollectionRefEntity2();
		re2_2.setText("MultipleCollectionRefEntity2-2");
		re2_2.setMultipleCollectionEntity(mce);

		mce.addRefEntity2(re2_1);
		mce.addRefEntity2(re2_2);

		mce = (MultipleCollectionEntity) s.merge(mce);

		s.getTransaction().commit();

		checkListener(listeners, listeners.getInitializeCollectionListener(),
				mce, null, eventCount++);
		checkListener(listeners, listeners.getPreCollectionUpdateListener(),
				mce, oldRefentities2, eventCount++);
		checkListener(listeners, listeners.getPostCollectionUpdateListener(),
				mce, mce.getRefEntities2(), eventCount++);
		checkEventCount(listeners, eventCount);

		s.close();

		for (MultipleCollectionRefEntity1 refEnt1 : mce.getRefEntities1()) {
			assertNotNull(refEnt1.getId());
		}
		for (MultipleCollectionRefEntity2 refEnt2 : mce.getRefEntities2()) {
			assertNotNull(refEnt2.getId());
		}

		// remove and add entities in both collections

		prevMce = mce.deepCopy();
		oldRefentities1 = prevMce.getRefEntities1();
		oldRefentities2 = prevMce.getRefEntities2();

		listeners.clear();
		eventCount = 0;

		s = openSession();
		s.beginTransaction();

		assertEquals(2, mce.getRefEntities1().size());
		assertEquals(2, mce.getRefEntities2().size());

		mce.removeRefEntity1(re1_2);

		MultipleCollectionRefEntity1 re1_3 = new MultipleCollectionRefEntity1();
		re1_3.setText("MultipleCollectionRefEntity1-3");
		re1_3.setMultipleCollectionEntity(mce);
		mce.addRefEntity1(re1_3);

		mce = (MultipleCollectionEntity) s.merge(mce);

		s.getTransaction().commit();
		s.close();

		checkListener(listeners, listeners.getInitializeCollectionListener(),
				mce, null, eventCount++);
		checkListener(listeners, listeners.getPreCollectionUpdateListener(),
				mce, oldRefentities1, eventCount++);
		checkListener(listeners, listeners.getPostCollectionUpdateListener(),
				mce, mce.getRefEntities1(), eventCount++);

		s = openSession();
		s.beginTransaction();

		mce.removeRefEntity2(re2_2);

		MultipleCollectionRefEntity2 re2_3 = new MultipleCollectionRefEntity2();
		re2_3.setText("MultipleCollectionRefEntity2-3");
		re2_3.setMultipleCollectionEntity(mce);
		mce.addRefEntity2(re2_3);

		mce = (MultipleCollectionEntity) s.merge(mce);

		s.getTransaction().commit();

		checkListener(listeners, listeners.getInitializeCollectionListener(),
				mce, null, eventCount++);
		checkListener(listeners, listeners.getPreCollectionUpdateListener(),
				mce, oldRefentities2, eventCount++);
		checkListener(listeners, listeners.getPostCollectionUpdateListener(),
				mce, mce.getRefEntities2(), eventCount++);

		checkEventCount(listeners, eventCount);

		s.close();
	}

	protected void checkListener(
			MultipleCollectionListeners listeners,
			MultipleCollectionListeners.Listener listenerExpected,
			org.hibernate.test.event.collection.Entity ownerExpected,
			List<? extends org.hibernate.test.event.collection.Entity> expectedCollectionEntrySnapshot,
			int index) {
		AbstractCollectionEvent event = listeners
				.getEvents().get(index);

		assertSame(listenerExpected, listeners.getListenersCalled().get(index));
		assertEquals(ownerExpected, event.getAffectedOwnerOrNull());
		assertEquals(ownerExpected.getId(), event.getAffectedOwnerIdOrNull());
		assertEquals(ownerExpected.getClass().getName(),
				event.getAffectedOwnerEntityName());

		if (event instanceof PreCollectionUpdateEvent) {
			Serializable snapshot = listeners.getSnapshots().get(index);
			assertEquals(expectedCollectionEntrySnapshot, snapshot);
		}
		if (event instanceof PreCollectionRemoveEvent) {
			Serializable snapshot = listeners.getSnapshots().get(index);
			assertEquals(expectedCollectionEntrySnapshot, snapshot);
		}
		if (event instanceof PostCollectionRecreateEvent) {
			Serializable snapshot = listeners.getSnapshots().get(index);
			assertEquals(expectedCollectionEntrySnapshot, snapshot);
		}

	}

	private void checkEventCount(MultipleCollectionListeners listeners,
			int nEventsExpected) {
		assertEquals(nEventsExpected, listeners.getListenersCalled().size());
		assertEquals(nEventsExpected, listeners.getEvents().size());
	}

}
