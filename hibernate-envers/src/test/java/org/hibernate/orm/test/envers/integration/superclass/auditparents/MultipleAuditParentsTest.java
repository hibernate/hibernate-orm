/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.superclass.auditparents;

import java.util.Set;
import jakarta.persistence.EntityManager;
import jakarta.persistence.MappedSuperclass;

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
 * Tests mapping of child entity that declares all of its ancestors as audited with {@link Audited#auditParents()} property.
 * All superclasses are marked with {@link MappedSuperclass} annotation but not {@link Audited}.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class MultipleAuditParentsTest extends BaseEnversJPAFunctionalTestCase {
	private long childMultipleId = 1L;
	private Integer siteMultipleId = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				MappedGrandparentEntity.class,
				MappedParentEntity.class,
				ChildMultipleParentsEntity.class,
				StrIntTestEntity.class
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		// Revision 1
		em.getTransaction().begin();
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
		em.getTransaction().commit();
		siteMultipleId = siteMultiple.getId();
	}

	@Test
	public void testCreatedAuditTable() {
		Set<String> expectedColumns = TestTools.makeSet( "child", "parent", "relation_id", "grandparent", "id" );
		Set<String> unexpectedColumns = TestTools.makeSet( "notAudited" );

		Table table = metadata().getEntityBinding(
				"org.hibernate.orm.test.envers.integration.superclass.auditparents.ChildMultipleParentsEntity_AUD"
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
	public void testMultipleAuditParents() {
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
		ChildMultipleParentsEntity child = getAuditReader().find(
				ChildMultipleParentsEntity.class,
				childMultipleId,
				1
		);
		Assert.assertEquals( expectedMultipleChild, child );
		Assert.assertEquals( expectedMultipleChild.getRelation().getId(), child.getRelation().getId() );
	}
}
