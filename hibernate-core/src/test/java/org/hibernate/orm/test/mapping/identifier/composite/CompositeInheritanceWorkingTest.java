/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier.composite;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This test works for some reason...
 */
@DomainModel(
		annotatedClasses = {
				CompositeInheritanceWorkingTest.TupAbstractEntity.class,
				CompositeInheritanceWorkingTest.DummyEntity.class,
				CompositeInheritanceWorkingTest.FooEntity.class, // And here the class is called FooEntity and this works for some reason
		}
)
@ServiceRegistry(
		settings = {
				// For your own convenience to see generated queries:
				@Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
				@Setting(name = AvailableSettings.FORMAT_SQL, value = "true"),
				// @Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
		}
)
@SessionFactory
@Jira("HHH-19076")
public class CompositeInheritanceWorkingTest {

	@Test
	void hhh19076WorkingTest(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			FooEntity e1 = new FooEntity("foo", "bar");
			em.persist(e1);

			CompositeIdClass key = e1.getCompositeId();
			FooEntity e2 = em.find( FooEntity.class, key);
			assertNotNull(e2);
		} );
	}

	@MappedSuperclass
	public static abstract class TupAbstractEntity {
		@Id
		private String oid = null;


		@SuppressWarnings("this-escape")
		protected TupAbstractEntity() {
		}

		protected TupAbstractEntity(String oid) {
			this.oid = oid;
		}

		public String getOid() {
			return oid;
		}

	}

	@Entity
	public static class DummyEntity extends TupAbstractEntity {
	}

	@Entity
	@IdClass(CompositeIdClass.class)
	public static class FooEntity extends TupAbstractEntity {

		@Id
		private String myId;

		protected FooEntity() {
			// for JPA
		}

		public FooEntity(String oid, String myId) {
			super(oid);
			this.myId = myId;
		}

		public String myId() {
			return myId;
		}

		public CompositeIdClass getCompositeId() {
			return new CompositeIdClass(getOid(), myId);
		}

	}

	public static class CompositeIdClass {

		private String oid;
		private String myId;

		public CompositeIdClass(String oid, String myId) {
			this.oid = oid;
			this.myId = myId;
		}

		public CompositeIdClass() {
		}

		public String oid() {
			return oid;
		}

		public String myId() {
			return myId;
		}

	}

}
