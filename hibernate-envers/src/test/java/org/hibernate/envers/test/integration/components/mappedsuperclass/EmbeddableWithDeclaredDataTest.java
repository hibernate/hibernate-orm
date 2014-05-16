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
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;

/**
 * @author Jakob Braeuchi.
 * @author Gail Badner
 */
@TestForIssue(jiraKey = "HHH-9193")
@FailureExpectedWithNewMetamodel( message = "Aggregate.getSuperType() returns null when it should return a MappedSuperclass")
public class EmbeddableWithDeclaredDataTest extends BaseEnversJPAFunctionalTestCase {
	private long id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { EntityWithEmbeddableWithDeclaredData.class, AbstractEmbeddable.class, EmbeddableWithDeclaredData.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		EntityWithEmbeddableWithDeclaredData entity = new EntityWithEmbeddableWithDeclaredData();
		entity.setName( "Entity 1" );
		entity.setValue( new EmbeddableWithDeclaredData( 42, "TestCodeart" ) );

		EntityTransaction tx = em.getTransaction();
		tx.begin();
		em.persist( entity );
		tx.commit();
		em.close();

		id = entity.getId();
	}

	@Test
	@FailureExpected( jiraKey = "HHH-9193" )
	public void testEmbeddableThatExtendsMappedSuperclass() {

		// Reload and Compare Revision
		EntityManager em = getEntityManager();
		EntityWithEmbeddableWithDeclaredData entityLoaded = em.find( EntityWithEmbeddableWithDeclaredData.class, id );

		AuditReader reader = AuditReaderFactory.get( em );

		List<Number> revs = reader.getRevisions( EntityWithEmbeddableWithDeclaredData.class, id );
		Assert.assertEquals( 1, revs.size() );

		EntityWithEmbeddableWithDeclaredData entityRev1 = reader.find( EntityWithEmbeddableWithDeclaredData.class, id, revs.get( 0 ) );

		Assert.assertEquals( entityLoaded.getName(), entityRev1.getName() );

		// only value.codeArt should be audited because it is the only audited field in EmbeddableWithDeclaredData;
		// fields in AbstractEmbeddable should not be audited.
		Assert.assertEquals( entityLoaded.getValue().getCodeart(), entityRev1.getValue().getCodeart() );
		Assert.assertNull( entityRev1.getValue().getCode() );
	}
}
