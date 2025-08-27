/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.ids.idclass;

import junit.framework.Assert;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import jakarta.persistence.EntityManager;
import java.util.Arrays;

/**
 * @author Matthew Morrissette (yinzara at gmail dot com)
 */
@JiraKey(value = "HHH-10667")
public class IdClassReferenceIdentifierTest extends BaseEnversJPAFunctionalTestCase {
	private ReferenceIdentifierClassId entityId = null;
	private Integer typeId = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				ReferenceIdentifierEntity.class,
				ReferenceIdentifierClassId.class,
				ClassType.class,
				IntegerGeneratedIdentityEntity.class
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
		ClassType type = new ClassType( "type", "initial description" );
		em.persist( type );

		IntegerGeneratedIdentityEntity type2 = new IntegerGeneratedIdentityEntity();
		em.persist(type2);

		ReferenceIdentifierEntity entity = new ReferenceIdentifierEntity();
		entity.setSampleValue( "initial data" );
		entity.setType( type );
		entity.setIiie( type2 );


		em.persist( entity );
		em.getTransaction().commit();

		typeId = type2.getId();
		entityId = new ReferenceIdentifierClassId( typeId, type.getType() );

		// Revision 2
		em.getTransaction().begin();
		type = em.find( ClassType.class, type.getType() );
		type.setDescription( "modified description" );
		em.merge( type );
		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();
		entity = em.find( ReferenceIdentifierEntity.class, entityId );
		entity.setSampleValue( "modified data" );
		em.merge( entity );
		em.getTransaction().commit();

		em.close();
	}

	@Test
	public void testRevisionsCounts() {
		Assert.assertEquals( Arrays.asList( 1, 2 ), getAuditReader().getRevisions( ClassType.class, "type" ) );
		Assert.assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( IntegerGeneratedIdentityEntity.class, typeId ) );
		Assert.assertEquals( Arrays.asList( 1, 3 ), getAuditReader().getRevisions( ReferenceIdentifierEntity.class, entityId ) );
	}

	@Test
	public void testHistoryOfEntity() {
		// given
		ReferenceIdentifierEntity entity = new ReferenceIdentifierEntity( new IntegerGeneratedIdentityEntity(typeId), new ClassType( "type", "initial description" ), "initial data" );

		// when
		ReferenceIdentifierEntity ver1 = getAuditReader().find( ReferenceIdentifierEntity.class, entityId, 1 );

		// then
		Assert.assertEquals( entity.getIiie().getId(), ver1.getIiie().getId() );
		Assert.assertEquals( entity.getSampleValue(), ver1.getSampleValue() );
		Assert.assertEquals( entity.getType().getType(), ver1.getType().getType() );
		Assert.assertEquals( entity.getType().getDescription(), ver1.getType().getDescription() );

		// given
		entity.setSampleValue( "modified data" );
		entity.getType().setDescription( "modified description" );

		// when
		ReferenceIdentifierEntity ver2 = getAuditReader().find( ReferenceIdentifierEntity.class, entityId, 3 );

		// then
		Assert.assertEquals( entity.getIiie().getId(), ver2.getIiie().getId() );
		Assert.assertEquals( entity.getSampleValue(), ver2.getSampleValue() );
		Assert.assertEquals( entity.getType().getType(), ver2.getType().getType() );
		Assert.assertEquals( entity.getType().getDescription(), ver2.getType().getDescription() );
	}

}
