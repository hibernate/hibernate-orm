/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.engine.collection;

import org.hibernate.Transaction;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				Father.class,
				Mother.class,
				Son.class
		}
)
@SessionFactory
public class UnidirCollectionWithMultipleOwnerTest {

	@Test
	public void testUnidirCollectionWithMultipleOwner(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					Transaction tx = session.beginTransaction();
					try {
						Father father = new Father();
						Mother mother = new Mother();
						session.persist( father );
						//s.persist( mother );
						Son son = new Son();
						father.getOrderedSons().add( son );
						son.setFather( father );
						mother.getSons().add( son );
						son.setMother( mother );
						session.persist( mother );
						session.persist( father );
						tx.commit();

						session.clear();

						tx = session.beginTransaction();
						son = session.get( Son.class, son.getId() );
						session.remove( son );
						session.flush();
						father = session.get( Father.class, father.getId() );
						mother = session.get( Mother.class, mother.getId() );
						session.remove( father );
						session.remove( mother );
						tx.commit();
					}
					catch (Exception e) {
						if ( tx.isActive() ) {
							tx.rollback();
						}
						throw e;
					}
				}
		);

	}
}
