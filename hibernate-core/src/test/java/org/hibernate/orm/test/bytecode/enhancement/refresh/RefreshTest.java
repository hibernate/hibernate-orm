package org.hibernate.orm.test.bytecode.enhancement.refresh;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
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

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
			RefreshTest.RealmEntity.class,
				RefreshTest.RealmAttributeEntity.class,
				RefreshTest.ComponentEntity.class,
		}
)
@SessionFactory
@BytecodeEnhanced
@JiraKey("HHH-16423")
public class RefreshTest {

	private static final String REALM_ID = "id";

	@AfterEach
	public void trearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					RealmEntity find = session.find( RealmEntity.class, "id" );
					if(find != null) {
						session.remove( find );
					}
				}
		);
	}

	@Test
	public void testRefresh(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					RealmEntity realm = new RealmEntity();
					realm.setId( REALM_ID );
					realm.setName( "realm" );
					session.persist( realm );
				}
		);

		scope.inTransaction(
				session -> {
					RealmEntity realm = session.find( RealmEntity.class, REALM_ID );

					session.refresh( realm );

					assertThat( realm.getComponents().size() ).isEqualTo( 0 );

					session.remove( realm );
				}
		);
	}

	@Test
	public void testRefresh2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					RealmEntity realm = new RealmEntity();
					realm.setId( "id" );
					realm.setName( "realm" );

					RealmAttributeEntity attribute1 = new RealmAttributeEntity();
					attribute1.setName( "a1" );
					RealmAttributeEntity attribute2 = new RealmAttributeEntity();
					attribute2.setName( "a2" );
					realm.addAttribute( attribute1 );
					realm.addAttribute( attribute2 );

					session.persist( attribute1 );
					session.persist( attribute2 );

					session.persist( realm );
				}
		);

		scope.inTransaction(
				session -> {
					RealmEntity realm = session.find( RealmEntity.class, REALM_ID );

					session.refresh( realm );

					assertThat( realm.getComponents().size() ).isEqualTo( 0 );
					assertThat( realm.getAttributes().size() ).isEqualTo( 2 );

					session.remove( realm );
				}
		);
	}

	@Test
	public void testRefresh3(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					RealmEntity realm = new RealmEntity();
					realm.setId( "id" );
					realm.setName( "realm" );

					ComponentEntity component = new ComponentEntity();
					component.setId( "id_comp" );
					realm.addComponent( component );

					ComponentEntity component2 = new ComponentEntity();
					component2.setId( "id_comp_2" );
					realm.addComponent( component2 );

					session.persist( realm );
				}
		);

		scope.inTransaction(
				session -> {
					RealmEntity realm = session.find( RealmEntity.class, REALM_ID );

					session.refresh( realm );

					assertThat( realm.getComponents().size() ).isEqualTo( 2 );
					assertThat( realm.getAttributes().size() ).isEqualTo( 0 );

					session.remove( realm );
				}
		);
	}

	@Test
	public void testRefresh4(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					RealmEntity realm = new RealmEntity();
					realm.setId( "id" );
					realm.setName( "realm" );

					ComponentEntity component = new ComponentEntity();
					component.setId( "id_comp" );
					realm.addComponent( component );

					ComponentEntity component2 = new ComponentEntity();
					component2.setId( "id_comp_2" );
					realm.addComponent( component2 );
					RealmAttributeEntity attribute1 = new RealmAttributeEntity();
					attribute1.setName( "a1" );
					RealmAttributeEntity attribute2 = new RealmAttributeEntity();
					attribute2.setName( "a2" );
					realm.addAttribute( attribute1 );
					realm.addAttribute( attribute2 );

					session.persist( attribute1 );
					session.persist( attribute2 );


					session.persist( realm );
				}
		);

		scope.inTransaction(
				session -> {
					RealmEntity realm = session.find( RealmEntity.class, REALM_ID );

					session.refresh( realm );

					assertThat( realm.getComponents().size() ).isEqualTo( 2 );
					assertThat( realm.getAttributes().size() ).isEqualTo( 2 );

					session.remove( realm );
				}
		);
	}

	@Table(name = "REALM")
	@Entity(name = "RealmEntity")
	public static class RealmEntity {
		@Id
		@Column(name = "ID", length = 36)
		protected String id;

		@Column(name = "NAME", unique = true)
		protected String name;

		@OneToMany(cascade = { CascadeType.REMOVE }, orphanRemoval = true, mappedBy = "realm", fetch = FetchType.EAGER)
		Collection<RealmAttributeEntity> attributes = new LinkedList<>();

		@OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.ALL }, orphanRemoval = true, mappedBy = "realm")
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
			if ( attributes == null ) {
				attributes = new LinkedList<>();
			}
			return attributes;
		}

		public void setAttributes(Collection<RealmAttributeEntity> attributes) {
			this.attributes = attributes;
		}

		public void addAttribute(RealmAttributeEntity attribute){
			attribute.setRealm( this );
			this.attributes.add( attribute );
		}

		public Set<ComponentEntity> getComponents() {
			if ( components == null ) {
				components = new HashSet<>();
			}
			return components;
		}

		public void setComponents(Set<ComponentEntity> components) {
			this.components = components;
		}

		public void addComponent(ComponentEntity component){
			this.components.add( component );
			component.setRealm( this );
		}
	}

	@Entity(name = "ComponentEntity")
	public static class ComponentEntity {

		@Id
		@Column(name = "ID", length = 36)
		protected String id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "REALM_ID")
		protected RealmEntity realm;

		@Column(name = "NAME")
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

	@Table(name = "REALM_ATTRIBUTE")
	@Entity(name = "RealmAttributeEntity")
	@IdClass(Key.class)
	public static class RealmAttributeEntity {

		@Id
		@ManyToOne(fetch = FetchType.LAZY)
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
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			Key key = (Key) o;

			if ( name != null ? !name.equals( key.name ) : key.name != null ) {
				return false;
			}
			if ( realm != null ?
					!realm.getId().equals( key.realm != null ? key.realm.getId() : null ) :
					key.realm != null ) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = realm != null ? realm.getId().hashCode() : 0;
			result = 31 * result + ( name != null ? name.hashCode() : 0 );
			return result;
		}
	}
}
