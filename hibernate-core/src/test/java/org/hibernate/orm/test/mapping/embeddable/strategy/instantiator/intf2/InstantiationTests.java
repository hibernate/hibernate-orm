/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.embeddable.strategy.instantiator.intf2;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.NotImplementedYet;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A "baseline" test for {@link org.hibernate.orm.test.mapping.embeddable.strategy.instantiator.intf.InstantiationTests}.
 *
 * There we have try to map an interface as an embeddable.
 *
 * Here we try to map a class that only defines getters to mimic a typical interface.
 *
 * Otherwise, they are mapped identically.
 */
@DomainModel( annotatedClasses = { Person.class, Name.class } )
@SessionFactory
@FailureExpected( jiraKey = "HHH-14950" )
@JiraKey( "HHH-14950" )
public class InstantiationTests {

	// for some reason, these tests fail a local build even though they are marked @FailureExpected

	//	@Test
	public void modelTest(DomainModelScope scope) {
		scope.withHierarchy( Person.class, (personMapping) -> {
			final Property name = personMapping.getProperty( "name" );
			final Component nameMapping = (Component) name.getValue();
			assertThat( nameMapping.getPropertySpan() ).isEqualTo( 2 );

			final Property aliases = personMapping.getProperty( "aliases" );
			final Component aliasMapping = (Component) ( (Collection) aliases.getValue() ).getElement();
			assertThat( aliasMapping.getPropertySpan() ).isEqualTo( 2 );
		});
	}

}
