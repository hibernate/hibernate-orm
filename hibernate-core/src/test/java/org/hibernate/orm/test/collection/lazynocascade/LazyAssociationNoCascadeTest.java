/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.lazynocascade;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vasily Kochnev
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/collection/lazynocascade/Parent.xml"
)
@SessionFactory
public class LazyAssociationNoCascadeTest {

	@Test
	public void testNoCascadeCache(SessionFactoryScope scope) {
		Parent parent = new Parent();

		BaseChild firstChild = new BaseChild();
		parent.getChildren().add( firstChild );

		Parent mergedParent = scope.fromSession(
				session -> {
					session.beginTransaction();
					try {
						session.persist( parent );
						session.getTransaction().commit();
						session.clear();

						Child secondChild = new Child();
						secondChild.setName( "SecondChildName" );
						parent.getChildren().add( secondChild );

						firstChild.setDependency( secondChild );

						session.beginTransaction();
						Parent merged = (Parent) session.merge( parent );
						session.getTransaction().commit();
						return merged;
					}
					catch (Exception exception) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw exception;
					}

				}
		);

		assertNotNull( mergedParent );
		assertEquals( mergedParent.getChildren().size(), 2 );
	}
}
