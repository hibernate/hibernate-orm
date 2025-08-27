/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;


@DomainModel(
		annotatedClasses = {
				CodedPairSetHolder.class,
				CodedPairHolder.class,
				Person.class,
				PersonPair.class
		}
)
@SessionFactory
public class CascadeToEmbeddedManyToOneTest {

	@AfterEach
	public void teaDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testPersistCascadeToSetOfEmbedded(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Set<PersonPair> setOfPairs = new HashSet<>();
					setOfPairs.add( new PersonPair( new Person( "PERSON NAME 1" ), new Person( "PERSON NAME 2" ) ) );
					session.persist( new CodedPairSetHolder( "CODE", setOfPairs ) );
					session.flush();
				}
		);
	}

	@Test
	public void testPersistCascadeToEmbedded(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					PersonPair personPair = new PersonPair(
							new Person( "PERSON NAME 1" ),
							new Person( "PERSON NAME 2" )
					);
					session.persist( new CodedPairHolder( "CODE", personPair ) );
					session.flush();
				}
		);
	}
}
