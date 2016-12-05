/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.javassist;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;

import javassist.CtClass;

import org.hibernate.bytecode.enhance.internal.javassist.EnhancerImpl;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * @author Vlad Mihalcea
 */
public class EnhancerFileNotFoundTest extends BaseUnitTestCase {

	public static class Enhancer extends EnhancerImpl {

		public Enhancer(EnhancementContext enhancementContext) {
			super( enhancementContext );
		}

		@Override
		public CtClass loadCtClassFromClass(Class<?> aClass) {
			return super.loadCtClassFromClass( aClass );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11307")
	public void test()
			throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
		EnhancementContext enhancementContextMock = mock( EnhancementContext.class );

		Enhancer enhancer = new Enhancer( enhancementContextMock );
		try {
			enhancer.loadCtClassFromClass( Mockito.mock( getClass() ).getClass() );
			fail("Should throw FileNotFoundException!");
		}
		catch ( Exception expected ) {
			assertEquals( FileNotFoundException.class, expected.getCause().getClass() );
		}
	}

}
