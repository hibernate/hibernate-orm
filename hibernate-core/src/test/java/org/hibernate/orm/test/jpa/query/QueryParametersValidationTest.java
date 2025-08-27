/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.annotations.Type;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * @author Andrea Boriero
 */
public class QueryParametersValidationTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {TestEntity.class};
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.JPA_LOAD_BY_ID_COMPLIANCE, "true" );
	}

	@JiraKey(value = "HHH-11397")
	@Test(expected = IllegalArgumentException.class)
	public void setParameterWithWrongTypeShouldThrowIllegalArgumentException() {
		final EntityManager entityManager = entityManagerFactory().createEntityManager();
		try {
			entityManager.createQuery( "select e from TestEntity e where e.id = :id" ).setParameter( "id", 1 );
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	public void setParameterWithCorrectTypeShouldNotThrowIllegalArgumentException() {
		final EntityManager entityManager = entityManagerFactory().createEntityManager();
		try {
			entityManager.createQuery( "select e from TestEntity e where e.id = :id" ).setParameter( "id", 1L );
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	@JiraKey(value = "HHH-11971")
	public void setPrimitiveParameterShouldNotThrowExceptions() {
		final EntityManager entityManager = entityManagerFactory().createEntityManager();
		try {
			entityManager.createQuery( "select e from TestEntity e where e.active = :active" ).setParameter(
					"active",
					true
			);
			entityManager.createQuery( "select e from TestEntity e where e.active = :active" ).setParameter(
					"active",
					Boolean.TRUE
			);
		}
		finally {
			entityManager.close();
		}
	}

	@Test(expected = IllegalArgumentException.class)
	@JiraKey( value = "HHH-11971")
	public void setWrongPrimitiveParameterShouldThrowIllegalArgumentException() {
		final EntityManager entityManager = entityManagerFactory().createEntityManager();
		try {
			entityManager.createQuery( "select e from TestEntity e where e.active = :active" ).setParameter( "active", 'c' );
		}
		finally {
			entityManager.close();
		}
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
		public Class returnedClass() {
			return boolean.class;
		}

		@Override
		public boolean equals(Boolean x, Boolean y) throws HibernateException {
			return Objects.equals( x, y);
		}

		@Override
		public int hashCode(Boolean x) throws HibernateException {
			return Objects.hashCode(x);
		}

		@Override
		public Boolean nullSafeGet(ResultSet rs, int position, WrapperOptions options)
				throws SQLException {
			return "Y".equals( rs.getString( position ) );
		}

		@Override
		public void nullSafeSet(
				PreparedStatement st,
				Boolean value,
				int index,
				WrapperOptions options) throws SQLException {
			st.setString(index, value ? "Y" : "N");
		}

		@Override
		public Boolean deepCopy(Boolean value) throws HibernateException {
			return value;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public Serializable disassemble(Boolean value) throws HibernateException {
			return null;
		}

		@Override
		public Boolean assemble(Serializable cached, Object owner) throws HibernateException {
			return null;
		}

		@Override
		public Boolean replace(Boolean original, Boolean target, Object owner) throws HibernateException {
			return null;
		}
	}
}
