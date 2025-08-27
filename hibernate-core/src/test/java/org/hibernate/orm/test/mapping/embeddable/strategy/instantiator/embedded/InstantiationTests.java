/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable.strategy.instantiator.embedded;

import org.hibernate.metamodel.spi.EmbeddableInstantiator;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for custom {@link EmbeddableInstantiator} usage,
 * specified on the embedded
 */
@DomainModel( annotatedClasses = { Person.class, Name.class } )
@SessionFactory
public class InstantiationTests {
	@Test
	public void basicTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Person mick = new Person( 1, new Name( "Mick", "Jagger" ) );
			session.persist( mick );

			final Person john = new Person( 2, new Name( "John", "Doe" ) );
			john.addAlias( new Name( "Jon", "Doe" ) );
			session.persist( john );
		} );
		scope.inTransaction( (session) -> {
			final Person mick = session.createQuery( "from Person where id = 1", Person.class ).uniqueResult();
			assertThat( mick.getName().getFirstName() ).isEqualTo( "Mick" );
		} );
		scope.inTransaction( (session) -> {
			final Person john = session.createQuery( "from Person p join fetch p.aliases where p.id = 2", Person.class ).uniqueResult();
			assertThat( john.getName().getFirstName() ).isEqualTo( "John" );
			assertThat( john.getAliases() ).hasSize( 1 );
			final Name alias = john.getAliases().iterator().next();
			assertThat( alias.getFirstName() ).isEqualTo( "Jon" );
		} );
	}
}
