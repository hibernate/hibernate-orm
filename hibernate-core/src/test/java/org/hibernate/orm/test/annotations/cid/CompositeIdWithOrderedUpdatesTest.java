/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cid;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@DomainModel(
		annotatedClasses = {
				CompositeIdWithOrderedUpdatesTest.ModelWithSelfChildren.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.ORDER_UPDATES, value = "true")
		}
)
@SessionFactory
@JiraKey("HHH-16725")
public class CompositeIdWithOrderedUpdatesTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSuccessfulPersist(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					ModelWithSelfChildren m = new ModelWithSelfChildren();
					m.setStringProperty( "a" );

					session.persist( m );

					ModelWithSelfChildren m2 = new ModelWithSelfChildren();
					m2.setStringProperty( "b" );

					session.persist( m2 );
				}
		);

		scope.inTransaction(
				session -> {
					assertThat(
							session.createQuery( "from ModelWithSelfChildren", ModelWithSelfChildren.class )
									.getResultList()
					).hasSize( 2 );
				}
		);
	}

	@Entity(name = "ModelWithSelfChildren")
	@IdClass(ModelWithSelfChildrenId.class)
	public static class ModelWithSelfChildren {

		@Id
		private String stringProperty;
		@Id
		private int integerProperty;

		@ManyToOne
		private ModelWithSelfChildren parent;

		@OneToMany(mappedBy = "parent")
		private List<ModelWithSelfChildren> children = new ArrayList<>();

		public String getStringProperty() {
			return stringProperty;
		}

		public void setStringProperty(String stringProperty) {
			this.stringProperty = stringProperty;
		}

		public int getIntegerProperty() {
			return integerProperty;
		}

		public void setIntegerProperty(int integerProperty) {
			this.integerProperty = integerProperty;
		}

		public ModelWithSelfChildren getParent() {
			return parent;
		}

		public void setParent(ModelWithSelfChildren parent) {
			this.parent = parent;
		}

		public List<ModelWithSelfChildren> getChildren() {
			return children;
		}

		public void setChildren(List<ModelWithSelfChildren> children) {
			this.children = children;
		}
	}

	public static class ModelWithSelfChildrenId implements Serializable {

		private String stringProperty;
		private int integerProperty;


		public String getStringProperty() {
			return stringProperty;
		}

		public void setStringProperty(String stringProperty) {
			this.stringProperty = stringProperty;
		}

		public int getIntegerProperty() {
			return integerProperty;
		}

		public void setIntegerProperty(int integerProperty) {
			this.integerProperty = integerProperty;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			ModelWithSelfChildrenId that = (ModelWithSelfChildrenId) o;
			return integerProperty == that.integerProperty && Objects.equals( stringProperty, that.stringProperty );
		}

		@Override
		public int hashCode() {
			return Objects.hash( stringProperty, integerProperty );
		}

		@Override
		public String toString() {
			return "Id{" +
					"string='" + stringProperty + '\'' +
					", integer=" + integerProperty +
					'}';
		}
	}


}
