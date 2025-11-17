/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.superclass.auditparents;

import java.util.Set;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.mapping.Column;
import org.hibernate.orm.test.envers.entities.StrIntTestEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;

import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests mapping of child entity that declares one of its ancestors as audited with {@link Audited#auditParents()} property.
 * All superclasses are marked with {@link MappedSuperclass} annotation but not {@link Audited}.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@EnversTest
@DomainModel(annotatedClasses = {
		MappedGrandparentEntity.class,
		MappedParentEntity.class,
		ChildSingleParentEntity.class,
		StrIntTestEntity.class
})
@SessionFactory
public class SingleAuditParentsTest {
	private long childSingleId = 1L;
	private Integer siteSingleId = null;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			StrIntTestEntity siteSingle = new StrIntTestEntity( "data 1", 1 );
			em.persist( siteSingle );
			em.persist(
					new ChildSingleParentEntity(
							childSingleId,
							"grandparent 1",
							"notAudited 1",
							"parent 1",
							"child 1",
							siteSingle
					)
			);
			siteSingleId = siteSingle.getId();
		} );
	}

	@Test
	public void testCreatedAuditTable(DomainModelScope scope) {
		final var expectedColumns = Set.of( "child", "grandparent", "id" );
		final var unexpectedColumns = Set.of( "parent", "relation_id", "notAudited" );

		final var table = scope.getDomainModel().getEntityBinding(
				"org.hibernate.orm.test.envers.integration.superclass.auditparents.ChildSingleParentEntity_AUD"
		).getTable();

		for ( String columnName : expectedColumns ) {
			// Check whether expected column exists.
			assertNotNull( table.getColumn( new Column( columnName ) ) );
		}
		for ( String columnName : unexpectedColumns ) {
			// Check whether unexpected column does not exist.
			assertNull( table.getColumn( new Column( columnName ) ) );
		}
	}

	@Test
	public void testSingleAuditParent(SessionFactoryScope scope) {
		scope.inSession( em -> {
			// expectedSingleChild.parent, expectedSingleChild.relation and expectedSingleChild.notAudited shall be null, because they are not audited.
			ChildSingleParentEntity expectedSingleChild = new ChildSingleParentEntity(
					childSingleId,
					"grandparent 1",
					null,
					null,
					"child 1",
					null
			);
			ChildSingleParentEntity child = AuditReaderFactory.get( em ).find( ChildSingleParentEntity.class, childSingleId, 1 );
			assertEquals( expectedSingleChild, child );
			assertNull( child.getRelation() );
		} );
	}
}
