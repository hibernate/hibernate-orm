/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers.test.integration.components.mappedsuperclass;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;

/**
 * @author Jakob Braeuchi.
 * @author Gail Badner
 */
@TestForIssue(jiraKey = "HHH-9193")
@FailureExpectedWithNewMetamodel( message = "Aggregate.getSuperType() returns null when it should return a MappedSuperclass")
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
