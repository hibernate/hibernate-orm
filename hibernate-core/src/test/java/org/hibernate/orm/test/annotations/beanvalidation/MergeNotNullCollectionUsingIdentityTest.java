/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.beanvalidation;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;

import org.hibernate.cfg.ValidationSettings;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Ryan Emerson
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class, jiraKey = "HHH-9979")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNoColumnInsert.class, jiraKey = "HHH-9979")
@ServiceRegistry(
		settings = @Setting(name = ValidationSettings.JAKARTA_VALIDATION_MODE, value = "AUTO")
)
@DomainModel(annotatedClasses = {MergeNotNullCollectionUsingIdentityTest.Parent.class, MergeNotNullCollectionUsingIdentityTest.Child.class})
@SessionFactory
@JiraKey( value = "HHH-9979")
class MergeNotNullCollectionUsingIdentityTest {

	@Test
	@FailureExpected(jiraKey = "HHH-9979")
	void testOneToManyNotNullCollection(SessionFactoryScope scope) {
		Parent parent = new Parent();
		Child child = new Child();

		List<Child> children = new ArrayList<>();
		children.add( child );

		child.setParent( parent );
		parent.setChildren( children );

		Parent p = scope.fromTransaction( s -> s.merge( parent ) );

		scope.inTransaction( s -> s.remove( p ) );
	}

	@Test
	void testOneToManyNullCollection(SessionFactoryScope scope) {
		Parent parent = new Parent();
		Child child = new Child();
		child.setParent( parent );

		assertThatThrownBy( () -> scope.fromTransaction( s -> s.merge( parent ) ) )
				.isInstanceOf( ConstraintViolationException.class );
	}

	@Entity
	@Table(name = "PARENT")
	static class Parent {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
		@NotNull
		private List<Child> children;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<Child> getChildren() {
			return children;
		}

		public void setChildren(List<Child> children) {
			this.children = children;
		}
	}

	@Entity
	@Table(name = "CHILD")
	static class Child {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@ManyToOne
		private Parent parent;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}
}
