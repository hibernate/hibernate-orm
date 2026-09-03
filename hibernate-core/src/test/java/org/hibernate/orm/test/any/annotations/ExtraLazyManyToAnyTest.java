/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.annotations;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.annotations.AnyDiscriminatorImplicitValues;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.AttributeBinderType;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.binder.AttributeBinder;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SessionFactory( generateStatistics = true )
@DomainModel(annotatedClasses = {ExtraLazyManyToAnyTest.Parent.class,
		ExtraLazyManyToAnyTest.Child1.class, ExtraLazyManyToAnyTest.Child2.class})
@Jira("https://hibernate.atlassian.net/browse/HHH-20838")
class ExtraLazyManyToAnyTest {
	@Test void test(SessionFactoryScope scope) {
		final SQLStatementInspector sqlCollector = scope.getCollectingStatementInspector();
		final Long id = scope.fromTransaction( s -> {
			Parent parent = new Parent();
			Child1 child1 = new Child1( "c1" );
			Child2 child2 = new Child2( "c2" );
			parent.children = Set.of( child1, child2 );
			parent.children1 = Set.of( new Child1( "c3" ) );
			parent.children2 = Map.of( "e1", new Child2( "c4" ) );
			s.persist( parent );
			return parent.id;
		} );

		scope.inTransaction( s -> {
			sqlCollector.clear();
			Parent parent = s.find( Parent.class, id );
			sqlCollector.assertExecutedCount( 1 );
			assertFalse( Hibernate.isInitialized( parent.children ) );
			assertFalse( Hibernate.isInitialized( parent.children1 ) );
			assertFalse( Hibernate.isInitialized( parent.children2 ) );
			Hibernate.initialize( parent.children );
			sqlCollector.assertExecutedCount( 2 );

			for ( Object child : parent.children ) {
				assertFalse( Hibernate.isInitialized( child ) );
			}
			Hibernate.initialize( parent.children1 );
			sqlCollector.assertExecutedCount( 3 );

			for ( Child1 child : parent.children1 ) {
				assertFalse( Hibernate.isInitialized( child ) );
			}
			Hibernate.initialize( parent.children2 );
			sqlCollector.assertExecutedCount( 4 );

			for ( Child2 child : parent.children2.values() ) {
				assertFalse( Hibernate.isInitialized( child ) );
			}
		});
	}
	@Entity
	@Table(name = "PARENT")
	static class Parent {
		@Id @GeneratedValue
		Long id;

		@ExtraLazy
		@ManyToAny(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		@JoinTable(name = "PARENT_CHILD", joinColumns = @JoinColumn(name = "PARENT_ID"))
		@AnyKeyJavaClass( Long.class )
		@Column(name = "CHILD_TYPE")
		@AnyDiscriminatorImplicitValues
		Set<Object> children;

		@ExtraLazy
		@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		@JoinTable(name = "PARENT_CHILD1", joinColumns = @JoinColumn(name = "PARENT_ID"))
		Set<Child1> children1;

		@ExtraLazy
		@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		@JoinTable(name = "PARENT_CHILD2", joinColumns = @JoinColumn(name = "PARENT_ID"))
		Map<String, Child2> children2;
	}
	@Entity
	@Table(name = "CHILD_1")
	static class Child1 {
		@Id @GeneratedValue
		Long id;
		String name;

		public Child1() {
		}

		public Child1(String name) {
			this.name = name;
		}
	}
	@Entity
	@Table(name = "CHILD_2")
	static class Child2 {
		@Id @GeneratedValue
		Long id;
		String name;

		public Child2() {
		}

		public Child2(String name) {
			this.name = name;
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
	@AttributeBinderType(binder = ExtraLazyBinder.class)
	public @interface ExtraLazy {

	}

	public static class ExtraLazyBinder implements AttributeBinder<ExtraLazy> {
		@Override
		public void bind(
				ExtraLazy annotation,
				MetadataBuildingContext buildingContext,
				PersistentClass persistentClass,
				Property property) {
			final Value value = property.getValue();
			if ( value instanceof Collection collection ) {
				if ( collection.getElement() instanceof ToOne toOne ) {
					toOne.setLazy( true );
					if ( toOne.getFetchMode() == FetchMode.JOIN || toOne.getFetchMode() == null ) {
						toOne.setFetchMode( FetchMode.SELECT );
					}
				}
				else if ( collection.getElement() instanceof Any any ) {
					any.setLazy( true );
					if ( any.getFetchMode() == FetchMode.JOIN || any.getFetchMode() == null ) {
						any.setFetchMode( FetchMode.SELECT );
					}
				}
			}
		}
	}
}
