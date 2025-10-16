/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany.detached;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.onetomany.detached.ListRefCollEntity;

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
@EnversTest
@Jpa(annotatedClasses = {ListRefCollEntity.class, StrTestEntity.class})
public class DetachedTest {
	private Integer parentId = null;
	private Integer childId = null;

	@BeforeClassTemplate
	@JiraKey(value = "HHH-7543")
	public void testUpdatingDetachedEntityWithRelation(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			ListRefCollEntity parent = new ListRefCollEntity( 1, "initial data" );
			StrTestEntity child = new StrTestEntity( "data" );
			em.persist( child );
			parent.setCollection( Arrays.asList( child ) );
			em.persist( parent );

			parentId = parent.getId();
			childId = child.getId();
		} );

		// Revision 2 - updating detached entity
		scope.inTransaction( em -> {
			ListRefCollEntity parent = new ListRefCollEntity( 1, "modified data" );
			parent.setId( parentId );
			parent.setCollection( Arrays.asList( new StrTestEntity( "data", childId ) ) );
			em.merge( parent );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals(
					Arrays.asList( 1, 2 ),
					auditReader.getRevisions( ListRefCollEntity.class, parentId )
			);
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( StrTestEntity.class, childId ) );
		} );
	}

	@Test
	public void testHistoryOfParent(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			ListRefCollEntity parent = new ListRefCollEntity( parentId, "initial data" );
			parent.setCollection( Arrays.asList( new StrTestEntity( "data", childId ) ) );

			ListRefCollEntity ver1 = auditReader.find( ListRefCollEntity.class, parentId, 1 );

			assertEquals( parent, ver1 );
			assertEquals( parent.getCollection(), ver1.getCollection() );

			parent.setData( "modified data" );

			ListRefCollEntity ver2 = auditReader.find( ListRefCollEntity.class, parentId, 2 );

			assertEquals( parent, ver2 );
			assertEquals( parent.getCollection(), ver2.getCollection() );
		} );
	}
}
