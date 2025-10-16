/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.superclass.auditparents;

import java.util.Set;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.entities.StrIntTestEntity;
import org.hibernate.mapping.Column;

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
 * Tests mapping of baby entity which declares its parent as audited with {@link Audited#auditParents()} property.
 * Moreover, child class (mapped superclass of baby entity) declares grandparent entity as audited. In this case all
 * attributes of baby class shall be audited.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@EnversTest
@DomainModel(annotatedClasses = {
		MappedGrandparentEntity.class,
		MappedParentEntity.class,
		StrIntTestEntity.class,
		ChildCompleteEntity.class,
		BabyCompleteEntity.class
})
@SessionFactory
public class TotalAuditParentsTest {
	private long babyCompleteId = 1L;
	private Integer siteCompleteId = null;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			StrIntTestEntity siteComplete = new StrIntTestEntity( "data 1", 1 );
			session.persist( siteComplete );
			session.persist(
					new BabyCompleteEntity(
							babyCompleteId,
							"grandparent 1",
							"notAudited 1",
							"parent 1",
							"child 1",
							siteComplete,
							"baby 1"
					)
			);
			session.flush();
			siteCompleteId = siteComplete.getId();
		} );
	}

	@Test
	public void testCreatedAuditTable(DomainModelScope scope) {
		final var expectedColumns = Set.of(
				"baby",
				"child",
				"parent",
				"relation_id",
				"grandparent",
				"id"
		);
		final var unexpectedColumns = Set.of( "notAudited" );

		final var table = scope.getDomainModel().getEntityBinding(
				"org.hibernate.orm.test.envers.integration.superclass.auditparents.BabyCompleteEntity_AUD"
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
	public void testCompleteAuditParents(SessionFactoryScope scope) {
		scope.inSession( em -> {
			// expectedBaby.notAudited shall be null, because it is not audited.
			BabyCompleteEntity expectedBaby = new BabyCompleteEntity(
					babyCompleteId,
					"grandparent 1",
					null,
					"parent 1",
					"child 1",
					new StrIntTestEntity( "data 1", 1, siteCompleteId ),
					"baby 1"
			);
			BabyCompleteEntity baby = AuditReaderFactory.get( em ).find( BabyCompleteEntity.class, babyCompleteId, 1 );
			assertEquals( expectedBaby, baby );
			assertEquals( expectedBaby.getRelation().getId(), baby.getRelation().getId() );
		} );
	}
}
