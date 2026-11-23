package org.hibernate.orm.test.id.usertype;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Comparator;

import org.hibernate.HibernateException;
import org.hibernate.annotations.Type;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.EnhancedUserType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@DomainModel(
		annotatedClasses = UserTypeComparableIdTest.SomeEntity.class
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.ORDER_UPDATES, value = "true")
)
public class UserTypeComparableIdTest {

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

	public static class CustomId implements Serializable, Comparable<CustomId> {

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

		@Override
		public int compareTo(CustomId o) {
			return value.compareTo( o.value );
		}
	}

	public static class CustomIdType implements EnhancedUserType<CustomId>, Comparator<CustomId> {

		@Override
		public int getSqlType() {
			return Types.BIGINT;
		}

		@Override
		@Nullable
		public CustomId nullSafeGet(@Nonnull ResultSet rs, int position, @Nonnull WrapperOptions options)
				throws SQLException {
			Long value = rs.getLong( position );

			return new CustomId( value );
		}

		@Override
		public void nullSafeSet(
				@Nonnull PreparedStatement preparedStatement,
				@Nullable CustomId customId,
				int index,
				@Nonnull WrapperOptions sessionImplementor) throws SQLException {
			if ( customId == null ) {
				preparedStatement.setNull( index, Types.BIGINT );
			}
			else {
				preparedStatement.setLong( index, customId.getValue() );
			}
		}

		@Override
		@Nonnull
		public Class returnedClass() {
			return CustomId.class;
		}

		@Override
		public int compare(@Nonnull CustomId o1, @Nonnull CustomId o2) {
			return o1.getValue().compareTo( o2.getValue() );
		}

		@Override
		public boolean equals(@Nullable CustomId x, @Nullable CustomId y) throws HibernateException {
			return x == null ? y == null : x.equals( y );
		}

		@Override
		public int hashCode(@Nonnull CustomId x) throws HibernateException {
			return x.hashCode();
		}

		@Override
		@Nullable
		public CustomId deepCopy(@Nullable CustomId value) throws HibernateException {
			return value;
		}

		@Override
		public boolean isMutable() {
			return true;
		}

		@Override
		@Nullable
		public Serializable disassemble(@Nullable CustomId value) throws HibernateException {
			return value;
		}

		@Override
		@Nullable
		public CustomId assemble(@Nullable Serializable cached, @Nullable Object owner) throws HibernateException {
			return (CustomId) cached;
		}

		@Override
		@Nullable
		public CustomId replace(
				@Nullable CustomId original,
				@Nullable CustomId target,
				@Nullable Object owner) throws HibernateException {
			return original;
		}

		@Override
		@Nonnull
		public String toSqlLiteral(@Nonnull CustomId value) {
			return String.valueOf( value.getValue() );
		}

		@Override
		@Nullable
		public String toString(@Nullable CustomId customId) throws HibernateException {
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
		@Nullable
		public CustomId fromStringValue(@Nullable CharSequence sequence) throws HibernateException {
			if ( sequence == null ) {
				return null;
			}

			final long longValue = Long.parseLong( sequence.toString() );
			return new CustomId( longValue );
		}
	}
}
