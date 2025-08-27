/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.merge;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.assertj.core.api.Assertions;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				MergeWithReferenceTest.Foo.class,
				MergeWithReferenceTest.Bar.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class MergeWithReferenceTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testMergeReference(SessionFactoryScope scope) {
		Bar bar = new Bar( "unique3" );
		scope.inTransaction( session -> {
			session.persist( bar );
		} );

		scope.inTransaction( session -> {
			Bar reference = session.getReference( Bar.class, bar.getId() );
			Foo merged = session.merge( new Foo( reference ) );
			Assertions.assertThat( merged.getBar().getKey() ).isEqualTo( bar.getKey() );
		} );
	}

	@Test
	public void testMergeReference2(SessionFactoryScope scope) {
		Bar bar = new Bar( "unique3" );
		scope.inTransaction( session -> {
			session.persist( bar );
		} );

		scope.inTransaction( session -> {
			Bar reference = session.getReference( Bar.class, bar.getId() );
			reference.setName( "Davide" );
			session.merge( reference );
		} );

		scope.inTransaction(
				session -> {
					Bar reference = session.getReference( Bar.class, bar.getId() );
					assertThat( reference.getName() ).isEqualTo( "Davide" );
				}
		);
	}

	@Entity(name = "Foo")
	public static class Foo {
		Foo(Bar bar) {
			this.bar = bar;
		}

		public Foo() {
		}

		@Id
		@GeneratedValue
		long id;

		@ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
		@Fetch(FetchMode.JOIN)
		@JoinColumn(name = "bar_key", referencedColumnName = "nat_key")
		Bar bar;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public Bar getBar() {
			return bar;
		}

		public void setBar(Bar bar) {
			this.bar = bar;
		}
	}

	@Entity(name = "Bar")
	public static class Bar {
		Bar(String key) {
			this.key = key;
		}

		public Bar() {
		}

		@Id
		@GeneratedValue
		long id;

		@Column(name = "nat_key", unique = true)
		String key;

		String name;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
