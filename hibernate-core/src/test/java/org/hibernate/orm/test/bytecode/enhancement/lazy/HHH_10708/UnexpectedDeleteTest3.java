/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.HHH_10708;

import jakarta.persistence.CollectionTable;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@JiraKey("HHH-10708")
@DomainModel(
		annotatedClasses = {
				UnexpectedDeleteTest3.Parent.class, UnexpectedDeleteTest3.Child.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class UnexpectedDeleteTest3 {

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Child child = new Child();
			child.setId( 2L );
			s.persist( child );

			Parent parent = new Parent();
			parent.setId( 1L );
			parent.setNames( Collections.singleton( "name" ) );
			parent.addChild( child );

			s.persist( parent );
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Parent parent = s.get( Parent.class, 1L );

			Child child = new Child();
			child.setId( 1L );
			s.persist( child );
			parent.addChild( child );

			// We need to leave at least one attribute unfetchd
			//parent.getNames().size();
			s.persist( parent );
		} );

		scope.inTransaction( s -> {
			Parent application = s.get( Parent.class, 1L );
			assertEquals( 2, application.getChildren().size(), "Loaded Children collection has unexpected size" );
		} );
	}

	// --- //

	@Entity
	@Table( name = "CHILD" )
	static class Child {

		Long id;

		@Id
		@Column( name = "id", unique = true, nullable = false )
		Long getId() {
			return id;
		}

		void setId(Long id) {
			this.id = id;
		}
	}

	@Entity
	@Table( name = "PARENT" )
	static class Parent {

		Long id;
		Set<String> names;
		Set<Child> children;

		@Id
		@Column( name = "id", unique = true, nullable = false )
		Long getId() {
			return id;
		}

		void setId(Long id) {
			this.id = id;
		}

		@CollectionTable(name = "NAMES")
		@ElementCollection
		Set<String> getNames() {
			return Collections.unmodifiableSet( names );
		}

		void setNames(Set<String> secrets) {
			this.names = secrets;
		}

		@ManyToMany( fetch = FetchType.LAZY, targetEntity = Child.class )
		Set<Child> getChildren() {
			return Collections.unmodifiableSet( children );
		}

		void addChild(Child child) {
			if (children == null) {
				children = new HashSet<>();
			}
			children.add( child );
		}
	}
}
