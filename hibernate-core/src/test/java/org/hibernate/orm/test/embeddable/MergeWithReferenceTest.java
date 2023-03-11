package org.hibernate.orm.test.embeddable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.assertj.core.api.Assertions;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		MergeWithReferenceTest.Foo.class,
		MergeWithReferenceTest.Bar.class
})
@SessionFactory
public class MergeWithReferenceTest {

	@Test
	public void testMergeReference(SessionFactoryScope scope) {
		Bar bar = new Bar( "unique3" );
		scope.inTransaction( session -> {
			session.persist( bar );
		} );
		scope.inTransaction( session -> {
			Foo merged = session.merge( new Foo( session.getReference( Bar.class, bar.getId() ) ) );
			Assertions.assertThat( merged.getBar().getKey() ).isEqualTo( bar.getKey() );
		} );
	}

	@Entity(name = "Foo")
	static class Foo {
		Foo(Bar bar) {
			this.bar = bar;
		}

		Foo() {
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
	static class Bar implements Serializable {
		Bar(String key) {
			this.key = key;
		}

		Bar() {
		}

		@Id
		@GeneratedValue
		long id;
		@Column(name = "nat_key", unique = true)
		String key;

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
	}
}
