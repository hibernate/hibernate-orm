/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.naming;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.integration.naming.entities.Child;
import org.hibernate.orm.test.envers.integration.naming.entities.Parent;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-6738")
@EnversTest
@DomainModel(annotatedClasses = {Parent.class, Child.class})
@SessionFactory
public class EstonianTableAlias {
	private Long parentId = null;
	private Long childId = null;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			Parent parent = new Parent( "parent" );
			Child child = new Child( "child" );
			parent.getCollection().add( child );
			em.persist( child );
			em.persist( parent );
			parentId = parent.getId();
			childId = child.getId();
		} );
	}

	@Test
	public void testAuditChildTableAlias(SessionFactoryScope scope) {
		Parent parent = new Parent( "parent", parentId );
		Child child = new Child( "child", childId );

		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			Parent ver1 = auditReader.find( Parent.class, parentId, 1 );

			assertEquals( parent, ver1 );
			assertEquals( TestTools.makeSet( child ), ver1.getCollection() );
		} );
	}
}
