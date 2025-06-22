/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.orderupdates;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static jakarta.persistence.CascadeType.REMOVE;

@DomainModel(
		annotatedClasses = {
				OrderUpdatesTest.Parent.class,
				OrderUpdatesTest.Child.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.ORDER_UPDATES, value = "true")
		}
)
@JiraKey(value = "HHH-16368")
public class OrderUpdatesTest {

	@Test
	public void testIt(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent1 = new Parent();
					parent1 = session.merge( parent1 );

					Parent parent2 = new Parent();
					parent2 = session.merge( parent2 );

					session.flush();

					// create two children with the same name, so that they differ only in their parent
					// otherwise the key doesn't trigger the exception
					Child child1 = new Child();
					child1.setName( "name1" );
					child1.setValue( "value" );
					child1.setParent( parent1 );
					child1 = session.merge( child1 );
					parent1.addChild( child1 );

					Child child2 = new Child();
					child2.setName( "name1" );
					child2.setValue( "value" );
					child2.setParent( parent2 );
					child2 = session.merge( child2 );
					parent2.addChild( child2 );

					session.flush();

					child1.setValue( "new-value" );
					child2.setValue( "new-value" );

					session.flush();
				}
		);
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@OneToMany(mappedBy = "parent", cascade = { REMOVE }, fetch = FetchType.LAZY)
		private Collection<Child> children = new LinkedList<>();

		public Long getId() {
			return this.id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public void addChild(Child child) {
			children.add( child );
			child.setParent( this );
		}

		public void removeChild(Child child) {
			children.remove( child );
			child.setParent( null );
		}
	}

	@Entity(name = "Chil;d")
	@IdClass(Key.class)
	public static class Child {

		@Id
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "ID")
		private Parent parent;

		@Id
		protected String name;

		@Column(name = "VALUE_COLUMN")
		protected String value;

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !( o instanceof Child ) ) {
				return false;
			}
			return parent != null && parent.equals( ( (Child) o ).getParent() );
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}
	}

	public static class Key implements Serializable, Comparable<Key> {

		protected Parent parent;

		protected String name;

		public Key() {
		}

		public Key(Parent parent, String name) {
			this.parent = parent;
			this.name = name;
		}

		public Parent getParent() {
			return parent;
		}

		public String getName() {
			return name;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			Key key = (Key) o;

			if ( parent != null ?
					!parent.getId().equals( key.parent != null ? key.parent.getId() : null ) :
					key.parent != null ) {
				return false;
			}
			if ( name != null ? !name.equals( key.name != null ? key.name : null ) : key.name != null ) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = parent != null ? parent.getId().hashCode() : 0;
			result = 31 * result + ( name != null ? name.hashCode() : 0 );
			return result;
		}

		private final static Comparator<Key> COMPARATOR = Comparator.comparing( Key::getName )
				.thenComparing( c -> c.getParent() == null ? null : c.getParent().getId() );

		@Override
		public int compareTo(Key other) {
			return COMPARATOR.compare( this, other );
		}
	}
}
