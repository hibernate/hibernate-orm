/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.write;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.HibernateException;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.jdbc.Expectation;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = CustomSqlWithExpectationTests.CustomEntity.class )
@SessionFactory
public class CustomSqlWithExpectationTests {
	@Test
	public void testBasicOperations(SessionFactoryScope scope) {

		assertEquals(0,count);

		// insert
		scope.inTransaction( (session) -> {
			session.persist( new CustomEntity( 1, "csutmo" ) );
		} );

		assertEquals(1,count);

		// update
		scope.inTransaction( (session) -> {
			final CustomEntity customEntity = session.get( CustomEntity.class, 1 );
			customEntity.setName( "custom" );
		} );

		assertEquals(2,count);

		// delete
		scope.inTransaction( (session) -> {
			final CustomEntity customEntity = session.get( CustomEntity.class, 1 );
			assertThat( customEntity.getName() ).isEqualTo( "custom" );
			session.remove( customEntity );
		} );

		assertEquals(3,count);
	}

	static int count = 0;

	public static class Custom extends Expectation.RowCount {
		@Override
		public int prepare(PreparedStatement statement) throws SQLException, HibernateException {
			count ++;
			return super.prepare(statement);
		}
	}

	@Entity( name = "CustomEntity" )
	@Table( name = "custom_entity" )
	@SQLInsert( sql = "insert into custom_entity (name, id) values (?, ?)", verify = Custom.class )
	@SQLDelete( sql = "delete from custom_entity where id = ?", verify = Custom.class )
	@SQLUpdate( sql = "update custom_entity set name = ? where id = ? ", verify = Custom.class )
	public static class CustomEntity {
	    @Id
	    private Integer id;
	    @Basic
		private String name;

		CustomEntity() {
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
