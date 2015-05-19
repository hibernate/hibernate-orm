/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.components.mappedsuperclass;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Jakob Braeuchi.
 * @author Gail Badner
 */
@TestForIssue(jiraKey = "HHH-9193")
public class EmbeddableWithNoDeclaredDataTest extends BaseEnversJPAFunctionalTestCase {
	private long id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { EntityWithEmbeddableWithNoDeclaredData.class, AbstractEmbeddable.class, EmbeddableWithNoDeclaredData.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		EntityWithEmbeddableWithNoDeclaredData entity = new EntityWithEmbeddableWithNoDeclaredData();
		entity.setName( "Entity 1" );
		entity.setValue( new EmbeddableWithNoDeclaredData( 84 ) );

		EntityTransaction tx = em.getTransaction();
		tx.begin();
		em.persist( entity );
		tx.commit();
		em.close();

		id = entity.getId();
	}

	@Test
	public void testEmbeddableThatExtendsMappedSuperclass() {

		// Reload and Compare Revision
		EntityManager em = getEntityManager();
		EntityWithEmbeddableWithNoDeclaredData entityLoaded = em.find( EntityWithEmbeddableWithNoDeclaredData.class, id );

		AuditReader reader = AuditReaderFactory.get( em );

		List<Number> revs = reader.getRevisions( EntityWithEmbeddableWithNoDeclaredData.class, id );
		Assert.assertEquals( 1, revs.size() );

		EntityWithEmbeddableWithNoDeclaredData entityRev1 = reader.find( EntityWithEmbeddableWithNoDeclaredData.class, id, revs.get( 0 ) );

		Assert.assertEquals( entityLoaded.getName(), entityRev1.getName() );

		// value should be null because there is no data in EmbeddableWithNoDeclaredData
		// and the fields in AbstractEmbeddable should not be audited.
		Assert.assertNull( entityRev1.getValue() );
	}
}
