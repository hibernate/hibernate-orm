/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity.trackmodifiedentities;

import jakarta.persistence.EntityManager;

import org.hibernate.envers.EntityTrackingRevisionListener;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.StrIntTestEntity;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.reventity.trackmodifiedentities.CustomTrackingRevisionEntity;
import org.hibernate.orm.test.envers.entities.reventity.trackmodifiedentities.CustomTrackingRevisionListener;
import org.hibernate.orm.test.envers.entities.reventity.trackmodifiedentities.ModifiedEntityTypeEntity;
import org.hibernate.orm.test.envers.tools.TestTools;

import org.junit.Test;


/**
 * Tests proper behavior of entity listener that implements {@link EntityTrackingRevisionListener}
 * interface. {@link CustomTrackingRevisionListener} shall be notified whenever an entity instance has been
 * added, modified or removed, so that changed entity name can be persisted.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class CustomTrackingEntitiesTest extends BaseEnversJPAFunctionalTestCase {
	private Integer steId = null;
	private Integer siteId = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				ModifiedEntityTypeEntity.class,
				StrTestEntity.class,
				StrIntTestEntity.class,
				CustomTrackingRevisionEntity.class
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1 - Adding two entities
		em.getTransaction().begin();
		StrTestEntity ste = new StrTestEntity( "x" );
		StrIntTestEntity site = new StrIntTestEntity( "y", 1 );
		em.persist( ste );
		em.persist( site );
		steId = ste.getId();
		siteId = site.getId();
		em.getTransaction().commit();

		// Revision 2 - Modifying one entity
		em.getTransaction().begin();
		site = em.find( StrIntTestEntity.class, siteId );
		site.setNumber( 2 );
		em.getTransaction().commit();

		// Revision 3 - Deleting both entities
		em.getTransaction().begin();
		ste = em.find( StrTestEntity.class, steId );
		site = em.find( StrIntTestEntity.class, siteId );
		em.remove( ste );
		em.remove( site );
		em.getTransaction().commit();
	}

	@Test
	public void testTrackAddedEntities() {
		ModifiedEntityTypeEntity steDescriptor = new ModifiedEntityTypeEntity( StrTestEntity.class.getName() );
		ModifiedEntityTypeEntity siteDescriptor = new ModifiedEntityTypeEntity( StrIntTestEntity.class.getName() );

		CustomTrackingRevisionEntity ctre = getAuditReader().findRevision( CustomTrackingRevisionEntity.class, 1 );

		assert ctre.getModifiedEntityTypes() != null;
		assert ctre.getModifiedEntityTypes().size() == 2;
		assert TestTools.makeSet( steDescriptor, siteDescriptor ).equals( ctre.getModifiedEntityTypes() );
	}

	@Test
	public void testTrackModifiedEntities() {
		ModifiedEntityTypeEntity siteDescriptor = new ModifiedEntityTypeEntity( StrIntTestEntity.class.getName() );

		CustomTrackingRevisionEntity ctre = getAuditReader().findRevision( CustomTrackingRevisionEntity.class, 2 );

		assert ctre.getModifiedEntityTypes() != null;
		assert ctre.getModifiedEntityTypes().size() == 1;
		assert TestTools.makeSet( siteDescriptor ).equals( ctre.getModifiedEntityTypes() );
	}

	@Test
	public void testTrackDeletedEntities() {
		ModifiedEntityTypeEntity steDescriptor = new ModifiedEntityTypeEntity( StrTestEntity.class.getName() );
		ModifiedEntityTypeEntity siteDescriptor = new ModifiedEntityTypeEntity( StrIntTestEntity.class.getName() );

		CustomTrackingRevisionEntity ctre = getAuditReader().findRevision( CustomTrackingRevisionEntity.class, 3 );

		assert ctre.getModifiedEntityTypes() != null;
		assert ctre.getModifiedEntityTypes().size() == 2;
		assert TestTools.makeSet( steDescriptor, siteDescriptor ).equals( ctre.getModifiedEntityTypes() );
	}

	@Test(expected = AuditException.class)
	public void testFindEntitiesChangedInRevisionException() {
		getAuditReader().getCrossTypeRevisionChangesReader();
	}
}
