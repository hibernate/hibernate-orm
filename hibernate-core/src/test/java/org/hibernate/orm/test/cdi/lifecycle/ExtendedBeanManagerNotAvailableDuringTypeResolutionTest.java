/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.lifecycle;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.beans.container.spi.ExtendedBeanManager;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import jakarta.enterprise.inject.spi.BeanManager;

@JiraKey(value = "HHH-15068")
public class ExtendedBeanManagerNotAvailableDuringTypeResolutionTest {

	@Test
	public void tryIt() throws IOException {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting(
						AvailableSettings.JAKARTA_CDI_BEAN_MANAGER,
						new ExtendedBeanManagerNotAvailableDuringTypeResolutionTest.ExtendedBeanManagerImpl()
				)
				.build();

		try {
			// this will trigger trying to locate MyEnumType as a managed-bean
			try (InputStream mappingInputStream =
						 new ByteArrayInputStream( TheEntity.ENTITY_DEFINITION.getBytes( StandardCharsets.UTF_8 ) )) {
				try (SessionFactory sf = new MetadataSources( ssr )
						.addInputStream( mappingInputStream )
						.buildMetadata()
						.buildSessionFactory()) {
				}
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	public static class TheEntity {
		private static final String ENTITY_NAME = "TheEntity";
		private static final String TABLE_NAME = "TheEntity";
		public static final String ENTITY_DEFINITION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<hibernate-mapping>\n" +
				"    <class name=\"" + TheEntity.class.getName() + "\" entity-name=\"" + ENTITY_NAME + "\" table=\"" + TABLE_NAME + "\">\n" +
				"        <id name=\"id\">\n" +
				"            <generator class=\"org.hibernate.id.enhanced.SequenceStyleGenerator\">\n" +
				"                <param name=\"sequence_name\">" + TABLE_NAME + "_GENERATOR</param>\n" +
				"                <param name=\"table_name\">" + TABLE_NAME + "_GENERATOR</param>\n" +
				"                <param name=\"initial_value\">1</param>\n" +
				"                <param name=\"increment_size\">1</param>\n" +
				"            </generator>\n" +
				"        </id>\n" +
				"        <property name=\"name\" />\n" +
				"        <property name=\"myEnum\" >\n" +
				"            <type name=\"org.hibernate.orm.test.EnumType\">\n" +
				"                <param name=\"enumClass\">" + MyEnum.class.getName() + "</param>\n" +
				"            </type>\n" +
				"        </property>\n" +
				"    </class>\n" +
				"</hibernate-mapping>\n";
		private Integer id;
		private String name;
		private MyEnum myEnum;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public MyEnum getMyEnum() {
			return myEnum;
		}

		public void setMyEnum(
				MyEnum myEnum) {
			this.myEnum = myEnum;
		}
	}

	private enum MyEnum {
		VALUE1,
		VALUE2;
	}

	public static class ExtendedBeanManagerImpl implements ExtendedBeanManager {
		private LifecycleListener lifecycleListener;

		@Override
		public void registerLifecycleListener(LifecycleListener lifecycleListener) {
			assert this.lifecycleListener == null;
			this.lifecycleListener = lifecycleListener;
		}

		public void notify(BeanManager ready) {
			lifecycleListener.beanManagerInitialized( ready );
		}
	}
}
