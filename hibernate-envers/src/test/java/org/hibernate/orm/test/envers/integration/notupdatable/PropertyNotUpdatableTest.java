/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.notupdatable;

import java.util.Arrays;
import java.util.List;

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
@JiraKey(value = "HHH-5411")
@EnversTest
@Jpa(annotatedClasses = {PropertyNotUpdatableEntity.class},
		integrationSettings = @Setting(name = EnversSettings.STORE_DATA_AT_DELETE, value = "true"))
public class PropertyNotUpdatableTest {
	private Long id = null;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			PropertyNotUpdatableEntity entity = new PropertyNotUpdatableEntity(
					"data",
					"constant data 1",
					"constant data 2"
			);
			em.persist( entity );
			id = entity.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			PropertyNotUpdatableEntity entity = em.find( PropertyNotUpdatableEntity.class, id );
			entity.setData( "modified data" );
			entity.setConstantData1( null );
			em.merge( entity );
		} );

		// Revision 3
		scope.inTransaction( em -> {
			PropertyNotUpdatableEntity entity = em.find( PropertyNotUpdatableEntity.class, id );
			entity.setData( "another modified data" );
			entity.setConstantData2( "invalid data" );
			em.merge( entity );
		} );

		// Revision 4
		scope.inTransaction( em -> {
			PropertyNotUpdatableEntity entity = em.find( PropertyNotUpdatableEntity.class, id );
			em.refresh( entity );
			em.remove( entity );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3, 4 ), auditReader.getRevisions( PropertyNotUpdatableEntity.class, id ) );
		} );
	}

	@Test
	public void testHistoryOfId(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			PropertyNotUpdatableEntity ver1 = new PropertyNotUpdatableEntity(
					"data",
					"constant data 1",
					"constant data 2",
					id
			);
			assertEquals( ver1, auditReader.find( PropertyNotUpdatableEntity.class, id, 1 ) );

			PropertyNotUpdatableEntity ver2 = new PropertyNotUpdatableEntity(
					"modified data",
					"constant data 1",
					"constant data 2",
					id
			);
			assertEquals( ver2, auditReader.find( PropertyNotUpdatableEntity.class, id, 2 ) );

			PropertyNotUpdatableEntity ver3 = new PropertyNotUpdatableEntity(
					"another modified data",
					"constant data 1",
					"constant data 2",
					id
			);
			assertEquals( ver3, auditReader.find( PropertyNotUpdatableEntity.class, id, 3 ) );
		} );
	}

	@Test
	public void testDeleteState(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			PropertyNotUpdatableEntity delete = new PropertyNotUpdatableEntity(
					"another modified data",
					"constant data 1",
					"constant data 2",
					id
			);
			List<Object> results = auditReader.createQuery().forRevisionsOfEntity(
					PropertyNotUpdatableEntity.class,
					true,
					true
			).getResultList();
			assertEquals( delete, results.get( 3 ) );
		} );
	}
}
