/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.lifecycle;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.HibernateException;
import org.hibernate.annotations.Type;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.beans.container.spi.ExtendedBeanManager;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.UserType;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

@JiraKey(value = "HHH-16096")
public class ExtendedBeanManagerNotAvailableDuringCustomUserTypeInitTest {

	@Test
	public void tryIt() {
		// pass ExtendedBeanManager but never initialize it
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.JAKARTA_CDI_BEAN_MANAGER, new ExtendedBeanManagerImpl() )
				.build();

		// this will trigger initialization of dynamic parameterized user type bean
		//noinspection EmptyTryBlock
		try (var ignored = new MetadataSources(ssr)
				.addAnnotatedClass(TheEntity.class)
				.buildMetadata()
				.buildSessionFactory()) {
		}
		finally {
			StandardServiceRegistryBuilder.destroy(ssr);
		}
	}

	@SuppressWarnings("JpaDataSourceORMInspection")
	@Entity(name = "TheEntity")
	@Table(name = "TheEntity")
	public static class TheEntity {
		@Id
		private Integer id;
		private String name;

		@Type(CustomDynamicParameterizedUserType.class)
		private String custom;
	}

	public static class CustomDynamicParameterizedUserType implements UserType<Object>, DynamicParameterizedType {

		@Override
		public int getSqlType() {
			return Types.VARCHAR;
		}

		@Override
		public Class<Object> returnedClass() {
			return Object.class;
		}

		@Override
		public boolean equals(Object x, Object y) throws HibernateException {
			if (x == null) {
				return y == null;
			} else {
				return x.equals(y);
			}
		}

		@Override
		public int hashCode(Object x) throws HibernateException {
			return x == null ? 0 : x.hashCode();
		}

		@Override
		public Object nullSafeGet(ResultSet rs, int i, WrapperOptions sharedSessionContractImplementor)
				throws SQLException {
			String xmldoc = rs.getString(i);
			return rs.wasNull() ? null : xmldoc;
		}

		@Override
		public void nullSafeSet(PreparedStatement st, Object value, int index, WrapperOptions options)
				throws SQLException  {
			if (value == null) {
				st.setNull(index, Types.OTHER);
			} else {
				st.setObject(index, value, Types.OTHER);
			}
		}

		@Override
		public Object deepCopy(Object value) throws HibernateException {
			return value;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public Serializable disassemble(Object value) throws HibernateException {
			return (String) value;
		}

		@Override
		public Object assemble(Serializable cached, Object owner) throws HibernateException {
			return cached;
		}

		@Override
		public Object replace(Object original, Object target, Object owner) throws HibernateException {
			return original;
		}

		@Override
		public void setParameterValues(Properties parameters) {
			// nothing
		}
	}

	public static class ExtendedBeanManagerImpl implements ExtendedBeanManager {
		private LifecycleListener lifecycleListener;

		@Override
		public void registerLifecycleListener(LifecycleListener lifecycleListener) {
			assert this.lifecycleListener == null;
			this.lifecycleListener = lifecycleListener;
		}

		// not called
		public void notify(BeanManager ready) {
			lifecycleListener.beanManagerInitialized( ready );
		}
	}
}
