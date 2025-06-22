/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.refresh;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Jpa(
		annotatedClasses = {
				JPARefreshTest.RealmEntity.class,
				JPARefreshTest.RealmAttributeEntity.class,
				JPARefreshTest.ComponentEntity.class,
		}
)
public class JPARefreshTest {

	@Test
	public void testDelete(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					entityManager.getTransaction().begin();
					try {
						RealmEntity realm = new RealmEntity();
						realm.setId( "id" );
						realm.setName( "realm" );

						ComponentEntity c1 = new ComponentEntity();
						c1.setId( "c1" );
						c1.setRealm( realm );

						ComponentEntity c2 = new ComponentEntity();
						c2.setId( "c2" );
						c2.setRealm( realm );

						realm.setComponents( Set.of( c1, c2 ) );

						entityManager.persist( realm );

						entityManager.getTransaction().commit();
						entityManager.getTransaction().begin();

						RealmEntity find = entityManager.find( RealmEntity.class, "id" );
						entityManager.refresh( realm );

						entityManager.remove( find );
						entityManager.getTransaction().commit();
					}
					catch (Exception e) {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}

	@Table(name="REALM")
	@Entity(name = "RealmEntity")
	public static class RealmEntity {
		@Id
		@Column(name="ID", length = 36)
		protected String id;

		@Column(name="NAME", unique = true)
		protected String name;

		@OneToMany(cascade ={ CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "realm", fetch = FetchType.EAGER)
		Collection<RealmAttributeEntity> attributes = new LinkedList<>();

		@OneToMany(fetch = FetchType.LAZY, cascade ={CascadeType.ALL}, orphanRemoval = true, mappedBy = "realm")
		Set<ComponentEntity> components = new HashSet<>();

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Collection<RealmAttributeEntity> getAttributes() {
			if (attributes == null) {
				attributes = new LinkedList<>();
			}
			return attributes;
		}

		public void setAttributes(Collection<RealmAttributeEntity> attributes) {
			this.attributes = attributes;
		}

		public Set<ComponentEntity> getComponents() {
			if (components == null) {
				components = new HashSet<>();
			}
			return components;
		}

		public void setComponents(Set<ComponentEntity> components) {
			this.components = components;
		}
	}

	@Entity(name = "ComponentEntity")
	public static class ComponentEntity {

		@Id
		@Column(name="ID", length = 36)
		protected String id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "REALM_ID")
		protected RealmEntity realm;

		@Column(name="NAME")
		protected String name;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public RealmEntity getRealm() {
			return realm;
		}

		public void setRealm(RealmEntity realm) {
			this.realm = realm;
		}

	}

	@Table(name="REALM_ATTRIBUTE")
	@Entity(name = "RealmAttributeEntity")
	@IdClass(Key.class)
	public static class RealmAttributeEntity {

		@Id
		@ManyToOne(fetch= FetchType.LAZY, cascade = CascadeType.PERSIST)
		@JoinColumn(name = "REALM_ID")
		protected RealmEntity realm;

		@Id
		@Column(name = "NAME")
		protected String name;

		@Column(name = "VALUE_COLUMN")
		protected String value;

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

		public RealmEntity getRealm() {
			return realm;
		}

		public void setRealm(RealmEntity realm) {
			this.realm = realm;
		}

	}

	public static class Key implements Serializable {

		protected RealmEntity realm;

		protected String name;

		public Key() {
		}

		public Key(RealmEntity user, String name) {
			this.realm = user;
			this.name = name;
		}

		public RealmEntity getRealm() {
			return realm;
		}

		public String getName() {
			return name;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Key key = (Key) o;

			if (name != null ? !name.equals(key.name) : key.name != null) return false;
			if (realm != null ? !realm.getId().equals(key.realm != null ? key.realm.getId() : null) : key.realm != null) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = realm != null ? realm.getId().hashCode() : 0;
			result = 31 * result + (name != null ? name.hashCode() : 0);
			return result;
		}
	}
}
