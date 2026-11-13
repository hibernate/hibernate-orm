package org.hibernate.orm.test.jpa.query;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.HibernateException;
import org.hibernate.annotations.Type;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

/**
 * @author Andrea Boriero
 */
@Jpa(
		annotatedClasses = {QueryParametersValidationTest.TestEntity.class},
		integrationSettings = {@Setting(name = AvailableSettings.JPA_LOAD_BY_ID_COMPLIANCE, value = "true")}
)
public class QueryParametersValidationTest {

	@JiraKey(value = "HHH-11397")
	@Test
	public void setParameterWithWrongTypeShouldThrowIllegalArgumentException(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			Assertions.assertThrows(
					IllegalArgumentException.class,
					() -> entityManager.createQuery( "select e from TestEntity e where e.id = :id" ).setParameter( "id", "X" )
			);
		} );
	}

	@Test
	public void setParameterWithCorrectTypeShouldNotThrowIllegalArgumentException(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			entityManager.createQuery( "select e from TestEntity e where e.id = :id" ).setParameter( "id", 1L );
		} );
	}

	@Test
	@JiraKey(value = "HHH-11971")
	public void setPrimitiveParameterShouldNotThrowExceptions(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			entityManager.createQuery( "select e from TestEntity e where e.active = :active" ).setParameter(
					"active",
					true
			);
			entityManager.createQuery( "select e from TestEntity e where e.active = :active" ).setParameter(
					"active",
					Boolean.TRUE
			);
		} );
	}

	@Test
	@JiraKey( value = "HHH-11971")
	public void setWrongPrimitiveParameterShouldThrowIllegalArgumentException(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			Assertions.assertThrows(
					IllegalArgumentException.class,
					() -> entityManager.createQuery( "select e from TestEntity e where e.active = :active" ).setParameter( "active", 'c' )
			);
		} );
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@Type( QueryParametersValidationTest.BooleanUserType.class )
		private boolean active;
	}

	public static class BooleanUserType implements UserType<Boolean> {

		@Override
		public int getSqlType() {
			return Types.CHAR;
		}

		@Override
		@Nonnull
		public Class returnedClass() {
			return boolean.class;
		}

		@Override
		public boolean equals(@Nullable Boolean x, @Nullable Boolean y) throws HibernateException {
			return Objects.equals( x, y);
		}

		@Override
		public int hashCode(@Nonnull Boolean x) throws HibernateException {
			return Objects.hashCode(x);
		}

		@Override
		@Nullable
		public Boolean nullSafeGet(@Nonnull ResultSet rs, int position, @Nonnull WrapperOptions options)
				throws SQLException {
			return "Y".equals( rs.getString( position ) );
		}

		@Override
		public void nullSafeSet(
				@Nonnull PreparedStatement st,
				@Nullable Boolean value,
				int index,
				@Nonnull WrapperOptions options) throws SQLException {
			st.setString(index, value == null ? null : value ? "Y" : "N");
		}

		@Override
		@Nullable
		public Boolean deepCopy(@Nullable Boolean value) throws HibernateException {
			return value;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		@Nullable
		public Serializable disassemble(@Nullable Boolean value) throws HibernateException {
			return null;
		}

		@Override
		@Nullable
		public Boolean assemble(@Nullable Serializable cached, @Nullable Object owner) throws HibernateException {
			return null;
		}

		@Override
		@Nullable
		public Boolean replace(
				@Nullable Boolean original,
				@Nullable Boolean target,
				@Nullable Object owner) throws HibernateException {
			return null;
		}
	}
}
