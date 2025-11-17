/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.superclass.auditparents;

import java.util.Set;

import org.hibernate.envers.Audited;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests mapping of child entity which parent declares one of its ancestors as audited with {@link Audited#auditParents()}
 * property. Child entity may mark explicitly its parent as audited or not.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@EnversTest
@DomainModel(annotatedClasses = {
		MappedGrandparentEntity.class,
		TransitiveParentEntity.class,
		ImplicitTransitiveChildEntity.class,
		ExplicitTransitiveChildEntity.class
})
@SessionFactory
public class TransitiveAuditParentsTest {
	private long childImpTransId = 1L;
	private long childExpTransId = 2L;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			em.persist(
					new ImplicitTransitiveChildEntity(
							childImpTransId,
							"grandparent 1",
							"notAudited 1",
							"parent 1",
							"child 1"
					)
			);
		} );

		// Revision 2
		scope.inTransaction( em -> {
			em.persist(
					new ExplicitTransitiveChildEntity(
							childExpTransId,
							"grandparent 2",
							"notAudited 2",
							"parent 2",
							"child 2"
					)
			);
		} );
	}

	@Test
	public void testCreatedAuditTables(DomainModelScope scope) {
		final var domainModel = scope.getDomainModel();
		final var explicitTransChildTable = domainModel.getEntityBinding(
				"org.hibernate.orm.test.envers.integration.superclass.auditparents.ExplicitTransitiveChildEntity_AUD"
		).getTable();
		checkTableColumns(
				Set.of( "child", "parent", "grandparent", "id" ),
				Set.of( "notAudited" ),
				explicitTransChildTable
		);

		final var implicitTransChildTable = domainModel.getEntityBinding(
				"org.hibernate.orm.test.envers.integration.superclass.auditparents.ImplicitTransitiveChildEntity_AUD"
		).getTable();
		checkTableColumns(
				Set.of( "child", "parent", "grandparent", "id" ),
				Set.of( "notAudited" ),
				implicitTransChildTable
		);
	}

	private void checkTableColumns(Set<String> expectedColumns, Set<String> unexpectedColumns, Table table) {
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
	public void testImplicitTransitiveAuditParents(SessionFactoryScope scope) {
		scope.inSession( em -> {
			// expectedChild.notAudited shall be null, because it is not audited.
			ImplicitTransitiveChildEntity expectedChild = new ImplicitTransitiveChildEntity(
					childImpTransId,
					"grandparent 1",
					null,
					"parent 1",
					"child 1"
			);
			ImplicitTransitiveChildEntity child = AuditReaderFactory.get( em ).find(
					ImplicitTransitiveChildEntity.class,
					childImpTransId,
					1
			);
			assertEquals( expectedChild, child );
		} );
	}

	@Test
	public void testExplicitTransitiveAuditParents(SessionFactoryScope scope) {
		scope.inSession( em -> {
			// expectedChild.notAudited shall be null, because it is not audited.
			ExplicitTransitiveChildEntity expectedChild = new ExplicitTransitiveChildEntity(
					childExpTransId,
					"grandparent 2",
					null,
					"parent 2",
					"child 2"
			);
			ExplicitTransitiveChildEntity child = AuditReaderFactory.get( em ).find(
					ExplicitTransitiveChildEntity.class,
					childExpTransId,
					2
			);
			assertEquals( expectedChild, child );
		} );
	}
}
