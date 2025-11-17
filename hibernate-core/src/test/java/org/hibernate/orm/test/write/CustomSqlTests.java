/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.write;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = CustomSqlTests.CustomEntity.class )
@SessionFactory
public class CustomSqlTests {
	@Test
	public void testBasicOperations(SessionFactoryScope scope) {
		// insert
		scope.inTransaction( (session) -> {
			session.persist( new CustomEntity( 1, "csutmo" ) );
		} );

		// update
		scope.inTransaction( (session) -> {
			final CustomEntity customEntity = session.get( CustomEntity.class, 1 );
			customEntity.setName( "custom" );
		} );

		// delete
		scope.inTransaction( (session) -> {
			final CustomEntity customEntity = session.get( CustomEntity.class, 1 );
			assertThat( customEntity.getName() ).isEqualTo( "custom" );
			session.remove( customEntity );
		} );
	}

	@Entity( name = "CustomEntity" )
	@Table( name = "custom_entity" )
	@SQLInsert( sql = "insert into custom_entity (name, id) values (?, ?)" )
	@SQLDelete( sql = "delete from custom_entity where id = ?" )
	@SQLUpdate( sql = "update custom_entity set name = ? where id = ? " )
	public static class CustomEntity {
		@Id
		private Integer id;
		@Basic
		private String name;

		private CustomEntity() {
			// for use by Hibernate
		}

		public CustomEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
