/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.HHH_10708;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@JiraKey( "HHH-10708" )
@DomainModel(
		annotatedClasses = {
			UnexpectedDeleteTest2.Foo.class, UnexpectedDeleteTest2.Bar.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class UnexpectedDeleteTest2 {

	private Bar myBar;

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Bar bar = new Bar();
			Foo foo1 = new Foo();
			Foo foo2 = new Foo();
			s.persist( bar );
			s.persist( foo1 );
			s.persist( foo2 );

			bar.foos.add( foo1 );
			bar.foos.add( foo2 );

			myBar = bar;
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			assertThrows(IllegalArgumentException.class,
						() -> s.refresh( myBar ),
						"Given entity is not associated with the persistence context"
			);

			// The issue is that currently, for some unknown reason, foos are deleted on flush
		} );

		scope.inTransaction( s -> {
			Bar bar = s.get( Bar.class, myBar.id );
			assertFalse( bar.foos.isEmpty() );
		} );
	}

	// --- //

	@Entity(name = "Bar")
	@Table( name = "BAR" )
	static class Bar {

		@Id
		@GeneratedValue
		Long id;

		@ManyToMany( fetch = FetchType.LAZY, targetEntity = Foo.class )
		Set<Foo> foos = new HashSet<>();
	}

	@Entity(name = "Foo")
	@Table( name = "FOO" )
	static class Foo {

		@Id
		@GeneratedValue
		Long id;
	}
}
