/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.performance;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.test.BaseEnversFunctionalTestCase;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.entities.onetomany.SetRefEdEntity;
import org.hibernate.envers.test.entities.onetomany.SetRefIngEntity;

import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class EvictAuditDataAfterCommitTest extends BaseEnversFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrTestEntity.class, SetRefEdEntity.class, SetRefIngEntity.class};
	}

	@Test
	@TestForIssue(jiraKey = "HHH-6614")
	public void testSessionCacheClear() {
		Session session = openSession();
		session.getTransaction().begin();
		StrTestEntity ste = new StrTestEntity( "data" );
		session.persist( ste );
		session.getTransaction().commit();
		checkEmptyAuditSessionCache( session, "org.hibernate.envers.test.entities.StrTestEntity_AUD" );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-6614")
	public void testSessionCacheCollectionClear() {
		final String[] auditEntityNames = new String[] {
				"org.hibernate.envers.test.entities.onetomany.SetRefEdEntity_AUD",
				"org.hibernate.envers.test.entities.onetomany.SetRefIngEntity_AUD"
		};

		SetRefEdEntity ed1 = new SetRefEdEntity( 1, "data_ed_1" );
		SetRefEdEntity ed2 = new SetRefEdEntity( 2, "data_ed_2" );
		SetRefIngEntity ing1 = new SetRefIngEntity( 3, "data_ing_1" );
		SetRefIngEntity ing2 = new SetRefIngEntity( 4, "data_ing_2" );

		Session session = openSession();
		session.getTransaction().begin();
		session.persist( ed1 );
		session.persist( ed2 );
		session.persist( ing1 );
		session.persist( ing2 );
		session.getTransaction().commit();
		checkEmptyAuditSessionCache( session, auditEntityNames );

		session.getTransaction().begin();
		ed1 = (SetRefEdEntity) session.load( SetRefEdEntity.class, ed1.getId() );
		ing1.setReference( ed1 );
		ing2.setReference( ed1 );
		session.getTransaction().commit();
		checkEmptyAuditSessionCache( session, auditEntityNames );

		session.getTransaction().begin();
		ed2 = (SetRefEdEntity) session.load( SetRefEdEntity.class, ed2.getId() );
		Set<SetRefIngEntity> reffering = new HashSet<SetRefIngEntity>();
		reffering.add( ing1 );
		reffering.add( ing2 );
		ed2.setReffering( reffering );
		session.getTransaction().commit();
		checkEmptyAuditSessionCache( session, auditEntityNames );

		session.getTransaction().begin();
		ed2 = (SetRefEdEntity) session.load( SetRefEdEntity.class, ed2.getId() );
		ed2.getReffering().remove( ing1 );
		session.getTransaction().commit();
		checkEmptyAuditSessionCache( session, auditEntityNames );

		session.getTransaction().begin();
		ed2 = (SetRefEdEntity) session.load( SetRefEdEntity.class, ed2.getId() );
		ed2.getReffering().iterator().next().setData( "mod_data_ing_2" );
		session.getTransaction().commit();
		checkEmptyAuditSessionCache( session, auditEntityNames );

		session.close();
	}

	private void checkEmptyAuditSessionCache(Session session, String... auditEntityNames) {
		List<String> entityNames = Arrays.asList( auditEntityNames );
		PersistenceContext persistenceContext = ((SessionImplementor) session).getPersistenceContext();
		for ( Map.Entry<Object, EntityEntry> entrySet : persistenceContext.reentrantSafeEntityEntries() ) {
			final EntityEntry entityEntry = entrySet.getValue();
			if ( entityNames.contains( entityEntry.getEntityName() ) ) {
				assert false : "Audit data shall not be stored in the session level cache. This causes performance issues.";
			}
			Assert.assertFalse(
					"Revision entity shall not be stored in the session level cache. This causes performance issues.",
					SequenceIdRevisionEntity.class.getName().equals( entityEntry.getEntityName() )
			);
		}
	}
}