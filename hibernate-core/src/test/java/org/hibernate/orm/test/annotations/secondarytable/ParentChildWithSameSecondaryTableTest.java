/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.secondarytable;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.SecondaryTable;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@ServiceRegistry( settings = @Setting( name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "-1" ) )
@DomainModel(
		annotatedClasses = {
				ParentChildWithSameSecondaryTableTest.EntityA.class,
				ParentChildWithSameSecondaryTableTest.EntityB.class,
				ParentChildWithSameSecondaryTableTest.EntityC.class,
		}
)
@SessionFactory
@JiraKey(value = "HHH-15117")
public class ParentChildWithSameSecondaryTableTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testPersist(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityC entityC = new EntityC();
					entityC.setId( 1L );
					entityC.setAttrB( "attrB-value" );
					entityC.setAttrC( "attrC-value" );
					session.persist( entityC );
				}
		);
	}

	@Test
	public void testPersist2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityC entityC = new EntityC();
					entityC.setId( 1L );
					entityC.setAttrC( "attrC-value" );
					session.persist( entityC );
					session.flush();
					session.clear();

					EntityC entityC1 = session.find( EntityC.class, 1L );
					assertThat( entityC1.getAttrC(), is( notNullValue() ) );
				}
		);
	}

	@Test
	public void testPersist3(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityC entityC = new EntityC();
					entityC.setId( 1L );
					entityC.setAttrB( "attrB-value" );
					session.persist( entityC );
					session.flush();
					session.clear();

					EntityC entityC1 = session.find( EntityC.class, 1L );
					assertThat( entityC1.getAttrB(), is( notNullValue() ) );
				}
		);
	}

	@Test
	public void testDelete(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityC entityC = new EntityC();
					entityC.setId( 1L );
					entityC.setAttrB( "attrB-value" );
					entityC.setAttrC( "attrC-value" );
					session.persist( entityC );
				}
		);
		scope.inTransaction(
				session -> {

					session.remove( session.get( EntityC.class, 1L ) );
				}
		);
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityC entityC = new EntityC();
					entityC.setId( 1L );
					entityC.setAttrB( "attrB-value" );
					entityC.setAttrC( "attrC-value" );
					session.persist( entityC );
				}
		);
		scope.inTransaction(
				session -> {
					final EntityC entityC = session.get( EntityC.class, 1L );
					assertThat( entityC.getAttrB(), is( "attrB-value" ) );
					assertThat( entityC.getAttrC(), is( "attrC-value" ) );

				}
		);
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityC entityC = new EntityC();
					entityC.setId( 1L );
					entityC.setAttrB( "attrB-value" );
					entityC.setAttrC( "attrC-value" );
					session.persist( entityC );
				}
		);
		scope.inTransaction(
				session -> {
					final EntityC entityC = session.get( EntityC.class, 1L );
					entityC.setAttrB( "new value for b" );
					entityC.setAttrC( "new value for c" );
				}
		);
		scope.inTransaction(
				session -> {
					final EntityC entityC = session.get( EntityC.class, 1L );
					assertThat( entityC.getAttrB(), is( "new value for b" ) );
					assertThat( entityC.getAttrC(), is( "new value for c" ) );

				}
		);
	}

	@Test
	public void testUpdate2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityC entityC = new EntityC();
					entityC.setId( 1L );
					entityC.setAttrB( "attrB-value" );
					entityC.setAttrC( "attrC-value" );
					session.persist( entityC );
				}
		);
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "update EntityC c set c.attrB = 'B', c.attrC = 'C'" )
							.executeUpdate();
				}
		);
		scope.inTransaction(
				session -> {
					final EntityC entityC = session.get( EntityC.class, 1L );
					assertThat( entityC.getAttrB(), is( "B" ) );
					assertThat( entityC.getAttrC(), is( "C" ) );

				}
		);
	}

	@Test
	public void testInsert(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "insert into EntityC(id, attrB,attrC) values (1L, 'B', 'C')" )
							.executeUpdate();
				}
		);
		scope.inTransaction(
				session -> {
					final EntityC entityC = session.get( EntityC.class, 1L );
					assertThat( entityC.getAttrB(), is( "B" ) );
					assertThat( entityC.getAttrC(), is( "C" ) );

				}
		);
	}

	@Entity(name = "EntityA")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(discriminatorType = DiscriminatorType.STRING, name = "type")
	public static class EntityA {

		@Id
		private Long id;

		private String name;

		public EntityA() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	protected static final String TABLE_NAME = "TABLE_B";

	@Entity(name = "EntityB")
	@SecondaryTable(name = TABLE_NAME)
	public static class EntityB extends EntityA {

		@Column(table = TABLE_NAME)
		private String attrB;

		public EntityB() {
		}

		public String getAttrB() {
			return attrB;
		}

		public void setAttrB(String attrB) {
			this.attrB = attrB;
		}
	}

	@Entity(name = "EntityC")
	@SecondaryTable(name = TABLE_NAME)
	public static class EntityC extends EntityB {

		@Column(table = TABLE_NAME)
		private String attrC;

		public EntityC() {
		}

		public String getAttrC() {
			return attrC;
		}

		public void setAttrC(String attrC) {
			this.attrC = attrC;
		}
	}
}
