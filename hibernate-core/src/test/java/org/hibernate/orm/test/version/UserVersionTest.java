package org.hibernate.orm.test.version;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.hibernate.HibernateException;
import org.hibernate.annotations.TypeRegistration;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserVersionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey("HHH-15240")
@DomainModel(annotatedClasses = UserVersionTest.TestEntity.class)
@SessionFactory
public class UserVersionTest {
	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testIt(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(session -> {
			session.persist( new TestEntity( 1 ) );
		} );

		factoryScope.inTransaction( session -> {
			TestEntity testEntity = session.find( TestEntity.class, 1 );
			assertThat( testEntity.getVersion().getRev() ).isEqualTo( CustomVersionUserVersionType.versionValue );
		} );
	}


	@Entity(name = "TestEntity")
	@TypeRegistration(basicClass = CustomVersion.class, userType =  CustomVersionUserVersionType.class)
	public static class TestEntity {
		@Id
		public int id;

		@Version
		@Column(name = "revision")
		CustomVersion version;

		public TestEntity() {
		}

		public TestEntity(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}

		public CustomVersion getVersion() {
			return version;
		}
	}

	public static class CustomVersion implements Serializable {
		private final long rev;


		public CustomVersion(long rev) {
			this.rev = rev;
		}

		public long getRev() {
			return rev;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			CustomVersion that = (CustomVersion) o;
			return rev == that.rev;
		}

		@Override
		public int hashCode() {
			return Objects.hash( rev );
		}
	}

	public static class CustomVersionUserVersionType implements UserVersionType<CustomVersion> {
		public static long versionValue;

		@Override
		public int getSqlType() {
			return Types.BIGINT;
		}

		@Override
		@Nonnull
		public Class<CustomVersion> returnedClass() {
			return CustomVersion.class;
		}

		@Override
		public boolean equals(@Nullable CustomVersion x, @Nullable CustomVersion y) throws HibernateException {
			return x == null ? y == null : x.equals( y );
		}

		@Override
		public int hashCode(@Nonnull final CustomVersion x) throws HibernateException {
			return x.hashCode();
		}

		@Override
		@Nullable
		public CustomVersion nullSafeGet(
				@Nonnull ResultSet resultSet,
				int position,
				@Nonnull WrapperOptions options) throws SQLException {
			return new CustomVersion( resultSet.getLong( position ) );
		}

		@Override
		public void nullSafeSet(
				@Nonnull PreparedStatement statement,
				@Nullable CustomVersion value,
				int index,
				@Nonnull WrapperOptions options) throws SQLException {
			if ( value == null ) {
				statement.setNull( index, getSqlType() );
			}
			else {
				statement.setLong( index, value.getRev() );
			}
		}

		@Override
		@Nullable
		public CustomVersion deepCopy(@Nullable CustomVersion value) throws HibernateException {
			return value;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		@Nullable
		public Serializable disassemble(@Nullable CustomVersion value) {
			return value;
		}

		@Override
		@Nullable
		public CustomVersion assemble(@Nullable Serializable cached, @Nullable Object owner) throws HibernateException {
			return (CustomVersion) cached;
		}

		@Override
		@Nullable
		public CustomVersion replace(
				@Nullable CustomVersion original,
				@Nullable CustomVersion target,
				@Nullable Object owner)
				throws HibernateException {
			return original;
		}

		@Override
		@Nonnull
		public CustomVersion seed(@Nullable SharedSessionContractImplementor session) {
			return new CustomVersion( ++versionValue );
		}

		@Override
		@Nonnull
		public CustomVersion next(@Nonnull CustomVersion current, @Nonnull SharedSessionContractImplementor session) {
			return seed( session );
		}

		@Override
		public int compare(@Nonnull CustomVersion a, @Nonnull CustomVersion b) {
			return Long.compare( a.getRev(), b.getRev() );
		}
	}
}
