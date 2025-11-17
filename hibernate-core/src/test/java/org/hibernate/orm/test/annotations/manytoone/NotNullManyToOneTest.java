/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytoone;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ExpectedException;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.ValidationSettings.JAKARTA_VALIDATION_MODE;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-13959")
@RequiresDialectFeature(feature= DialectFeatureChecks.SupportsIdentityColumns.class)
@ServiceRegistry(settings = @Setting(name = JAKARTA_VALIDATION_MODE, value = "auto" ) )
@DomainModel(annotatedClasses = {
		NotNullManyToOneTest.Parent.class,
		NotNullManyToOneTest.Child.class
})
@SessionFactory
public class NotNullManyToOneTest {
	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testSave(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(session -> {
			Parent parent = new Parent( new Child() );
			session.persist( parent );
		} );
	}

	@Test
	@ExpectedException( jakarta.validation.ConstraintViolationException.class )
	public void testSaveChildWithoutParent(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(session -> {
			Child child = new Child();
			session.persist( child );
		} );
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@NotNull
		@ManyToOne
		private Parent parent;

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@ManyToOne(cascade = CascadeType.ALL)
		private Child child;

		public Parent(Child child) {
			this.child = child;
			this.child.setParent( this );
		}

		public Child getChild() {
			return child;
		}
	}
}
