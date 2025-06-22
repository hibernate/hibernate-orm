/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.ids.embeddedid;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

@JiraKey(value = "HHH-13361")
public class NestedEmbeddedIdentifiersTest extends BaseEnversJPAFunctionalTestCase {

	private OwnerOfRelationCodeId id;

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class<?>[] { OwnerOfRelationCode.class, CompositeEntity.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1, test insert
		final OwnerOfRelationCode owner = doInJPA( this::entityManagerFactory, session -> {
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
		doInJPA( this::entityManagerFactory, session -> {
			OwnerOfRelationCode ownerEntity = session.find( OwnerOfRelationCode.class, id );
			ownerEntity.setDescription( "first description" );
		} );
	}

	@Test
	public void testRevisionCounts() {
		assertEquals( Arrays.asList( 1, 2 ), getAuditReader().getRevisions( OwnerOfRelationCode.class, id ) );
	}

	@Test
	public void testIdentifierAtRevision1() {
		final OwnerOfRelationCode rev1 = getAuditReader().find( OwnerOfRelationCode.class, id, 1 );
		assertEquals( rev1.getCodeObject().getSecondIdentifier(), "secondIdentifier" );
		assertEquals( rev1.getCodeObject().getCompositeEntity().getFirstCode(), "firstCode" );
		assertEquals( rev1.getCodeObject().getCompositeEntity().getSecondCode(), "secondCode" );
		assertNull( rev1.getDescription() );
	}

	@Test
	public void testIdentifierAtRevision2() {
		final OwnerOfRelationCode rev2 = getAuditReader().find( OwnerOfRelationCode.class, id, 2 );
		assertEquals( rev2.getCodeObject().getSecondIdentifier(), "secondIdentifier" );
		assertEquals( rev2.getCodeObject().getCompositeEntity().getFirstCode(), "firstCode" );
		assertEquals( rev2.getCodeObject().getCompositeEntity().getSecondCode(), "secondCode" );
		assertEquals( rev2.getDescription(), "first description" );
	}
}
