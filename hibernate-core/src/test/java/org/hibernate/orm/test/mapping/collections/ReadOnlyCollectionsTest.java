/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.collections;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		ReadOnlyCollectionsTest.CollectionsContainer.class,
		ReadOnlyCollectionsTest.TargetEntity.class,
		BasicEntity.class
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17468" )
public class ReadOnlyCollectionsTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new TargetEntity( 1L ) );
			session.persist( new TargetEntity( 2L ) );
			session.persist( new BasicEntity( 1, "be1" ) );
			session.persist( new BasicEntity( 2, "be2" ) );
			session.persist( new CollectionsContainer( 1L, "container_1" ) );
			session.persist( new CollectionsContainer( 2L, "container_2" ) );
		} );
		scope.inTransaction( session -> {
			session.createNativeMutationQuery( "insert into otm_join_table(container_id, basic_id) values (2,1)" ).executeUpdate();
			session.createNativeMutationQuery( "insert into otm_join_table(container_id, basic_id) values (2,2)" ).executeUpdate();
			session.createNativeMutationQuery( "insert into mtm_join_table(container_id, basic_id) values (2,1)" ).executeUpdate();
			session.createNativeMutationQuery( "insert into mtm_join_table(container_id, basic_id) values (2,2)" ).executeUpdate();
			session.createNativeMutationQuery( "insert into el_col_table(container_id, long_value) values (2,1)" ).executeUpdate();
			session.createNativeMutationQuery( "insert into el_col_table(container_id, long_value) values (2,2)" ).executeUpdate();
			session.createNativeMutationQuery( "insert into TargetEntity(id, container_id) values (98,2)" ).executeUpdate();
			session.createNativeMutationQuery( "insert into TargetEntity(id, container_id) values (99,2)" ).executeUpdate();
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from TargetEntity" ).executeUpdate();
			session.createMutationQuery( "delete from CollectionsContainer" ).executeUpdate();
			session.createMutationQuery( "delete from BasicEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testInsert(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CollectionsContainer container = session.find( CollectionsContainer.class, 1L );
			container.name = "container_1_updated";

			container.oneToMany.add( session.find( TargetEntity.class, 1L ) );
			container.oneToMany.add( session.find( TargetEntity.class, 2L ) );

			final BasicEntity be1 = session.find( BasicEntity.class, 1 );
			final BasicEntity be2 = session.find( BasicEntity.class, 2 );

			container.oneToManyJoinTable.add( be1 );
			container.oneToManyJoinTable.add( be2 );

			container.manyToMany.add( be1 );
			container.manyToMany.add( be2 );

			container.elementCollection.add( 1L );
			container.elementCollection.add( 2L );
		} );

		scope.inSession( session -> {
			final CollectionsContainer container = session.find( CollectionsContainer.class, 1L );
			// No elements should have been added to the collections
			assertThat( container.name ).isEqualTo( "container_1_updated" );
			assertThat( container.oneToMany ).hasSize( 0 );
			assertThat( container.oneToManyJoinTable ).hasSize( 0 );
			assertThat( container.manyToMany ).hasSize( 0 );
			assertThat( container.elementCollection ).hasSize( 0 );
		} );
	}

	@Test
	public void testDelete(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CollectionsContainer container = session.find( CollectionsContainer.class, 2L );
			assertThat( container.oneToMany ).hasSize( 2 );
			assertThat( container.oneToManyJoinTable ).hasSize( 2 );
			assertThat( container.manyToMany ).hasSize( 2 );
			assertThat( container.elementCollection ).hasSize( 2 );

			container.name = "container_2_updated";

			container.oneToMany.clear();
			container.oneToManyJoinTable.clear();
			container.manyToMany.clear();
			container.elementCollection.clear();
		} );

		scope.inTransaction( session -> {
			final CollectionsContainer container = session.find( CollectionsContainer.class, 2L );
			// No elements should have been removed from the collections
			assertThat( container.name ).isEqualTo( "container_2_updated" );
			assertThat( container.oneToMany ).hasSize( 2 );
			assertThat( container.oneToManyJoinTable ).hasSize( 2 );
			assertThat( container.manyToMany ).hasSize( 2 );
			assertThat( container.elementCollection ).hasSize( 2 );
		} );
	}

	@Entity( name = "CollectionsContainer" )
	public static class CollectionsContainer {
		@Id
		private Long id;

		private String name;

		@OneToMany
		@JoinColumn( name = "container_id", insertable = false, updatable = false )
		private List<TargetEntity> oneToMany = new ArrayList<>();

		@OneToMany
		@JoinTable(
				name = "otm_join_table",
				joinColumns = @JoinColumn( name = "container_id", insertable = false, updatable = false ),
				inverseJoinColumns = @JoinColumn( name = "basic_id" )
		)
		private List<BasicEntity> oneToManyJoinTable = new ArrayList<>();

		@ManyToMany
		@JoinTable(
				name = "mtm_join_table",
				joinColumns = @JoinColumn( name = "container_id", insertable = false, updatable = false ),
				inverseJoinColumns = @JoinColumn( name = "basic_id" )
		)
		private List<BasicEntity> manyToMany = new ArrayList<>();

		@ElementCollection
		@Column( name = "long_value" )
		@CollectionTable( name = "el_col_table", joinColumns = @JoinColumn( name = "container_id", insertable = false, updatable = false ) )
		private List<Long> elementCollection = new ArrayList<>();

		public CollectionsContainer() {
		}

		public CollectionsContainer(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity( name = "TargetEntity" )
	public static class TargetEntity {
		@Id
		private Long id;

		public TargetEntity() {
		}

		public TargetEntity(Long id) {
			this.id = id;
		}
	}
}
