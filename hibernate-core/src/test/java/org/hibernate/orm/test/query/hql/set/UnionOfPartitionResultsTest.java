/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql.set;

import java.time.LocalDate;

import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * @author Jan Schatteman
 */
@DomainModel(
		annotatedClasses = {UnionOfPartitionResultsTest.Apple.class, UnionOfPartitionResultsTest.Pie.class}
)
@SessionFactory
@JiraKey( "HHH-18069" )
public class UnionOfPartitionResultsTest {

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUnion.class)
	public void testSubqueryWithUnion(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String q = "SELECT id " +
							"FROM " +
							"( " +
							"( SELECT id id, bakedPie bakedPie " +
							"\tFROM Apple c " +
							") " +
							"\tUNION ALL " +
							"( SELECT id id, bakedPie bakedPie " +
							"\tFROM Apple a " +
							") " +
							")";

                    Query query = session.createQuery( q );

					query.list();
				}
		);
	}

	@Test
	public void testSubquery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String q = "SELECT id " +
							"FROM " +
							"( " +
							"\tSELECT id id, bakedPie bakedPie " +
							"\tFROM Apple c " +
							")";

					Query query = session.createQuery( q );

					query.list();
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUnion.class)
	public void testUnionQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String q = "( SELECT id id, bakedPie bakedPie " +
							"\tFROM Apple c " +
							") " +
							"\tUNION ALL " +
							"( SELECT id id, bakedPie bakedPie " +
							"\tFROM Apple c " +
							") ";

                    Query query = session.createQuery( q );

					query.list();
				}
		);
	}


	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUnion.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportPartitionBy.class)
	public void testUnionOfPartitionResults(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String q =
							"SELECT new CurrentApple(id, bakedPie.id, dir) " +
							"FROM (" +
							"(" +
							"SELECT id id, bakedPie bakedPie, bakedOn bakedOn, MAX(bakedOn) OVER (PARTITION BY bakedPie.id) mbo, -1 dir " +
							"FROM Apple c " +
							"WHERE bakedPie.id IN (1,2,3,4) AND bakedOn <= :now" +
							") UNION ALL (" +
							"SELECT id id, bakedPie bakedPie, bakedOn bakedOn, MIN(bakedOn) OVER (PARTITION BY bakedPie.id) mbo, 1 dir " +
							"FROM Apple c " +
							"WHERE bakedPie.id IN (1,2,3,4) AND bakedOn > :now" +
							")" +
							") " +
							"WHERE bakedOn = mbo ORDER BY dir";

					Query<CurrentApple> query = session.createQuery( q, CurrentApple.class );
					query.setParameter( "now", LocalDate.now());

					query.list();
				}
		);
	}


	public static class CurrentApple {
		private final int id;
		private final int pieId;
		private final int dir;

		public CurrentApple(int id, int pieId, int dir) {
			this.id = id;
			this.pieId = pieId;
			this.dir = dir;
		}

		public int getDir() {
			return dir;
		}

		public int getId() {
			return id;
		}

		public int getPieId() {
			return pieId;
		}
	}

	@Entity(name = "Apple")
	public static class Apple {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Integer id;
		private LocalDate bakedOn;
		@ManyToOne
		private Pie bakedPie;

		public Integer getId() {
			return id;
		}

		public Apple setId(Integer id) {
			this.id = id;
			return this;
		}

		public LocalDate getBakedOn() {
			return bakedOn;
		}

		public Apple setBakedOn(LocalDate bakedOn) {
			this.bakedOn = bakedOn;
			return this;
		}

		public Pie getBakedPie() {
			return bakedPie;
		}

		public Apple setBakedPie(Pie bakedPie) {
			this.bakedPie = bakedPie;
			return this;
		}
	}

	@Entity(name = "Pie")
	public static class Pie {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Integer id;
		private String taste;

		public Integer getId() {
			return id;
		}

		public Pie setId(Integer id) {
			this.id = id;
			return this;
		}

		public String getTaste() {
			return taste;
		}

		public Pie setTaste(String taste) {
			this.taste = taste;
			return this;
		}
	}

}
