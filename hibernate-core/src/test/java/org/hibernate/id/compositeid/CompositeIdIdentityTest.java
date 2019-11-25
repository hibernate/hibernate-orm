/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.compositeid;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.query.Query;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Fabio Massimo Ercoli
 */
@RequiresDialectFeature(DialectChecks.SupportsCompositeNestedIdentityColumns.class)
@TestForIssue(jiraKey = "HHH-9662")
public class CompositeIdIdentityTest extends BaseCoreFunctionalTestCase {

	@Test
	public void insertLoadAndQuery() {
		AtomicReference<Long> generatedId = new AtomicReference<>( null );

		doInHibernate( this::sessionFactory, session -> {
			CompositeIdIdentityEntity entity = new CompositeIdIdentityEntity();
			entity.setId( new CompositeId( 739L ) );

			session.persist( entity );

			verifyEntity( entity );
			generatedId.set( entity.getId().getGeneratedId() );
		} );

		doInHibernate( this::sessionFactory, session -> {
			CompositeId entityId = new CompositeId( 739L );
			entityId.setGeneratedId( generatedId.get() );

			CompositeIdIdentityEntity entity = session.load( CompositeIdIdentityEntity.class, entityId );

			verifyEntity( entity );
		} );

		doInHibernate( this::sessionFactory, session -> {
			Query<CompositeIdIdentityEntity> query = session.createQuery( "select e from Entity e", CompositeIdIdentityEntity.class );

			List<CompositeIdIdentityEntity> list = query.list();
			assertEquals( 1, list.size() );

			CompositeIdIdentityEntity entity = list.get( 0 );

			verifyEntity( entity );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { CompositeIdIdentityEntity.class };
	}

	private void verifyEntity(CompositeIdIdentityEntity entity) {
		assertNotNull( entity );
		assertNotNull( entity.getId() );
		assertNotNull( entity.getId().getId() );
		assertNotNull( entity.getId().getGeneratedId() );
	}
}
