/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		annotatedClasses = {
				SubselectTest.EntityA.class,
				SubselectTest.EntityB.class,
				SubselectTest.EntityC.class,
		}
)
@SessionFactory(useCollectingStatementInspector = true)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "10"),
				@Setting(name = AvailableSettings.FORMAT_SQL, value = "false"),
		}
)
@JiraKey("HHH-16624")
public class SubselectTest {

	public static final int B_ID = 2;
	public static final int B1_ID = 3;
	public static final int A_ID = 1;

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityA entityA = new EntityA( A_ID, "A" );
					EntityB entityB = new EntityB( B_ID, "B" );
					EntityB entityB1 = new EntityB( B1_ID, "B1" );

					entityA.addEntityB( entityB );
					entityA.addEntityB( entityB1 );

					EntityC entityC = new EntityC( 4, "C" );
					EntityC entityC1 = new EntityC( 5, "C1" );

					entityB.addEntityC( entityC );

					entityB1.addEntityC( entityC1 );


					session.persist( entityA );
					session.persist( entityB );
					session.persist( entityB1 );
					session.persist( entityC );
					session.persist( entityC1 );
				}
		);
	}

	@Test
	public void testSkipSubselectWhenQueryResultIsOne(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		scope.inTransaction(
				session -> {
					List<EntityB> bs = session.createQuery( "select b from EntityB b where id= :id", EntityB.class )
							.setParameter( "id", B_ID )
							.list();
					assertThat( bs.size() ).isEqualTo( 1 );
					statementInspector.clear();

					assertThat( bs.get( 0 ).getEntityCs() ).hasSize( 1 );
					List<String> sqlQueries = statementInspector.getSqlQueries();
					assertThat( sqlQueries.size() ).isEqualTo( 1 );
					String query = sqlQueries.get( 0 );
					assertFalse( containsSubquery( query ), " The query should not contain a subquery" );

				}
		);
	}

	@Test
	public void testSubselectIsCreatedWhenQueryResultIsGreaterThanOne(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		scope.inTransaction(
				session -> {
					List<EntityB> bs = session.createQuery( "select b from EntityB b ", EntityB.class )
							.list();
					assertThat( bs.size() ).isEqualTo( 2 );
					statementInspector.clear();

					assertThat( bs.get( 0 ).getEntityCs() ).hasSize( 1 );
					List<String> sqlQueries = statementInspector.getSqlQueries();
					assertThat( sqlQueries.size() ).isEqualTo( 1 );
					String query = sqlQueries.get( 0 );
					assertTrue( containsSubquery( query ), " The query should contain a subquery" );
				}
		);
	}

	@Test
	public void testSubselectIsCreatedWhenQueryResultIsGreaterThanOne2(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		scope.inTransaction(
				session -> {
					EntityA a = session.get(EntityA.class,A_ID);
					List<EntityB> bs = a.getEntityBs();
					assertThat( bs.size() ).isEqualTo( 2 );
					statementInspector.clear();

					assertThat( bs.get( 0 ).getEntityCs() ).hasSize( 1 );
					List<String> sqlQueries = statementInspector.getSqlQueries();
					assertThat( sqlQueries.size() ).isEqualTo( 1 );
					String query = sqlQueries.get( 0 );
					assertTrue( containsSubquery( query ), " The query should contain a subquery" );
				}
		);
	}

	private static boolean containsSubquery(String query) {
		return query.toLowerCase().substring( query.indexOf( "where" ) ).contains( "select" );
	}

	@Entity(name = "EntityA")
	@Table(name = "ENTITY_A")
	public static class EntityA {
		@Id
		Integer id;

		String name;

		@OneToMany
		List<EntityB> entityBs = new ArrayList<>();

		public EntityA() {
		}

		public EntityA(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public List<EntityB> getEntityBs() {
			return entityBs;
		}

		public void addEntityB(EntityB entityB) {
			entityBs.add( entityB );
		}
	}

	@Entity(name = "EntityB")
	@Table(name = "ENTITY_B")
	public static class EntityB {
		@Id
		Integer id;

		String name;

		@OneToMany(mappedBy = "entityB")
		@Fetch(FetchMode.SUBSELECT)
		List<EntityC> entityCs = new ArrayList<>();

		public EntityB() {
		}

		public EntityB(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public List<EntityC> getEntityCs() {
			return entityCs;
		}

		public void addEntityC(EntityC entityC) {
			entityCs.add( entityC );
			entityC.entityB = this;
		}
	}

	@Entity(name = "EntityC")
	@Table(name = "ENTITY_C")
	public static class EntityC {
		@Id
		Integer id;

		String name;

		public EntityC() {
		}

		public EntityC(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@JoinColumn(name = "ENTITY_B")
		@ManyToOne
		EntityB entityB;

		public Integer getId() {
			return id;
		}

		public EntityB getEntityB() {
			return entityB;
		}
	}

}
