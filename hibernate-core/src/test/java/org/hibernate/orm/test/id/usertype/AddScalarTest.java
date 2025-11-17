/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.usertype;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.query.NativeQuery;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@DomainModel(
		annotatedClasses = AddScalarTest.Book.class,
		typeContributors = AddScalarTest.UuidTypeContributor.class
)
@SessionFactory
@JiraKey(value = "HHH-19703")
public class AddScalarTest {

	@BeforeAll
	static void init(SessionFactoryScope scope) {
		scope.inTransaction( session ->
				session.persist( new Book( 1L, UUID.randomUUID().toString() ) ) );
	}

	@AfterAll
	static void clean(SessionFactoryScope scope) {
		scope.inTransaction( session ->
				session.createMutationQuery( "delete from Book" ).executeUpdate() );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		final var actual = scope.fromSession( session ->
				session.createNativeQuery( "select uuid from book where id=:id" )
						.setParameter( "id", Long.valueOf( 1 ) )
						.unwrap( NativeQuery.class )
						.addScalar( "uuid", Uuid.class )
						.getSingleResult() );
		assertInstanceOf( Uuid.class, actual );
	}

	@Entity(name = "Book")
	@Table(name = "book")
	static class Book {

		@Id
		private Long id;

		private String uuid;

		public Book() {
		}

		public Book(Long id, String uuid) {
			this.id = id;
			this.uuid = uuid;
		}
	}

	record Uuid(UUID uuid) {

	}

	static class UuidType implements UserType<Uuid> {

		@Override
		public int getSqlType() {
			return SqlTypes.VARCHAR;
		}

		@Override
		public Class<Uuid> returnedClass() {
			return Uuid.class;
		}

		@Override
		public Uuid deepCopy(Uuid value) {
			return new Uuid( value.uuid );
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public Uuid nullSafeGet(ResultSet rs, int position, WrapperOptions options) throws SQLException {
			final var result = rs.getString( position );
			return rs.wasNull() ? null : new Uuid( UUID.fromString( result ) );
		}

		@Override
		public void nullSafeSet(PreparedStatement st, Uuid value, int position, WrapperOptions options)
				throws SQLException {
			if ( value == null ) {
				st.setNull( position, getSqlType() );
			}
			else {
				st.setObject( position, value.uuid.toString(), getSqlType() );
			}
		}
	}

	static class UuidTypeContributor implements TypeContributor {

		@Override
		public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
			typeContributions.contributeType( new UuidType() );
		}
	}
}
