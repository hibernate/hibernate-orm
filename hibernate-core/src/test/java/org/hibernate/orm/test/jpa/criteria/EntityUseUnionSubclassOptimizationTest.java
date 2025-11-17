/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(
		annotatedClasses = {
				EntityUseUnionSubclassOptimizationTest.Thing.class,
				EntityUseUnionSubclassOptimizationTest.Building.class,
				EntityUseUnionSubclassOptimizationTest.House.class,
				EntityUseUnionSubclassOptimizationTest.Skyscraper.class,
				EntityUseUnionSubclassOptimizationTest.Vehicle.class,
				EntityUseUnionSubclassOptimizationTest.Car.class,
				EntityUseUnionSubclassOptimizationTest.Airplane.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
// Run only on H2 to avoid dealing with SQL dialect differences
@RequiresDialect( H2Dialect.class )
public class EntityUseUnionSubclassOptimizationTest {

	@Test
	public void testEqTypeRestriction(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				entityManager -> {
					sqlStatementInterceptor.clear();
					entityManager.createSelectionQuery( "from Thing t where type(t) = House", Thing.class )
							.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals(
							"select " +
									"t1_0.id," +
									"t1_0.clazz_," +
									"t1_0.seats," +
									"t1_0.nr," +
									"t1_0.doors," +
									"t1_0.familyName," +
									"t1_0.architectName," +
									"t1_0.name " +
									"from (" +
									"select id, nr, null as name, null as seats, null as architectName, null as doors, familyName, 2 as clazz_ from House" +
									") t1_0 " +
									"where " +
									"t1_0.clazz_=2",
							sqlStatementInterceptor.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	public void testEqSuperTypeRestriction(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				entityManager -> {
					sqlStatementInterceptor.clear();
					entityManager.createSelectionQuery( "select 1 from Thing t where type(t) = Building", Integer.class )
							.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals(
							"select " +
									"1 " +
									"from (" +
									"select id, nr, 1 as clazz_ from Building" +
									") t1_0 " +
									"where " +
									"t1_0.clazz_=1",
							sqlStatementInterceptor.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	public void testEqTypeRestrictionSelectId(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				entityManager -> {
					sqlStatementInterceptor.clear();
					entityManager.createSelectionQuery( "select t.id from Thing t where type(t) = House", Long.class )
							.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals(
							"select " +
									"t1_0.id " +
									"from (" +
									"select id, nr, familyName, 2 as clazz_ from House" +
									") t1_0 " +
									"where " +
									"t1_0.clazz_=2",
							sqlStatementInterceptor.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	public void testNotEqTypeRestriction(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				entityManager -> {
					sqlStatementInterceptor.clear();
					entityManager.createSelectionQuery( "from Thing t where type(t) <> House", Thing.class )
							.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals(
							"select " +
									"t1_0.id," +
									"t1_0.clazz_," +
									"t1_0.seats," +
									"t1_0.nr," +
									"t1_0.doors," +
									"t1_0.familyName," +
									"t1_0.architectName," +
									"t1_0.name " +
									"from (" +
									"select id, null as nr, name, seats, null as architectName, null as doors, null as familyName, 6 as clazz_ from Airplane " +
									"union all " +
									"select id, nr, null as name, null as seats, null as architectName, null as doors, null as familyName, 1 as clazz_ from Building " +
									"union all " +
									"select id, null as nr, name, null as seats, null as architectName, doors, null as familyName, 5 as clazz_ from Car " +
									"union all " +
									"select id, nr, null as name, null as seats, architectName, doors, null as familyName, 3 as clazz_ from Skyscraper " +
									"union all " +
									"select id, null as nr, name, null as seats, null as architectName, null as doors, null as familyName, 4 as clazz_ from Vehicle" +
									") t1_0 " +
									"where " +
									"t1_0.clazz_<>2",
							sqlStatementInterceptor.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	public void testInTypeRestriction(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				entityManager -> {
					sqlStatementInterceptor.clear();
					entityManager.createSelectionQuery( "from Thing t where type(t) in (House, Car)", Thing.class )
							.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals(
							"select " +
									"t1_0.id," +
									"t1_0.clazz_," +
									"t1_0.seats," +
									"t1_0.nr," +
									"t1_0.doors," +
									"t1_0.familyName," +
									"t1_0.architectName," +
									"t1_0.name " +
									"from (" +
									"select id, null as nr, name, null as seats, null as architectName, doors, null as familyName, 5 as clazz_ from Car " +
									"union all " +
									"select id, nr, null as name, null as seats, null as architectName, null as doors, familyName, 2 as clazz_ from House" +
									") t1_0 " +
									"where " +
									"t1_0.clazz_ in (2,5)",
							sqlStatementInterceptor.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	public void testInTypeCommonSuperTypeRestriction(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				entityManager -> {
					sqlStatementInterceptor.clear();
					entityManager.createSelectionQuery( "from Thing t where type(t) in (House, Skyscraper)", Thing.class )
							.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals(
							"select " +
									"t1_0.id," +
									"t1_0.clazz_," +
									"t1_0.seats," +
									"t1_0.nr," +
									"t1_0.doors," +
									"t1_0.familyName," +
									"t1_0.architectName," +
									"t1_0.name " +
									"from (" +
									"select id, nr, null as name, null as seats, null as architectName, null as doors, familyName, 2 as clazz_ from House " +
									"union all " +
									"select id, nr, null as name, null as seats, architectName, doors, null as familyName, 3 as clazz_ from Skyscraper" +
									") t1_0 " +
									"where " +
									"t1_0.clazz_ in (2,3)",
							sqlStatementInterceptor.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	public void testNotInTypeRestriction(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				entityManager -> {
					sqlStatementInterceptor.clear();
					entityManager.createSelectionQuery( "from Thing t where type(t) not in (House, Car)", Thing.class )
							.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals(
							"select " +
									"t1_0.id," +
									"t1_0.clazz_," +
									"t1_0.seats," +
									"t1_0.nr," +
									"t1_0.doors," +
									"t1_0.familyName," +
									"t1_0.architectName," +
									"t1_0.name " +
									"from (" +
									"select id, null as nr, name, seats, null as architectName, null as doors, null as familyName, 6 as clazz_ from Airplane " +
									"union all " +
									"select id, nr, null as name, null as seats, null as architectName, null as doors, null as familyName, 1 as clazz_ from Building " +
									"union all " +
									"select id, nr, null as name, null as seats, architectName, doors, null as familyName, 3 as clazz_ from Skyscraper " +
									"union all " +
									"select id, null as nr, name, null as seats, null as architectName, null as doors, null as familyName, 4 as clazz_ from Vehicle" +
									") t1_0 " +
									"where " +
									"t1_0.clazz_ not in (2,5)",
							sqlStatementInterceptor.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	public void testTreatPath(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				entityManager -> {
					sqlStatementInterceptor.clear();
					entityManager.createSelectionQuery( "from Thing t where treat(t as House).familyName is not null", Thing.class )
							.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals(
							"select " +
									"t1_0.id," +
									"t1_0.clazz_," +
									"t1_0.seats," +
									"t1_0.nr," +
									"t1_0.doors," +
									"t1_0.familyName," +
									"t1_0.architectName," +
									"t1_0.name " +
									"from (" +
									"select id, nr, familyName, null as architectName, null as doors, null as name, null as seats, 2 as clazz_ from House " +
									"union all " +
									"select id, nr, null as familyName, architectName, doors, null as name, null as seats, 3 as clazz_ from Skyscraper " +
									"union all " +
									"select id, null as nr, null as familyName, null as architectName, doors, name, null as seats, 5 as clazz_ from Car " +
									"union all " +
									"select id, null as nr, null as familyName, null as architectName, null as doors, name, seats, 6 as clazz_ from Airplane " +
									"union all " +
									"select id, nr, null as familyName, null as architectName, null as doors, null as name, null as seats, 1 as clazz_ from Building " +
									"union all " +
									"select id, null as nr, null as familyName, null as architectName, null as doors, name, null as seats, 4 as clazz_ from Vehicle" +
									") t1_0 " +
									"where " +
									"t1_0.familyName is not null",
							sqlStatementInterceptor.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	public void testTreatPathEverywhere(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				entityManager -> {
					sqlStatementInterceptor.clear();
					entityManager.createSelectionQuery( "select treat(t as House) from Thing t where treat(t as House).familyName is not null", Thing.class )
							.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals(
							"select " +
									"t1_0.id," +
									"t1_0.nr," +
									"t1_0.familyName " +
									"from (" +
									"select id, nr, familyName, 2 as clazz_ from House" +
									") t1_0 " +
									"where " +
									"t1_0.familyName is not null",
							sqlStatementInterceptor.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	public void testTreatPathSharedColumn(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				entityManager -> {
					sqlStatementInterceptor.clear();
					entityManager.createSelectionQuery( "from Thing t where treat(t as Skyscraper).doors is not null", Thing.class )
							.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals(
							"select " +
									"t1_0.id," +
									"t1_0.clazz_," +
									"t1_0.seats," +
									"t1_0.nr," +
									"t1_0.doors," +
									"t1_0.familyName," +
									"t1_0.architectName," +
									"t1_0.name " +
									"from (" +
									"select id, nr, familyName, null as architectName, null as doors, null as name, null as seats, 2 as clazz_ from House " +
									"union all " +
									"select id, nr, null as familyName, architectName, doors, null as name, null as seats, 3 as clazz_ from Skyscraper " +
									"union all " +
									"select id, null as nr, null as familyName, null as architectName, doors, name, null as seats, 5 as clazz_ from Car " +
									"union all " +
									"select id, null as nr, null as familyName, null as architectName, null as doors, name, seats, 6 as clazz_ from Airplane " +
									"union all " +
									"select id, nr, null as familyName, null as architectName, null as doors, null as name, null as seats, 1 as clazz_ from Building " +
									"union all " +
									"select id, null as nr, null as familyName, null as architectName, null as doors, name, null as seats, 4 as clazz_ from Vehicle" +
									") t1_0 " +
									"where " +
									"case when t1_0.clazz_=3 then t1_0.doors end is not null",
							sqlStatementInterceptor.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	public void testTreatPathEverywhereSharedColumn(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				entityManager -> {
					sqlStatementInterceptor.clear();
					entityManager.createSelectionQuery( "select treat(t as Skyscraper) from Thing t where treat(t as Skyscraper).doors is not null", Thing.class )
							.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals(
							"select " +
									"t1_0.id," +
									"t1_0.nr," +
									"t1_0.architectName," +
									"t1_0.doors " +
									"from (" +
									"select id, nr, architectName, doors, 3 as clazz_ from Skyscraper" +
									") t1_0 " +
									"where " +
									"case when t1_0.clazz_=3 then t1_0.doors end is not null",
							sqlStatementInterceptor.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	public void testQueryChildUseParent(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				entityManager -> {
					sqlStatementInterceptor.clear();
					entityManager.createSelectionQuery( "select t.nr from Skyscraper t", Integer.class )
							.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals(
							"select " +
									"s1_0.nr " +
									"from Skyscraper s1_0",
							sqlStatementInterceptor.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Entity(name = "Thing")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static abstract class Thing {
		@Id
		private Long id;

		public Thing() {
		}

		public Thing(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "Building")
	public static class Building extends Thing {

		private Integer nr;

		public Building() {
		}
	}

	@Entity(name = "House")
	public static class House extends Building {

		private String familyName;

		public House() {
		}
	}

	@Entity(name = "Skyscraper")
	public static class Skyscraper extends Building {

		private String architectName;
		private Integer doors;

		public Skyscraper() {
		}
	}

	@Entity(name = "Vehicle")
	public static class Vehicle extends Thing {

		private String name;

		public Vehicle() {
		}
	}

	@Entity(name = "Car")
	public static class Car extends Vehicle {

		private Integer doors;

		public Car() {
		}
	}

	@Entity(name = "Airplane")
	public static class Airplane extends Vehicle {

		private Integer seats;

		public Airplane() {
		}
	}
}
