/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.embeddable.strategy.instantiator.intf;

import java.lang.reflect.Method;

import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.NotImplementedYet;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = { Person.class, NameImpl.class } )
@SessionFactory
@NotImplementedYet( strict = false )
public class InstantiationTests {
	@Test
	public void modelTest(DomainModelScope scope) {
		scope.withHierarchy( Person.class, (personMapping) -> {
			final Property name = personMapping.getProperty( "name" );
			final Component nameMapping = (Component) name.getValue();
			assertThat( nameMapping.getPropertySpan() ).isEqualTo( 2 );
		});
	}

	@Test
	public void basicTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Person mick = new Person( 1, new NameImpl( "Mick", "Jagger" ) );
			session.persist( mick );

			final Person john = new Person( 2, new NameImpl( "John", "Doe" ) );
			john.addAlias( new NameImpl( "Jon", "Doe" ) );
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
