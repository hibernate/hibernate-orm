/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.ids.idclass;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-4751")
@EnversTest
@Jpa(annotatedClasses = {SampleClass.class, RelationalClassId.class, ClassType.class})
public class IdClassWithRelationTest {
	private RelationalClassId entityId = null;
	private String typeId = null;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			ClassType type = new ClassType( "type", "initial description" );
			SampleClass entity = new SampleClass();
			entity.setType( type );
			entity.setSampleValue( "initial data" );
			em.persist( type );
			em.persist( entity );

			typeId = type.getType();
			entityId = new RelationalClassId( entity.getId(), new ClassType( "type", "initial description" ) );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			ClassType type = em.find( ClassType.class, typeId );
			type.setDescription( "modified description" );
			em.merge( type );
		} );

		// Revision 3
		scope.inTransaction( em -> {
			SampleClass entity = em.find( SampleClass.class, entityId );
			entity.setSampleValue( "modified data" );
			em.merge( entity );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( ClassType.class, typeId ) );
			assertEquals( Arrays.asList( 1, 3 ), auditReader.getRevisions( SampleClass.class, entityId ) );
		} );
	}

	@Test
	public void testHistoryOfEntity(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			// given
			SampleClass entity = new SampleClass( entityId.getId(), entityId.getType(), "initial data" );

			// when
			SampleClass ver1 = auditReader.find( SampleClass.class, entityId, 1 );

			// then
			assertEquals( entity.getId(), ver1.getId() );
			assertEquals( entity.getSampleValue(), ver1.getSampleValue() );
			assertEquals( entity.getType().getType(), ver1.getType().getType() );
			assertEquals( entity.getType().getDescription(), ver1.getType().getDescription() );

			// given
			entity.setSampleValue( "modified data" );
			entity.getType().setDescription( "modified description" );

			// when
			SampleClass ver2 = auditReader.find( SampleClass.class, entityId, 3 );

			// then
			assertEquals( entity.getId(), ver2.getId() );
			assertEquals( entity.getSampleValue(), ver2.getSampleValue() );
			assertEquals( entity.getType().getType(), ver2.getType().getType() );
			assertEquals( entity.getType().getDescription(), ver2.getType().getDescription() );
		} );
	}

	@Test
	public void testHistoryOfType(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			// given
			ClassType type = new ClassType( typeId, "initial description" );

			// when
			ClassType ver1 = auditReader.find( ClassType.class, typeId, 1 );

			// then
			assertEquals( type, ver1 );
			assertEquals( type.getDescription(), ver1.getDescription() );

			// given
			type.setDescription( "modified description" );

			// when
			ClassType ver2 = auditReader.find( ClassType.class, typeId, 2 );

			// then
			assertEquals( type, ver2 );
			assertEquals( type.getDescription(), ver2.getDescription() );
		} );
	}
}
