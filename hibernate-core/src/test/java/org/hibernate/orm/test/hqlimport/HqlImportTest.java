/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hqlimport;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@code <import>} (hql-import) elements in orm.xml are processed
 * and register class names for use in HQL polymorphic queries.
 */
@JiraKey("HHH-20716")
@DomainModel(xmlMappings = "org/hibernate/orm/test/hqlimport/Mappings.orm.xml")
@SessionFactory
public class HqlImportTest {

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void testPolymorphicQueryUsingImportedName(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Dog dog = new Dog();
			dog.setName( "Rex" );
			dog.setBreed( "Labrador" );
			session.persist( dog );

			Cat cat = new Cat();
			cat.setName( "Whiskers" );
			cat.setIndoor( true );
			session.persist( cat );
		} );

		scope.inTransaction( session -> {
			// "Animal" is not an entity — it's imported via <import class="Animal"/>
			// so it can be used as an HQL name for polymorphic queries
			List<Animal> animals = session.createQuery( "from Animal", Animal.class ).list();
			assertThat( animals ).hasSize( 2 );
			assertThat( animals ).extracting( Animal::getName ).containsExactlyInAnyOrder( "Rex", "Whiskers" );
		} );
	}
}
