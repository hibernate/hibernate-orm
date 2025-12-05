/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.ids.embeddedid;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@JiraKey(value = "HHH-13361")
@EnversTest
@Jpa(annotatedClasses = {OwnerOfRelationCode.class, CompositeEntity.class})
public class NestedEmbeddedIdentifiersTest {

	private OwnerOfRelationCodeId id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1, test insert
		final OwnerOfRelationCode owner = scope.fromTransaction( session -> {
			CompositeEntity compositeEntity = new CompositeEntity();
			compositeEntity.setFirstCode( "firstCode" );
			compositeEntity.setSecondCode( "secondCode" );
			session.persist( compositeEntity );

			OwnerOfRelationCode ownerEntity = new OwnerOfRelationCode();
			ownerEntity.setCompositeEntity( compositeEntity );
			ownerEntity.setSecondIdentifier( "secondIdentifier" );

			session.persist( ownerEntity );
			return ownerEntity;
		} );

		this.id = owner.getCodeObject();

		// Revision 2, test update
		scope.inTransaction( session -> {
			OwnerOfRelationCode ownerEntity = session.find( OwnerOfRelationCode.class, id );
			ownerEntity.setDescription( "first description" );
		} );
	}

	@Test
	public void testRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( Arrays.asList( 1, 2 ),
					AuditReaderFactory.get( em ).getRevisions( OwnerOfRelationCode.class, id ) );
		} );
	}

	@Test
	public void testIdentifierAtRevision1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final OwnerOfRelationCode rev1 = AuditReaderFactory.get( em )
					.find( OwnerOfRelationCode.class, id, 1 );
			assertEquals( rev1.getCodeObject().getSecondIdentifier(), "secondIdentifier" );
			assertEquals( rev1.getCodeObject().getCompositeEntity().getFirstCode(), "firstCode" );
			assertEquals( rev1.getCodeObject().getCompositeEntity().getSecondCode(), "secondCode" );
			assertNull( rev1.getDescription() );
		} );
	}

	@Test
	public void testIdentifierAtRevision2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final OwnerOfRelationCode rev2 = AuditReaderFactory.get( em )
					.find( OwnerOfRelationCode.class, id, 2 );
			assertEquals( rev2.getCodeObject().getSecondIdentifier(), "secondIdentifier" );
			assertEquals( rev2.getCodeObject().getCompositeEntity().getFirstCode(), "firstCode" );
			assertEquals( rev2.getCodeObject().getCompositeEntity().getSecondCode(), "secondCode" );
			assertEquals( rev2.getDescription(), "first description" );
		} );
	}
}
