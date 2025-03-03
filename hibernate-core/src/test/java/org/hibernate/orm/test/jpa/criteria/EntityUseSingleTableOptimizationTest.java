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
				EntityUseSingleTableOptimizationTest.Thing.class,
				EntityUseSingleTableOptimizationTest.Building.class,
				EntityUseSingleTableOptimizationTest.House.class,
				EntityUseSingleTableOptimizationTest.Skyscraper.class,
				EntityUseSingleTableOptimizationTest.Vehicle.class,
				EntityUseSingleTableOptimizationTest.Car.class,
				EntityUseSingleTableOptimizationTest.Airplane.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
// Run only on H2 to avoid dealing with SQL dialect differences
@RequiresDialect( H2Dialect.class )
public class EntityUseSingleTableOptimizationTest {

	@Test
	public void testEqTypeRestriction(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				entityManager -> {
					sqlStatementInterceptor.clear();
					entityManager.createSelectionQuery( "from Thing t where type(t) = House" )
							.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals(
							"select " +
									"t1_0.id," +
									"t1_0.DTYPE," +
									"t1_0.seats," +
									"t1_0.nr," +
									"t1_0.doors," +
									"t1_0.familyName," +
									"t1_0.architectName," +
									"t1_0.name " +
									"from Thing t1_0 " +
									"where " +
									"t1_0.DTYPE='House'",
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
					entityManager.createSelectionQuery( "select 1 from Thing t where type(t) = Building" )
							.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals(
							"select " +
									"1 " +
									"from Thing t1_0 " +
									"where " +
									"t1_0.DTYPE='Building'",
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
					entityManager.createSelectionQuery( "select t.id from Thing t where type(t) = House" )
							.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals(
							"select " +
									"t1_0.id " +
									"from Thing t1_0 " +
									"where " +
									"t1_0.DTYPE='House'",
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
					entityManager.createSelectionQuery( "from Thing t where type(t) <> House" )
							.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals(
							"select " +
									"t1_0.id," +
									"t1_0.DTYPE," +
									"t1_0.seats," +
									"t1_0.nr," +
									"t1_0.doors," +
									"t1_0.familyName," +
									"t1_0.architectName," +
									"t1_0.name " +
									"from Thing t1_0 " +
									"where " +
									"t1_0.DTYPE<>'House'",
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
					entityManager.createSelectionQuery( "from Thing t where type(t) in (House, Car)" )
							.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals(
							"select " +
									"t1_0.id," +
									"t1_0.DTYPE," +
									"t1_0.seats," +
									"t1_0.nr," +
									"t1_0.doors," +
									"t1_0.familyName," +
									"t1_0.architectName," +
									"t1_0.name " +
									"from Thing t1_0 " +
									"where " +
									"t1_0.DTYPE in ('House','Car')",
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
					entityManager.createSelectionQuery( "from Thing t where type(t) in (House, Skyscraper)" )
							.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals(
							"select " +
									"t1_0.id," +
									"t1_0.DTYPE," +
									"t1_0.seats," +
									"t1_0.nr," +
									"t1_0.doors," +
									"t1_0.familyName," +
									"t1_0.architectName," +
									"t1_0.name " +
									"from Thing t1_0 " +
									"where " +
									"t1_0.DTYPE in ('House','Skyscraper')",
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
					entityManager.createSelectionQuery( "from Thing t where type(t) not in (House, Car)" )
							.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals(
							"select " +
									"t1_0.id," +
									"t1_0.DTYPE," +
									"t1_0.seats," +
									"t1_0.nr," +
									"t1_0.doors," +
									"t1_0.familyName," +
									"t1_0.architectName," +
									"t1_0.name " +
									"from Thing t1_0 " +
									"where " +
									"t1_0.DTYPE not in ('House','Car')",
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
					entityManager.createSelectionQuery( "from Thing t where treat(t as House).familyName is not null" )
							.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals(
							"select " +
									"t1_0.id," +
									"t1_0.DTYPE," +
									"t1_0.seats," +
									"t1_0.nr," +
									"t1_0.doors," +
									"t1_0.familyName," +
									"t1_0.architectName," +
									"t1_0.name " +
									"from Thing t1_0 " +
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
					entityManager.createSelectionQuery( "from Thing t where treat(t as Skyscraper).doors is not null" )
							.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals(
							"select " +
									"t1_0.id," +
									"t1_0.DTYPE," +
									"t1_0.seats," +
									"t1_0.nr," +
									"t1_0.doors," +
									"t1_0.familyName," +
									"t1_0.architectName," +
									"t1_0.name " +
									"from Thing t1_0 " +
									"where " +
									"case when t1_0.DTYPE='Skyscraper' then t1_0.doors end is not null",
							sqlStatementInterceptor.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	public void testTreatPathInDisjunction(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				entityManager -> {
					sqlStatementInterceptor.clear();
					entityManager.createSelectionQuery( "from Thing t where treat(t as House).familyName is not null or t.id > 0" )
							.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals(
							"select " +
									"t1_0.id," +
									"t1_0.DTYPE," +
									"t1_0.seats," +
									"t1_0.nr," +
									"t1_0.doors," +
									"t1_0.familyName," +
									"t1_0.architectName," +
									"t1_0.name " +
									"from Thing t1_0 " +
									"where " +
									"t1_0.familyName is not null or t1_0.id>0",
							sqlStatementInterceptor.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	public void testTypeRestrictionInDisjunction(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				entityManager -> {
					sqlStatementInterceptor.clear();
					entityManager.createSelectionQuery( "from Thing t where type(t) = House or t.id > 0" )
							.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals(
							"select " +
									"t1_0.id," +
									"t1_0.DTYPE," +
									"t1_0.seats," +
									"t1_0.nr," +
									"t1_0.doors," +
									"t1_0.familyName," +
									"t1_0.architectName," +
									"t1_0.name " +
									"from Thing t1_0 " +
									"where " +
									"t1_0.DTYPE='House' or t1_0.id>0",
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
					entityManager.createSelectionQuery( "select t.nr from Skyscraper t" )
							.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals(
							"select " +
									"s1_0.nr " +
									"from Thing s1_0 " +
									"where s1_0.DTYPE='Skyscraper'",
							sqlStatementInterceptor.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Entity(name = "Thing")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
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
