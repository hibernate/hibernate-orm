/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.persistence.FetchType;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.hib3rnat3.C0nst4nts३;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import org.mockito.Mockito;

import static java.lang.Integer.valueOf;
import static org.hibernate.internal.util.ReflectHelperTest.Status.OFF;
import static org.hibernate.internal.util.ReflectHelperTest.Status.ON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
	public void test_getConstantValue_simpleAlias() {
		when( sessionFactoryOptionsMock.isConventionalJavaConstants() ).thenReturn( true );

		Object value = ReflectHelper.getConstantValue( "alias.b", sessionFactoryImplementorMock);
		assertNull(value);
		verify(classLoaderServiceMock, never()).classForName( anyString() );
	}

	@Test
	public void test_getConstantValue_simpleAlias_non_conventional() {
		when( sessionFactoryOptionsMock.isConventionalJavaConstants() ).thenReturn( false );

		Object value = ReflectHelper.getConstantValue( "alias.b", sessionFactoryImplementorMock);
		assertNull(value);
		verify(classLoaderServiceMock, times(1)).classForName( eq( "alias" ) );
	}

	@Test
	public void test_getConstantValue_nestedAlias() {
		when( sessionFactoryOptionsMock.isConventionalJavaConstants() ).thenReturn( true );

		Object value = ReflectHelper.getConstantValue( "alias.b.c", sessionFactoryImplementorMock);
		assertNull(value);
		verify(classLoaderServiceMock, never()).classForName( anyString() );
	}

	@Test
	public void test_getConstantValue_nestedAlias_non_conventional() {
		when( sessionFactoryOptionsMock.isConventionalJavaConstants() ).thenReturn( false );

		Object value = ReflectHelper.getConstantValue( "alias.b.c", sessionFactoryImplementorMock);
		assertNull(value);
		verify(classLoaderServiceMock, times(1)).classForName( eq( "alias.b" ) );
	}

	@Test
	public void test_getConstantValue_outerEnum() {
		when( sessionFactoryOptionsMock.isConventionalJavaConstants() ).thenReturn( true );

		when( classLoaderServiceMock.classForName( "javax.persistence.FetchType" ) ).thenReturn( (Class) FetchType.class );
		Object value = ReflectHelper.getConstantValue( "javax.persistence.FetchType.LAZY", sessionFactoryImplementorMock);
		assertEquals( FetchType.LAZY, value );
		verify(classLoaderServiceMock, times(1)).classForName( eq("javax.persistence.FetchType") );
	}

	@Test
	public void test_getConstantValue_enumClass() {
		when( sessionFactoryOptionsMock.isConventionalJavaConstants() ).thenReturn( true );

		when( classLoaderServiceMock.classForName( "org.hibernate.internal.util.ReflectHelperTest$Status" ) ).thenReturn( (Class) Status.class );
		Object value = ReflectHelper.getConstantValue( "org.hibernate.internal.util.ReflectHelperTest$Status", sessionFactoryImplementorMock);
		assertNull(value);
		verify(classLoaderServiceMock, never()).classForName( eq("org.hibernate.internal.util") );
	}

	@Test
	public void test_getConstantValue_nestedEnum() {

		when( sessionFactoryOptionsMock.isConventionalJavaConstants() ).thenReturn( true );
		when( classLoaderServiceMock.classForName( "org.hibernate.internal.util.ReflectHelperTest$Status" ) ).thenReturn( (Class) Status.class );
		Object value = ReflectHelper.getConstantValue( "org.hibernate.internal.util.ReflectHelperTest$Status.ON", sessionFactoryImplementorMock);
		assertEquals( ON, value );
		verify(classLoaderServiceMock, times(1)).classForName( eq("org.hibernate.internal.util.ReflectHelperTest$Status") );
	}

	@Test
	public void test_getConstantValue_constant_digits() {

		when( sessionFactoryOptionsMock.isConventionalJavaConstants() ).thenReturn( true );
		when( classLoaderServiceMock.classForName( "org.hibernate.internal.util.hib3rnat3.C0nst4nts३" ) ).thenReturn( (Class) C0nst4nts३.class );
		Object value = ReflectHelper.getConstantValue( "org.hibernate.internal.util.hib3rnat3.C0nst4nts३.ABC_DEF", sessionFactoryImplementorMock);
		assertEquals( C0nst4nts३.ABC_DEF, value );
		verify(classLoaderServiceMock, times(1)).classForName( eq("org.hibernate.internal.util.hib3rnat3.C0nst4nts३") );
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

	@TestForIssue(jiraKey = "HHH-12090")
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

	@TestForIssue(jiraKey = "HHH-12090")
	@Test
	public void test_setMethod_nestedInterfaces_on_superclasses() {
		assertNotNull( ReflectHelper.findSetterMethod( E.class, "id", String.class ) );
	}
}