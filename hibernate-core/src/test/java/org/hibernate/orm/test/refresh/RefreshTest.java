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

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
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

@DomainModel(
		annotatedClasses = {
				RefreshTest.RealmEntity.class,
				RefreshTest.RealmAttributeEntity.class,
				RefreshTest.ComponentEntity.class,
				RefreshTest.SimpleEntity.class
		}
)
@SessionFactory
public class RefreshTest {


	@Test
	public void testIt(SessionFactoryScope scope)  {
		scope.inSession(
				session -> {
					session.getTransaction().begin();
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

						session.persist( realm );

						session.getTransaction().commit();
						session.getTransaction().begin();

						RealmEntity find = session.find( RealmEntity.class, "id" );
						session.refresh( realm );

						session.remove( find );
						session.getTransaction().commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}

	@Test
	public void testRefreshWithNullId(SessionFactoryScope scope) {
		Assertions.assertThrows(
				IllegalArgumentException.class,
				() -> {
					scope.inTransaction(
							session -> {
								SimpleEntity se = new SimpleEntity();
								se.setName( "a" );
								session.refresh( se );
							}
					);
				}
		);
	}

	@Entity(name= "SimpleEntity" )
	public static class SimpleEntity {
		@Id
		Long id;
		String name;

		public void setName(String name) {
			this.name = name;
		}
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
