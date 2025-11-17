/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sql;

import java.util.List;

import org.hibernate.FetchNotFoundException;
import org.hibernate.Hibernate;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		NativeQueryEagerAssociationTest.Building.class,
		NativeQueryEagerAssociationTest.Classroom.class
} )
@SessionFactory
public class NativeQueryEagerAssociationTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Building building1 = new Building( 1L, "building_1" );
			final Building building2 = new Building( 2L, "building_2" );
			final Building building3 = new Building( 3L, "building_3" );
			final Building building4 = new Building( 4L, "building_4" );
			final Building building5 = new Building( 5L, "building_5" );
			session.persist( building1 );
			session.persist( building2 );
			session.persist( building3 );
			session.persist( building4 );
			session.persist( new Classroom(
					1L,
					"classroom_1",
					building1,
					building2,
					List.of( building3, building4 )
			) );
			session.persist( building5 );
			session.persist( new Classroom( 2L, "classroom_2", building5, null, null ) );
		} );
		scope.inTransaction( session -> {
			// delete associated entity to trigger @NotFound
			session.createMutationQuery( "delete from Building where id = 5L" ).executeUpdate();
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Classroom" ).executeUpdate();
			session.createMutationQuery( "delete from Building" ).executeUpdate();
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16191" )
	public void testNativeQuery(SessionFactoryScope scope) {
		final Classroom result = (Classroom) scope.fromTransaction(
				session -> session.createNativeQuery( "select * from Classroom where id = 1", Classroom.class )
						.getSingleResult()
		);
		assertClassroom( result );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16191" )
	public void testNativeQueryNotFound(SessionFactoryScope scope) {
		assertThrows( FetchNotFoundException.class, () -> scope.inTransaction(
				session -> session.createNativeQuery( "select * from Classroom where id = 2", Classroom.class )
						.getSingleResult()
		) );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17224" )
	public void testNativeQueryColumnAliasInjection(SessionFactoryScope scope) {
		final Classroom result = (Classroom) scope.fromTransaction(
				session -> session.createNativeQuery( "select {c.*} from Classroom c where c.id = 1" )
						.addEntity( "c", Classroom.class )
						.getSingleResult()
		);
		assertClassroom( result );
	}

	private void assertClassroom(Classroom result) {
		assertEquals( 1L, result.getId() );
		assertTrue( Hibernate.isInitialized( result.getBuilding() ) );
		assertTrue( Hibernate.isInitialized( result.getSecondaryBuilding() ) );
		assertTrue( Hibernate.isInitialized( result.getAdjacentBuildings() ) );
		assertEquals( 1L, result.getBuilding().getId() );
		assertEquals( "building_1", result.getBuilding().getDescription() );
		assertEquals( 2L, result.getSecondaryBuilding().getId() );
		assertEquals( "building_2", result.getSecondaryBuilding().getDescription() );
		assertEquals( 2, result.getAdjacentBuildings().size() );
	}

	@Entity( name = "Building" )
	@Table( name = "Building" )
	public static class Building {
		@Id
		private Long id;

		private String description;

		public Building() {
		}

		public Building(Long id, String description) {
			this.id = id;
			this.description = description;
		}

		public Long getId() {
			return id;
		}

		public String getDescription() {
			return description;
		}
	}

	@Entity( name = "Classroom" )
	@Table( name = "Classroom" )
	public static class Classroom {
		@Id
		private Long id;

		private String description;

		@ManyToOne( fetch = FetchType.EAGER )
		@NotFound( action = NotFoundAction.EXCEPTION )
		@JoinColumn( name = "building_id" )
		private Building building;

		@ManyToOne( fetch = FetchType.EAGER )
		@JoinColumn( name = "secondary_id" )
		private Building secondaryBuilding;

		@OneToMany( fetch = FetchType.EAGER )
		private List<Building> adjacentBuildings;

		public Classroom() {
		}

		public Classroom(
				Long id,
				String description,
				Building building,
				Building secondaryBuilding,
				List<Building> adjacentBuildings) {
			this.id = id;
			this.description = description;
			this.building = building;
			this.secondaryBuilding = secondaryBuilding;
			this.adjacentBuildings = adjacentBuildings;
		}

		public Long getId() {
			return id;
		}

		public String getDescription() {
			return description;
		}

		public Building getBuilding() {
			return building;
		}

		public Building getSecondaryBuilding() {
			return secondaryBuilding;
		}

		public List<Building> getAdjacentBuildings() {
			return adjacentBuildings;
		}
	}
}
