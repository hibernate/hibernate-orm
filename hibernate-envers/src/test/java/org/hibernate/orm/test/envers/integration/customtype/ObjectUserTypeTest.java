/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.customtype;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-7870")
@EnversTest
@Jpa(
		annotatedClasses = {ObjectUserTypeEntity.class},
		integrationSettings = @Setting(name = EnversSettings.STORE_DATA_AT_DELETE, value = "true")
)
public class ObjectUserTypeTest {
	private int id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1 - add
		scope.inTransaction( em -> {
			ObjectUserTypeEntity entity = new ObjectUserTypeEntity( "builtInType1", "stringUserType1" );
			em.persist( entity );
			this.id = entity.getId();
		} );

		// Revision 2 - modify
		scope.inTransaction( em -> {
			ObjectUserTypeEntity entity = em.find( ObjectUserTypeEntity.class, this.id );
			entity.setUserType( 2 );
			em.merge( entity );
		} );

		// Revision 3 - remove
		scope.inTransaction( em -> {
			ObjectUserTypeEntity entity = em.find( ObjectUserTypeEntity.class, this.id );
			em.remove( entity );
		} );
	}

	@Test
	public void testRevisionCount(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals(
					Arrays.asList( 1, 2, 3 ),
					auditReader.getRevisions( ObjectUserTypeEntity.class, id )
			);
		} );
	}

	@Test
	public void testHistory(EntityManagerFactoryScope scope) {
		ObjectUserTypeEntity ver1 = new ObjectUserTypeEntity( id, "builtInType1", "stringUserType1" );
		ObjectUserTypeEntity ver2 = new ObjectUserTypeEntity( id, "builtInType1", 2 );

		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( ver1, auditReader.find( ObjectUserTypeEntity.class, id, 1 ) );
			assertEquals( ver2, auditReader.find( ObjectUserTypeEntity.class, id, 2 ) );
			assertEquals(
					ver2,
					auditReader.createQuery()
							.forRevisionsOfEntity( ObjectUserTypeEntity.class, true, true )
							.getResultList()
							.get( 2 )
			); // Checking delete state.
		} );
	}
}
