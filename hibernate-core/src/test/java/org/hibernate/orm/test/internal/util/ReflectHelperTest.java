/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.internal.util;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static java.lang.Integer.valueOf;
import static org.hibernate.orm.test.internal.util.ReflectHelperTest.Status.OFF;
import static org.hibernate.orm.test.internal.util.ReflectHelperTest.Status.ON;
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

	static class D implements C {

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

	static class E extends D {

	}

	static class F extends D {
		public Status getStatus(){
			return OFF;
		}
	}

	@BeforeAll
	public static void init() {
		var sessionFactoryImplementorMock = Mockito.mock( SessionFactoryImplementor.class );

		var sessionFactoryOptionsMock = Mockito.mock( SessionFactoryOptions.class );
		when( sessionFactoryImplementorMock.getSessionFactoryOptions()).thenReturn( sessionFactoryOptionsMock );

		var serviceRegistryMock = Mockito.mock( ServiceRegistryImplementor.class );
		when( sessionFactoryImplementorMock.getServiceRegistry() ).thenReturn( serviceRegistryMock );

		var classLoaderServiceMock = Mockito.mock( ClassLoaderService.class );
		when( serviceRegistryMock.getService( eq( ClassLoaderService.class ) ) ).thenReturn( classLoaderServiceMock );
	}

	@Test
	public void test_getMethod_nestedInterfaces() {
		Assertions.assertNotNull( ReflectHelper.findGetterMethod( C.class, "id" ) );
	}

	@Test
	public void test_getMethod_superclass() {
		Assertions.assertNotNull( ReflectHelper.findGetterMethod( E.class, "id" ) );
	}

	@Test
	public void test_setMethod_nestedInterfaces() {
		Assertions.assertNotNull( ReflectHelper.findSetterMethod( C.class, "id", Integer.class ) );
	}

	@JiraKey(value = "HHH-12090")
	@Test
	public void test_getMethod_nestedInterfaces_on_superclasses()
			throws InvocationTargetException, IllegalAccessException {
		Method statusMethodEClass = ReflectHelper.findGetterMethod( E.class, "status" );
		Assertions.assertNotNull( statusMethodEClass );
		Assertions.assertEquals( ON, statusMethodEClass.invoke( new E() ) );

		Method statusMethodFClass = ReflectHelper.findGetterMethod( F.class, "status" );
		Assertions.assertNotNull( statusMethodFClass );
		Assertions.assertEquals( OFF, statusMethodFClass.invoke( new F() ) );
	}

	@JiraKey(value = "HHH-12090")
	@Test
	public void test_setMethod_nestedInterfaces_on_superclasses() {
		Assertions.assertNotNull( ReflectHelper.findSetterMethod( E.class, "id", String.class ) );
	}
}
