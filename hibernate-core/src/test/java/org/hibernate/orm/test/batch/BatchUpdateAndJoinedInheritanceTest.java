/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		BatchUpdateAndJoinedInheritanceTest.JoinedBase.class,
		BatchUpdateAndJoinedInheritanceTest.JoinedEntity.class
} )
@ServiceRegistry( settings = @Setting( name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "5" ) )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16517" )
public class BatchUpdateAndJoinedInheritanceTest {
	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from JoinedBase" ).executeUpdate() );
	}

	@Test
	public void testMultipleUpdates(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			JoinedEntity entity = new JoinedEntity( 1L, "original_name", 0 );
			session.persist( entity );
			session.flush();
			session.clear();
			// first update
			entity = session.get( JoinedEntity.class, 1L );
			entity.setName( "updated_name" );
			entity.setValue( 1 );
			session.flush();
			session.clear();
			// second update
			entity = session.get( JoinedEntity.class, 1L );
			assertEquals( "updated_name", entity.getName() );
			assertEquals( 1, entity.getValue() );
			entity.setName( "updated_name_2" );
			entity.setValue( 2 );
			session.flush();
			session.clear();
			// final assertions
			entity = session.get( JoinedEntity.class, 1L );
			assertEquals( "updated_name_2", entity.getName() );
			assertEquals( 2, entity.getValue() );
		} );
	}

	@Entity( name = "JoinedBase" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public static class JoinedBase {
		@Id
		private Long id;

		@Column( name = "value_col" )
		private Integer value;

		public JoinedBase() {
		}

		public JoinedBase(Long id, Integer value) {
			this.id = id;
			this.value = value;
		}

		public Integer getValue() {
			return value;
		}

		public void setValue(Integer value) {
			this.value = value;
		}
	}

	@Entity( name = "JoinedEntity" )
	public static class JoinedEntity extends JoinedBase {
		private String name;

		public JoinedEntity() {
		}

		public JoinedEntity(Long id, String name, Integer value) {
			super( id, value );
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
