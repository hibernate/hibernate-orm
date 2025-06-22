/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.multivaluebinding;

import jakarta.persistence.EntityManager;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfMethodInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.getMethodFromMetamodelFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TopicPostTest extends CompilationTest {
	@Test
	@WithClasses({Post.class, PostRepository.class})
	public void test() {
		assertMetamodelClassGeneratedFor( Post.class, true );
		assertMetamodelClassGeneratedFor( Post.class );
		assertMetamodelClassGeneratedFor( PostRepository.class );

		assertPresenceOfMethodInMetamodelFor( Post.class, "getPostsByName", EntityManager.class, List.class );
		final Method method = getMethodFromMetamodelFor( Post.class, "getPostsByName", EntityManager.class, List.class );
		final Type methodParam = method.getGenericParameterTypes()[1];
		if ( methodParam instanceof ParameterizedType parameterized ) {
			assertEquals( String.class, parameterized.getActualTypeArguments()[0] );
		}
		else {
			fail();
		}

		assertPresenceOfMethodInMetamodelFor( PostRepository.class, "getPostsByName", Collection.class );
		final Method repositoryMethod = getMethodFromMetamodelFor( PostRepository.class, "getPostsByName", Collection.class );
		final Type repositoryMethodParam = repositoryMethod.getGenericParameterTypes()[0];
		if ( repositoryMethodParam instanceof ParameterizedType parameterized ) {
			assertEquals( String.class, parameterized.getActualTypeArguments()[0] );
		}
		else {
			fail();
		}
	}
}
