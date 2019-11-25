/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.milestone;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.dialect.HSQLDialect;
import org.hibernate.query.Query;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Fabio Massimo Ercoli
 */
@RequiresDialect( HSQLDialect.class )
@TestForIssue(jiraKey = "HHH-9662")
public class EventMilestoneTest extends BaseCoreFunctionalTestCase {

	@Test
	public void insertLoadAndQuery() {
		AtomicReference<Long> generatedId = new AtomicReference<>( null );

		doInHibernate( this::sessionFactory, session -> {
			EventMilestone entity = new EventMilestone();
			entity.setId( new EventMilestonePK( 739L ) );

			session.persist( entity );

			verifyEntity( entity );
			generatedId.set( entity.getId().getEventMilestoneId() );
		} );

		doInHibernate( this::sessionFactory, session -> {
			EventMilestonePK entityId = new EventMilestonePK( 739L );
			entityId.setEventMilestoneId( generatedId.get() );

			EventMilestone entity = session.load( EventMilestone.class, entityId );

			verifyEntity( entity );
		} );

		doInHibernate( this::sessionFactory, session -> {
			Query<EventMilestone> query = session.createQuery( "select e from EventMilestone e", EventMilestone.class );

			List<EventMilestone> list = query.list();
			assertEquals( 1, list.size() );

			EventMilestone entity = list.get( 0 );

			verifyEntity( entity );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EventMilestone.class };
	}

	private void verifyEntity(EventMilestone entity) {
		assertNotNull( entity );
		assertNotNull( entity.getId() );
		assertNotNull( entity.getId().getEventId() );
		assertNotNull( entity.getId().getEventMilestoneId() );
	}
}
