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
				EntityUseJoinedSubclassOptimizationTest.Thing.class,
				EntityUseJoinedSubclassOptimizationTest.Building.class,
				EntityUseJoinedSubclassOptimizationTest.House.class,
				EntityUseJoinedSubclassOptimizationTest.Skyscraper.class,
				EntityUseJoinedSubclassOptimizationTest.Vehicle.class,
				EntityUseJoinedSubclassOptimizationTest.Car.class,
				EntityUseJoinedSubclassOptimizationTest.Airplane.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
// Run only on H2 to avoid dealing with SQL dialect differences
@RequiresDialect( H2Dialect.class )
public class EntityUseJoinedSubclassOptimizationTest {

	@Test
	public void testEqTypeRestriction(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				entityManager -> {
					sqlStatementInterceptor.clear();
					entityManager.createSelectionQuery( "from Thing t where type(t) = House" )
							.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					// We need to join all tables because the EntityResult will create fetches for all subtypes.
					// We could optimize this by making use of tableGroupEntityNameUses in BaseSqmToSqlAstConverter#visitFetches,
					// but for that to be safe, we need to call BaseSqmToSqlAstConverter#visitSelectClause last
					// and make sure the select clause contains no treat expressions, as that would affect tableGroupEntityNameUses
					assertEquals(
							"select " +
									"t1_0.id," +
									"case " +
									"when t1_4.id is not null then 2 " +
									"when t1_5.id is not null then 3 " +
									"when t1_3.id is not null then 5 " +
									"when t1_1.id is not null then 6 " +
									"when t1_2.id is not null then 1 " +
									"when t1_6.id is not null then 4 " +
									"end," +
									"t1_1.seats," +
									"t1_2.nr," +
									"t1_3.doors," +
									"t1_4.familyName," +
									"t1_5.architectName," +
									"t1_5.doors," +
									"t1_6.name " +
									"from Thing t1_0 " +
									"left join Airplane t1_1 on t1_0.id=t1_1.id " +
									"join Building t1_2 on t1_0.id=t1_2.id " +
									"left join Car t1_3 on t1_0.id=t1_3.id " +
									"join House t1_4 on t1_0.id=t1_4.id " +
									"left join Skyscraper t1_5 on t1_0.id=t1_5.id " +
									"left join Vehicle t1_6 on t1_0.id=t1_6.id " +
									"where " +
									"case " +
									"when t1_4.id is not null then 2 " +
									"when t1_5.id is not null then 3 " +
									"when t1_3.id is not null then 5 " +
									"when t1_1.id is not null then 6 " +
									"when t1_2.id is not null then 1 " +
									"when t1_6.id is not null then 4 " +
									"end=2",
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
									"join Building t1_1 on t1_0.id=t1_1.id " +
									"left join House t1_2 on t1_0.id=t1_2.id " +
									"left join Skyscraper t1_3 on t1_0.id=t1_3.id " +
									"where " +
									"case " +
									"when t1_2.id is not null then 2 " +
									"when t1_3.id is not null then 3 " +
									"when t1_1.id is not null then 1 " +
									"end=1",
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
					// If we use select items directly, we only use the entity name on which the attribute was declared,
					// so we can cut down the joined tables further
					assertEquals(
							"select " +
									"t1_0.id " +
									"from Thing t1_0 " +
									"join House t1_2 on t1_0.id=t1_2.id " +
									"where " +
									"case " +
									"when t1_2.id is not null then 2 " +
									"end=2",
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
					// We need to join all tables because the EntityDomainResult will create fetches for all subtypes
					// But actually, to know if a row is of type "House" or not, we need to join that table anyway
					assertEquals(
							"select " +
									"t1_0.id," +
									"case " +
									"when t1_4.id is not null then 2 " +
									"when t1_5.id is not null then 3 " +
									"when t1_3.id is not null then 5 " +
									"when t1_1.id is not null then 6 " +
									"when t1_2.id is not null then 1 " +
									"when t1_6.id is not null then 4 " +
									"end," +
									"t1_1.seats," +
									"t1_2.nr," +
									"t1_3.doors," +
									"t1_4.familyName," +
									"t1_5.architectName," +
									"t1_5.doors," +
									"t1_6.name " +
									"from Thing t1_0 " +
									"left join Airplane t1_1 on t1_0.id=t1_1.id " +
									"left join Building t1_2 on t1_0.id=t1_2.id " +
									"left join Car t1_3 on t1_0.id=t1_3.id " +
									"left join House t1_4 on t1_0.id=t1_4.id " +
									"left join Skyscraper t1_5 on t1_0.id=t1_5.id " +
									"left join Vehicle t1_6 on t1_0.id=t1_6.id " +
									"where " +
									"case " +
									"when t1_4.id is not null then 2 " +
									"when t1_5.id is not null then 3 " +
									"when t1_3.id is not null then 5 " +
									"when t1_1.id is not null then 6 " +
									"when t1_2.id is not null then 1 " +
									"when t1_6.id is not null then 4 " +
									"end<>2",
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
									"case " +
									"when t1_4.id is not null then 2 " +
									"when t1_5.id is not null then 3 " +
									"when t1_3.id is not null then 5 " +
									"when t1_1.id is not null then 6 " +
									"when t1_2.id is not null then 1 " +
									"when t1_6.id is not null then 4 " +
									"end," +
									"t1_1.seats," +
									"t1_2.nr," +
									"t1_3.doors," +
									"t1_4.familyName," +
									"t1_5.architectName," +
									"t1_5.doors," +
									"t1_6.name " +
									"from Thing t1_0 " +
									"left join Airplane t1_1 on t1_0.id=t1_1.id " +
									"left join Building t1_2 on t1_0.id=t1_2.id " +
									"left join Car t1_3 on t1_0.id=t1_3.id " +
									"left join House t1_4 on t1_0.id=t1_4.id " +
									"left join Skyscraper t1_5 on t1_0.id=t1_5.id " +
									"left join Vehicle t1_6 on t1_0.id=t1_6.id " +
									"where " +
									"case " +
									"when t1_4.id is not null then 2 " +
									"when t1_5.id is not null then 3 " +
									"when t1_3.id is not null then 5 " +
									"when t1_1.id is not null then 6 " +
									"when t1_2.id is not null then 1 " +
									"when t1_6.id is not null then 4 " +
									"end in (2,5)",
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
									"case " +
									"when t1_4.id is not null then 2 " +
									"when t1_5.id is not null then 3 " +
									"when t1_3.id is not null then 5 " +
									"when t1_1.id is not null then 6 " +
									"when t1_2.id is not null then 1 " +
									"when t1_6.id is not null then 4 " +
									"end," +
									"t1_1.seats," +
									"t1_2.nr," +
									"t1_3.doors," +
									"t1_4.familyName," +
									"t1_5.architectName," +
									"t1_5.doors," +
									"t1_6.name " +
									"from Thing t1_0 " +
									"left join Airplane t1_1 on t1_0.id=t1_1.id " +
									"join Building t1_2 on t1_0.id=t1_2.id " +
									"left join Car t1_3 on t1_0.id=t1_3.id " +
									"left join House t1_4 on t1_0.id=t1_4.id " +
									"left join Skyscraper t1_5 on t1_0.id=t1_5.id " +
									"left join Vehicle t1_6 on t1_0.id=t1_6.id " +
									"where " +
									"case " +
									"when t1_4.id is not null then 2 " +
									"when t1_5.id is not null then 3 " +
									"when t1_3.id is not null then 5 " +
									"when t1_1.id is not null then 6 " +
									"when t1_2.id is not null then 1 " +
									"when t1_6.id is not null then 4 " +
									"end in (2,3)",
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
									"case " +
									"when t1_4.id is not null then 2 " +
									"when t1_5.id is not null then 3 " +
									"when t1_3.id is not null then 5 " +
									"when t1_1.id is not null then 6 " +
									"when t1_2.id is not null then 1 " +
									"when t1_6.id is not null then 4 " +
									"end," +
									"t1_1.seats," +
									"t1_2.nr," +
									"t1_3.doors," +
									"t1_4.familyName," +
									"t1_5.architectName," +
									"t1_5.doors," +
									"t1_6.name " +
									"from Thing t1_0 " +
									"left join Airplane t1_1 on t1_0.id=t1_1.id " +
									"left join Building t1_2 on t1_0.id=t1_2.id " +
									"left join Car t1_3 on t1_0.id=t1_3.id " +
									"left join House t1_4 on t1_0.id=t1_4.id " +
									"left join Skyscraper t1_5 on t1_0.id=t1_5.id " +
									"left join Vehicle t1_6 on t1_0.id=t1_6.id " +
									"where " +
									"case " +
									"when t1_4.id is not null then 2 " +
									"when t1_5.id is not null then 3 " +
									"when t1_3.id is not null then 5 " +
									"when t1_1.id is not null then 6 " +
									"when t1_2.id is not null then 1 " +
									"when t1_6.id is not null then 4 " +
									"end not in (2,5)",
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
					// We need to join all tables because the EntityResult will create fetches for all subtypes.
					// See #testEqTypeRestriction() for further explanation
					assertEquals(
							"select " +
									"t1_0.id," +
									"case " +
									"when t1_1.id is not null then 2 " +
									"when t1_5.id is not null then 3 " +
									"when t1_4.id is not null then 5 " +
									"when t1_2.id is not null then 6 " +
									"when t1_3.id is not null then 1 " +
									"when t1_6.id is not null then 4 " +
									"end," +
									"t1_2.seats," +
									"t1_3.nr," +
									"t1_4.doors," +
									"t1_1.familyName," +
									"t1_5.architectName," +
									"t1_5.doors,t1_6.name " +
									"from Thing t1_0 " +
									"left join House t1_1 on t1_0.id=t1_1.id " +
									"left join Airplane t1_2 on t1_0.id=t1_2.id " +
									"left join Building t1_3 on t1_0.id=t1_3.id " +
									"left join Car t1_4 on t1_0.id=t1_4.id " +
									"left join Skyscraper t1_5 on t1_0.id=t1_5.id " +
									"left join Vehicle t1_6 on t1_0.id=t1_6.id " +
									"where t1_1.familyName is not null",
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
					entityManager.createSelectionQuery( "select treat(t as House) from Thing t where treat(t as House).familyName is not null" )
							.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					// We need to join all tables because the EntityResult will create fetches for all subtypes.
					// See #testEqTypeRestriction() for further explanation
					assertEquals(
							"select " +
									"t1_1.id," +
									"t1_2.nr," +
									"t1_1.familyName " +
									"from Thing t1_0 " +
									"join House t1_1 on t1_0.id=t1_1.id " +
									"join Building t1_2 on t1_0.id=t1_2.id " +
									"where " +
									"t1_1.familyName is not null",
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
									"case " +
									"when t1_5.id is not null then 2 " +
									"when t1_1.id is not null then 3 " +
									"when t1_4.id is not null then 5 " +
									"when t1_2.id is not null then 6 " +
									"when t1_3.id is not null then 1 " +
									"when t1_6.id is not null then 4 " +
									"end," +
									"t1_2.seats," +
									"t1_3.nr," +
									"t1_4.doors," +
									"t1_5.familyName," +
									"t1_1.architectName," +
									"t1_1.doors," +
									"t1_6.name " +
									"from Thing t1_0 " +
									"left join Skyscraper t1_1 on t1_0.id=t1_1.id " +
									"left join Airplane t1_2 on t1_0.id=t1_2.id " +
									"left join Building t1_3 on t1_0.id=t1_3.id " +
									"left join Car t1_4 on t1_0.id=t1_4.id " +
									"left join House t1_5 on t1_0.id=t1_5.id " +
									"left join Vehicle t1_6 on t1_0.id=t1_6.id " +
									"where " +
									"case when case " +
									"when t1_5.id is not null then 2 " +
									"when t1_1.id is not null then 3 " +
									"when t1_4.id is not null then 5 " +
									"when t1_2.id is not null then 6 " +
									"when t1_3.id is not null then 1 " +
									"when t1_6.id is not null then 4 " +
									"end=3 then t1_1.doors end is not null",
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
					entityManager.createSelectionQuery( "select treat(t as Skyscraper) from Thing t where treat(t as Skyscraper).doors is not null" )
							.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals(
							"select " +
									"t1_1.id," +
									"t1_2.nr," +
									"t1_1.architectName," +
									"t1_1.doors " +
									"from Thing t1_0 " +
									"join Skyscraper t1_1 on t1_0.id=t1_1.id " +
									"join Building t1_2 on t1_0.id=t1_2.id " +
									"where " +
									"case when case " +
									"when t1_1.id is not null then 3 " +
									"when t1_2.id is not null then 1 " +
									"end=3 then t1_1.doors end is not null",
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
									"s1_1.nr " +
									"from Skyscraper s1_0 " +
									"join Building s1_1 on s1_0.id=s1_1.id",
							sqlStatementInterceptor.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Entity(name = "Thing")
	@Inheritance(strategy = InheritanceType.JOINED)
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
