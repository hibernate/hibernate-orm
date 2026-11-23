package org.hibernate.orm.test.bootstrap.binding.annotations.override;

import java.util.Set;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.Column;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class InheritedAttributeOverridingTest {
	private StandardServiceRegistry standardServiceRegistry;

	@BeforeEach
	public void buildServiceRegistry() {
		standardServiceRegistry = ServiceRegistryUtil.serviceRegistry();
	}

	@AfterEach
	public void releaseServiceRegistry() {
		if ( standardServiceRegistry != null ) {
			StandardServiceRegistryBuilder.destroy( standardServiceRegistry );
		}
	}

	@Test
	@JiraKey( value = "HHH-9485" )
	public void testInheritedAttributeOverridingMappedsuperclass() {
		Metadata metadata = new MetadataSources( standardServiceRegistry )
				.addAnnotatedClass( A.class )
				.addAnnotatedClass( B.class )
				.buildMetadata();

		( (MetadataImplementor) metadata ).orderColumns( false );
		( (MetadataImplementor) metadata ).validate();
	}

	@Test
	@JiraKey( value = "HHH-9485" )
	public void testInheritedAttributeOverridingEntity() {
		Metadata metadata = new MetadataSources( standardServiceRegistry )
				.addAnnotatedClass( C.class )
				.addAnnotatedClass( D.class )
				.buildMetadata();

		( (MetadataImplementor) metadata ).orderColumns( false );
		( (MetadataImplementor) metadata ).validate();
	}

	@Test
	@JiraKey("HHH-13248")
	public void manyToManyKeepsDeclaredJoinTable() {
		final var metadata = new MetadataSources( standardServiceRegistry )
				.addAnnotatedClass( UserGroup.class )
				.buildMetadata();
		final var members = metadata.getCollectionBinding( UserGroup.class.getName() + ".members" );
		assertThat( members.getCollectionTable().getName() ).isEqualTo( "explicit_m2m" );
		assertThat( members.getKey().getColumns() ).extracting( Column::getName ).containsExactly( "owner_id" );
		assertThat( members.getElement().getColumns() ).extracting( Column::getName ).containsExactly( "member_id" );
	}

	@MappedSuperclass
	static class GroupMembership {
		@Id long id;

		@ManyToMany
		@JoinTable(name = "explicit_m2m", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "member_id"))
		Set<UserGroup> members;
	}

	@Entity(name = "UserGroup")
	@AssociationOverride(name = "members", joinColumns = @JoinColumn(name = "overridden_id"))
	static class UserGroup extends GroupMembership {}

	@Test
	@JiraKey("HHH-13248")
	public void oneToManyColumnOverrideReplacesDeclaredJoinTable() {
		final var metadata = new MetadataSources( standardServiceRegistry )
				.addAnnotatedClass( Team.class )
				.addAnnotatedClass( Player.class )
				.buildMetadata();
		final var players = metadata.getCollectionBinding( Team.class.getName() + ".players" );
		// the join columns in the override select a foreign key mapping, held by the target entity
		assertThat( players.isOneToMany() ).isTrue();
		assertThat( players.getCollectionTable() )
				.isSameAs( metadata.getEntityBinding( Player.class.getName() ).getTable() )
				.isNotSameAs( metadata.getEntityBinding( Team.class.getName() ).getTable() );
		assertThat( players.getKey().getColumns() ).extracting( Column::getName ).containsExactly( "team_id" );
		assertThat( metadata.collectTableMappings() ).extracting( org.hibernate.mapping.Table::getName )
				.doesNotContain( "explicit_o2m" );
	}

	@MappedSuperclass
	static class Squad {
		@Id long id;

		@OneToMany
		@JoinTable(name = "explicit_o2m")
		Set<Player> players;
	}

	@Entity(name = "Team")
	@AssociationOverride(name = "players", joinColumns = @JoinColumn(name = "team_id"))
	static class Team extends Squad {}

	@Entity(name = "Player")
	static class Player {
		@Id long id;
	}

	@MappedSuperclass
	public static class A {
		private Integer id;
		private String name;

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity( name = "B" )
	public static class B extends A {
		@Override
		public String getName() {
			return super.getName();
		}

		@Override
		public void setName(String name) {
			super.setName( name );
		}
	}


	@Entity( name = "C" )
	public static class C {
		private Integer id;
		private String name;

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity( name = "D" )
	public static class D extends C {
		@Override
		public String getName() {
			return super.getName();
		}

		@Override
		public void setName(String name) {
			super.setName( name );
		}
	}
}
