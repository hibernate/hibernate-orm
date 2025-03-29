/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytomany;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

/**
 * @author Jan Schatteman
 */
@RequiresDialect( value = H2Dialect.class )
@DomainModel(
		annotatedClasses = {
				Cat.class, Woman.class, Man.class
		}
)
@SessionFactory
public class UnmodifiableCollectionMergeTest {

	@Test
	public void testMergeUnmodifiableCollection(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					CatPk catPk = new CatPk();
					catPk.setName( "Minou" );
					catPk.setThoroughbred( "Persian" );
					Cat cat = new Cat();
					cat.setId( catPk );

					WomanPk womanPk = new WomanPk();
					womanPk.setFirstName( "Emma" );
					womanPk.setLastName( "Peel" );
					Woman woman = new Woman();
					woman.setId( womanPk );

					Set<Woman> women = new HashSet<>();
					women.add( woman );

					cat.setHumanContacts( Collections.unmodifiableSet( women ) );
					Set<Cat> cats = new HashSet<>();
					cats.add( cat );
					woman.setCats( Collections.unmodifiableSet(cats) );

					session.persist( cat );
					session.persist( woman );

					session.flush();
					session.merge( woman );
				}
		);
	}

}
