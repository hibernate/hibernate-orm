/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.loading.semantics;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.library.Book;
import org.hibernate.testing.orm.domain.library.Person;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Steve Ebersole
 */
@DomainModel(standardModels = StandardDomainModel.LIBRARY)
@SessionFactory
public abstract class Base {
	@BeforeEach
	void createTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var mistborn = new Book( 1, "Mistborn", "123" );
			var mistborn2 = new Book( 2, "The Well of Ascension", "456" );
			var mistborn3 = new Book( 3, "The Hero of Ages", "789" );

			var sanderson = new Person( 1, "Brandon Sanderson" );
			mistborn.addAuthor( sanderson );
			mistborn2.addAuthor( sanderson );
			mistborn3.addAuthor( sanderson );

			session.persist( mistborn );
			session.persist( mistborn2 );
			session.persist( mistborn3 );
			session.persist( sanderson );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}
}
