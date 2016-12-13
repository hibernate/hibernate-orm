/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util;

import javax.persistence.FetchType;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;

import org.junit.Test;

import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <code>ReflectHelperTest</code> -
 *
 * @author Vlad Mihalcea
 */
public class ReflectHelperTest {

	public enum Status {
		ON,
		OFF
	}

	@Test
	public void testGetConstantValue() {
		Object value;

		ClassLoaderService classLoaderServiceMock = Mockito.mock(ClassLoaderService.class);
		value = ReflectHelper.getConstantValue( "alias.b", classLoaderServiceMock);
		assertNull(value);
		verify(classLoaderServiceMock, never()).classForName( anyString() );
		Mockito.reset( classLoaderServiceMock );

		value = ReflectHelper.getConstantValue( "alias.b.c", classLoaderServiceMock);
		assertNull(value);
		verify(classLoaderServiceMock, never()).classForName( anyString() );
		Mockito.reset( classLoaderServiceMock );

		when( classLoaderServiceMock.classForName( "javax.persistence.FetchType" ) ).thenReturn( (Class) FetchType.class );
		value = ReflectHelper.getConstantValue( "javax.persistence.FetchType.LAZY", classLoaderServiceMock);
		assertEquals( FetchType.LAZY, value );
		verify(classLoaderServiceMock, times(1)).classForName( eq("javax.persistence.FetchType") );
		Mockito.reset( classLoaderServiceMock );

		when( classLoaderServiceMock.classForName( "org.hibernate.internal.util.ReflectHelperTest$Status" ) ).thenReturn( (Class) Status.class );
		value = ReflectHelper.getConstantValue( "org.hibernate.internal.util.ReflectHelperTest$Status", classLoaderServiceMock);
		assertNull(value);
		verify(classLoaderServiceMock, never()).classForName( eq("org.hibernate.internal.util") );
		Mockito.reset( classLoaderServiceMock );

		when( classLoaderServiceMock.classForName( "org.hibernate.internal.util.ReflectHelperTest$Status" ) ).thenReturn( (Class) Status.class );
		value = ReflectHelper.getConstantValue( "org.hibernate.internal.util.ReflectHelperTest$Status.ON", classLoaderServiceMock);
		assertEquals( Status.ON, value );
		verify(classLoaderServiceMock, times(1)).classForName( eq("org.hibernate.internal.util.ReflectHelperTest$Status") );
		Mockito.reset( classLoaderServiceMock );
	}
}