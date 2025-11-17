/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.superclass.auditparents;

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

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests mapping of child entity that declares all of its ancestors as audited with {@link Audited#auditParents()} property.
 * All superclasses are marked with {@link MappedSuperclass} annotation but not {@link Audited}.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@EnversTest
@DomainModel(annotatedClasses = {
		MappedGrandparentEntity.class,
		MappedParentEntity.class,
		ChildMultipleParentsEntity.class,
		StrIntTestEntity.class
})
@SessionFactory
public class MultipleAuditParentsTest {
	private long childMultipleId = 1L;
	private Integer siteMultipleId = null;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			StrIntTestEntity siteMultiple = new StrIntTestEntity( "data 1", 1 );
			em.persist( siteMultiple );
			em.persist(
					new ChildMultipleParentsEntity(
							childMultipleId,
							"grandparent 1",
							"notAudited 1",
							"parent 1",
							"child 1",
							siteMultiple
					)
			);
			siteMultipleId = siteMultiple.getId();
		} );
	}

	@Test
	public void testCreatedAuditTable(DomainModelScope scope) {
		final var expectedColumns = Set.of( "child", "parent", "relation_id", "grandparent", "id" );
		final var unexpectedColumns = Set.of( "notAudited" );

		final var table = scope.getDomainModel().getEntityBinding(
				"org.hibernate.orm.test.envers.integration.superclass.auditparents.ChildMultipleParentsEntity_AUD"
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
	public void testMultipleAuditParents(SessionFactoryScope scope) {
		scope.inSession( em -> {
			// expectedMultipleChild.notAudited shall be null, because it is not audited.
			ChildMultipleParentsEntity expectedMultipleChild = new ChildMultipleParentsEntity(
					childMultipleId,
					"grandparent 1",
					null,
					"parent 1",
					"child 1",
					new StrIntTestEntity(
							"data 1",
							1,
							siteMultipleId
					)
			);
			ChildMultipleParentsEntity child = AuditReaderFactory.get( em ).find(
					ChildMultipleParentsEntity.class,
					childMultipleId,
					1
			);
			assertEquals( expectedMultipleChild, child );
			assertEquals( expectedMultipleChild.getRelation().getId(), child.getRelation().getId() );
		} );
	}
}
