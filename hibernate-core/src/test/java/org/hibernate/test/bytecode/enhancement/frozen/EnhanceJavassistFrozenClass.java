/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.frozen;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javassist.ClassPool;
import javassist.CtClass;

import org.hibernate.bytecode.enhance.internal.javassist.EnhancerImpl;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.test.util.ReflectionUtil;
import org.junit.Test;

import org.mockito.Mockito;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-11322")
public class EnhanceJavassistFrozenClass extends BaseUnitTestCase {

	@Test
	public void test() {
		ClassPool classPoolMock = Mockito.mock(ClassPool.class);

		EnhancementContext enhancementContextMock = Mockito.mock(EnhancementContext.class);
		EnhancerImpl enhancer = new EnhancerImpl( enhancementContextMock );
		ReflectionUtil.setField( enhancer, "classPool", classPoolMock );
		CtClass ctClassMock = Mockito.mock(CtClass.class);
		try {
			when( ctClassMock.isFrozen() ).thenReturn( true );
			when( ctClassMock.isInterface() ).thenReturn( true );
			when(classPoolMock.makeClassIfNew(any( ByteArrayInputStream.class))).thenReturn( ctClassMock );
			enhancer.enhance( "abc", new byte[] { 1, 2, 3 } );

		}
		catch ( IOException e ) {
			fail(e.getMessage());
		}
		finally {
			verify( ctClassMock, times( 1 ) ).defrost();
		}
	}
}
