/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.usertype;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.HibernateException;
import org.hibernate.annotations.Type;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.EnhancedUserType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@DomainModel(
		annotatedClasses = UserTypeNonComparableIdTest.SomeEntity.class
)
@SessionFactory
public class UserTypeNonComparableIdTest {

	@Test
	@JiraKey(value = "HHH-8999")
	public void testUserTypeId(SessionFactoryScope scope) {
		SomeEntity e1 = new SomeEntity();
		SomeEntity e2 = new SomeEntity();
		scope.inTransaction(
				session -> {
					CustomId e1Id = new CustomId( 1L );
					e1.setCustomId( e1Id );
					CustomId e2Id = new CustomId( 2L );
					e2.setCustomId( e2Id );
					session.persist( e1 );
					session.persist( e2 );
				}
		);

		scope.inTransaction(
				session -> {
					session.remove( session.get( SomeEntity.class, e1.getCustomId() ) );
					session.remove( session.get( SomeEntity.class, e2.getCustomId() ) );
				}
		);
	}

	@Entity
	@Table(name = "some_entity")
	public static class SomeEntity {

		@Id
		@Type( CustomIdType.class )
		@Column(name = "id")
		private CustomId customId;

		public CustomId getCustomId() {
			return customId;
		}

		public void setCustomId(final CustomId customId) {
			this.customId = customId;
		}
	}

	public static class CustomId implements Serializable {

		private final Long value;

		public CustomId(final Long value) {
			this.value = value;
		}

		public Long getValue() {
			return value;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			CustomId customId = (CustomId) o;

			return !( value != null ? !value.equals( customId.value ) : customId.value != null );

		}

		@Override
		public int hashCode() {
			return value != null ? value.hashCode() : 0;
		}
	}

	public static class CustomIdType implements EnhancedUserType<CustomId> {

		@Override
		public int getSqlType() {
			return Types.BIGINT;
		}

		@Override
		public CustomId nullSafeGet(ResultSet rs, int position, WrapperOptions options)
				throws SQLException {
			Long value = rs.getLong( position );

			return new CustomId( value );
		}

		@Override
		public void nullSafeSet(
				PreparedStatement preparedStatement,
				CustomId customId,
				int index,
				WrapperOptions sessionImplementor) throws SQLException {
			if ( customId == null ) {
				preparedStatement.setNull( index, Types.BIGINT );
			}
			else {
				preparedStatement.setLong( index, customId.getValue() );
			}
		}

		@Override
		public Class<CustomId> returnedClass() {
			return CustomId.class;
		}

		@Override
		public boolean equals(CustomId x, CustomId y) throws HibernateException {
			return x.equals( y );
		}

		@Override
		public int hashCode(CustomId x) throws HibernateException {
			return x.hashCode();
		}

		@Override
		public CustomId deepCopy(CustomId value) throws HibernateException {
			return value;
		}

		@Override
		public boolean isMutable() {
			return true;
		}

		@Override
		public Serializable disassemble(CustomId value) throws HibernateException {
			return value;
		}

		@Override
		public CustomId assemble(Serializable cached, Object owner) throws HibernateException {
			return (CustomId) cached;
		}

		@Override
		public CustomId replace(CustomId original, CustomId target, Object owner) throws HibernateException {
			return original;
		}

		@Override
		public String toSqlLiteral(CustomId customId) {
			return toString( customId );
		}

		@Override
		public String toString(CustomId customId) throws HibernateException {
			if ( customId == null ) {
				return null;
			}

			final Long longValue = customId.getValue();
			if ( longValue == null ) {
				return null;
			}

			return longValue.toString();
		}

		@Override
		public CustomId fromStringValue(CharSequence sequence) throws HibernateException {
			if ( sequence == null ) {
				return null;
			}

			final long longValue = Long.parseLong( sequence.toString() );
			return new CustomId( longValue );
		}
	}
}
