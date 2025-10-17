/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.version;

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
		public Class<CustomVersion> returnedClass() {
			return CustomVersion.class;
		}

		@Override
		public boolean equals(CustomVersion x, CustomVersion y) throws HibernateException {
			return x.equals( y );
		}

		@Override
		public int hashCode(final CustomVersion x) throws HibernateException {
			return x.hashCode();
		}

		@Override
		public CustomVersion nullSafeGet(
				ResultSet resultSet,
				int position,
				WrapperOptions options) throws SQLException {
			return new CustomVersion( resultSet.getLong( position ) );
		}

		@Override
		public void nullSafeSet(
				PreparedStatement statement,
				CustomVersion value,
				int index,
				WrapperOptions options) throws SQLException {
			statement.setLong( index, value.getRev() );
		}

		@Override
		public CustomVersion deepCopy(CustomVersion value) throws HibernateException {
			return value;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public Serializable disassemble(CustomVersion value) {
			return value;
		}

		@Override
		public CustomVersion assemble(Serializable cached, Object owner) throws HibernateException {
			return (CustomVersion) cached;
		}

		@Override
		public CustomVersion replace(CustomVersion original, CustomVersion target, Object owner)
				throws HibernateException {
			return original;
		}

		@Override
		public CustomVersion seed(SharedSessionContractImplementor session) {
			return new CustomVersion( ++versionValue );
		}

		@Override
		public CustomVersion next(CustomVersion current, SharedSessionContractImplementor session) {
			return seed( session );
		}

		@Override
		public int compare(CustomVersion a, CustomVersion b) {
			return Long.compare( a.getRev(), b.getRev() );
		}
	}
}
