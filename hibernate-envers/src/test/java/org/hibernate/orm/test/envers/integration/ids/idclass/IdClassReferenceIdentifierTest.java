/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.ids.idclass;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Matthew Morrissette (yinzara at gmail dot com)
 */
@JiraKey(value = "HHH-10667")
@EnversTest
@Jpa(annotatedClasses = {
		ReferenceIdentifierEntity.class,
		ReferenceIdentifierClassId.class,
		ClassType.class,
		IntegerGeneratedIdentityEntity.class
})
public class IdClassReferenceIdentifierTest {
	private ReferenceIdentifierClassId entityId = null;
	private Integer typeId = null;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			ClassType type = new ClassType( "type", "initial description" );
			em.persist( type );

			IntegerGeneratedIdentityEntity type2 = new IntegerGeneratedIdentityEntity();
			em.persist( type2 );

			ReferenceIdentifierEntity entity = new ReferenceIdentifierEntity();
			entity.setSampleValue( "initial data" );
			entity.setType( type );
			entity.setIiie( type2 );

			em.persist( entity );

			typeId = type2.getId();
			entityId = new ReferenceIdentifierClassId( typeId, type.getType() );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			ClassType type = em.find( ClassType.class, "type" );
			type.setDescription( "modified description" );
			em.merge( type );
		} );

		// Revision 3
		scope.inTransaction( em -> {
			ReferenceIdentifierEntity entity = em.find( ReferenceIdentifierEntity.class, entityId );
			entity.setSampleValue( "modified data" );
			em.merge( entity );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( ClassType.class, "type" ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( IntegerGeneratedIdentityEntity.class, typeId ) );
			assertEquals( Arrays.asList( 1, 3 ), auditReader.getRevisions( ReferenceIdentifierEntity.class, entityId ) );
		} );
	}

	@Test
	public void testHistoryOfEntity(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			// given
			ReferenceIdentifierEntity entity = new ReferenceIdentifierEntity(
					new IntegerGeneratedIdentityEntity( typeId ),
					new ClassType( "type", "initial description" ),
					"initial data"
			);

			// when
			ReferenceIdentifierEntity ver1 = auditReader.find( ReferenceIdentifierEntity.class, entityId, 1 );

			// then
			assertEquals( entity.getIiie().getId(), ver1.getIiie().getId() );
			assertEquals( entity.getSampleValue(), ver1.getSampleValue() );
			assertEquals( entity.getType().getType(), ver1.getType().getType() );
			assertEquals( entity.getType().getDescription(), ver1.getType().getDescription() );

			// given
			entity.setSampleValue( "modified data" );
			entity.getType().setDescription( "modified description" );

			// when
			ReferenceIdentifierEntity ver2 = auditReader.find( ReferenceIdentifierEntity.class, entityId, 3 );

			// then
			assertEquals( entity.getIiie().getId(), ver2.getIiie().getId() );
			assertEquals( entity.getSampleValue(), ver2.getSampleValue() );
			assertEquals( entity.getType().getType(), ver2.getType().getType() );
			assertEquals( entity.getType().getDescription(), ver2.getType().getDescription() );
		} );
	}
}
