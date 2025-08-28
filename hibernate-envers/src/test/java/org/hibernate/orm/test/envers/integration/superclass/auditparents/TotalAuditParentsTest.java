/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.superclass.auditparents;

import java.util.Set;
import jakarta.persistence.EntityManager;

import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.StrIntTestEntity;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests mapping of baby entity which declares its parent as audited with {@link Audited#auditParents()} property.
 * Moreover, child class (mapped superclass of baby entity) declares grandparent entity as audited. In this case all
 * attributes of baby class shall be audited.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class TotalAuditParentsTest extends BaseEnversJPAFunctionalTestCase {
	private long babyCompleteId = 1L;
	private Integer siteCompleteId = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				MappedGrandparentEntity.class,
				MappedParentEntity.class,
				StrIntTestEntity.class,
				ChildCompleteEntity.class,
				BabyCompleteEntity.class
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		// Revision 1
		em.getTransaction().begin();
		StrIntTestEntity siteComplete = new StrIntTestEntity( "data 1", 1 );
		em.persist( siteComplete );
		em.persist(
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
		em.getTransaction().commit();
		siteCompleteId = siteComplete.getId();
	}

	@Test
	public void testCreatedAuditTable() {
		Set<String> expectedColumns = TestTools.makeSet(
				"baby",
				"child",
				"parent",
				"relation_id",
				"grandparent",
				"id"
		);
		Set<String> unexpectedColumns = TestTools.makeSet( "notAudited" );

		Table table = metadata().getEntityBinding(
				"org.hibernate.orm.test.envers.integration.superclass.auditparents.BabyCompleteEntity_AUD"
		).getTable();

		for ( String columnName : expectedColumns ) {
			// Check whether expected column exists.
			Assert.assertNotNull( table.getColumn( new Column( columnName ) ) );
		}
		for ( String columnName : unexpectedColumns ) {
			// Check whether unexpected column does not exist.
			Assert.assertNull( table.getColumn( new Column( columnName ) ) );
		}
	}

	@Test
	public void testCompleteAuditParents() {
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
		BabyCompleteEntity baby = getAuditReader().find( BabyCompleteEntity.class, babyCompleteId, 1 );
		Assert.assertEquals( expectedBaby, baby );
		Assert.assertEquals( expectedBaby.getRelation().getId(), baby.getRelation().getId() );
	}
}
