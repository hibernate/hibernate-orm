/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;



import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the "treat" keyword in HQL.
 *
 * @author Etienne Miret
 * @author Steve Ebersole
 *
 * @see org.hibernate.orm.test.jpa.ql.TreatKeywordTest
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(xmlMappings = "org/hibernate/orm/test/hql/Animal.hbm.xml")
@SessionFactory
public class TreatKeywordTest {
	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@JiraKey( value = "HHH-9342" )
	public void memberOfTreatTest(SessionFactoryScope factoryScope) {
		// prepare test data
		factoryScope.inTransaction( (s) -> {
			var owner = new Human();
			s.persist( owner );

			var wildDog = new Dog();
			s.persist( wildDog );

			var petDog = new Dog();
			petDog.setOwner( owner );
			s.persist( petDog );

			var petCat = new Cat();
			petCat.setOwner( owner );
			s.persist( petCat );
		} );

		factoryScope.inTransaction( (s) -> {
			var hql = """
					select pet
					from Animal pet, Animal owner
					where pet member of treat (owner as Human).pets
					""";
			var results = s.createQuery( hql ).list();
			assertEquals( 2, results.size() );
		} );
	}
}
