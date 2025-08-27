/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.internal.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Before;
import org.junit.Test;

import org.mockito.Mockito;

import static java.lang.Integer.valueOf;
import static org.hibernate.orm.test.internal.util.ReflectHelperTest.Status.OFF;
import static org.hibernate.orm.test.internal.util.ReflectHelperTest.Status.ON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Vlad Mihalcea
 */
public class ReflectHelperTest {

	public enum Status {
		ON,
		OFF
	}

	interface A {

		Integer getId();
		void setId(Integer id);

		default Status getStatus(){
			return ON;
		}

		default void setId(String id){
			this.setId(valueOf(id));
		}
	}

	interface B extends A {
		String getName();
	}

	interface C extends B {
		String getData();
	}

	class D implements C {

		@Override
		public Integer getId() {
			return null;
		}

		@Override
		public void setId(Integer id) {

		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public String getData() {
			return null;
		}
	}

	class E extends D {

	}

	class F extends D {
		public Status getStatus(){
			return OFF;
		}
	}

	private SessionFactoryImplementor sessionFactoryImplementorMock;

	private SessionFactoryOptions sessionFactoryOptionsMock;

	private ServiceRegistryImplementor serviceRegistryMock;

	private ClassLoaderService classLoaderServiceMock;

	@Before
	public void init() {
		sessionFactoryImplementorMock = Mockito.mock(SessionFactoryImplementor.class);
		sessionFactoryOptionsMock = Mockito.mock(SessionFactoryOptions.class);
		when(sessionFactoryImplementorMock.getSessionFactoryOptions()).thenReturn( sessionFactoryOptionsMock );

		serviceRegistryMock = Mockito.mock(ServiceRegistryImplementor.class);
		when( sessionFactoryImplementorMock.getServiceRegistry() ).thenReturn( serviceRegistryMock );

		classLoaderServiceMock = Mockito.mock(ClassLoaderService.class);
		when( serviceRegistryMock.getService( eq( ClassLoaderService.class ) ) ).thenReturn( classLoaderServiceMock );
	}

	@Test
	public void test_getMethod_nestedInterfaces() {
		assertNotNull( ReflectHelper.findGetterMethod( C.class, "id" ) );
	}

	@Test
	public void test_getMethod_superclass() {
		assertNotNull( ReflectHelper.findGetterMethod( E.class, "id" ) );
	}

	@Test
	public void test_setMethod_nestedInterfaces() {
		assertNotNull( ReflectHelper.findSetterMethod( C.class, "id", Integer.class ) );
	}

	@JiraKey(value = "HHH-12090")
	@Test
	public void test_getMethod_nestedInterfaces_on_superclasses()
			throws InvocationTargetException, IllegalAccessException {
		Method statusMethodEClass = ReflectHelper.findGetterMethod( E.class, "status" );
		assertNotNull(statusMethodEClass);
		assertEquals( ON,  statusMethodEClass.invoke( new E() ) );

		Method statusMethodFClass = ReflectHelper.findGetterMethod( F.class, "status" );
		assertNotNull(statusMethodFClass);
		assertEquals( OFF,  statusMethodFClass.invoke( new F() ) );
	}

	@JiraKey(value = "HHH-12090")
	@Test
	public void test_setMethod_nestedInterfaces_on_superclasses() {
		assertNotNull( ReflectHelper.findSetterMethod( E.class, "id", String.class ) );
	}
}
