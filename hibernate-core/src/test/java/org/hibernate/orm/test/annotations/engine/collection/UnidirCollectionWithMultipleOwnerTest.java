/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.engine.collection;


import org.hibernate.Transaction;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Bernard
 */
public class UnidirCollectionWithMultipleOwnerTest extends SessionFactoryBasedFunctionalTest {

	@Test
	public void testUnidirCollectionWithMultipleOwner() {
		inSession(
				session -> {
					Father father = new Father();
					Mother mother = new Mother();
					Son son = new Son();

					try {
						session.save( father );
						//s.save( mother );
						father.getOrderedSons().add( son );
						son.setFather( father );
						mother.getSons().add( son );
						son.setMother( mother );

						Transaction tx = session.beginTransaction();
						session.save( mother );
						session.save( father );
						tx.commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}

					session.clear();
					try {
						Transaction tx = session.beginTransaction();
						son = session.get( Son.class, son.getId() );
						session.delete( son );
						session.flush();
						father = session.get( Father.class, father.getId() );
						mother = session.get( Mother.class, mother.getId() );
						session.delete( father );
						session.delete( mother );
						tx.commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}

				}
		);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Father.class,
				Mother.class,
				Son.class
		};
	}
}
