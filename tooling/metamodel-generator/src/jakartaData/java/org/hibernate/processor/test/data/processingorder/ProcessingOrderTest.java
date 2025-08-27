/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.processingorder;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfMethodInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.getMethodFromMetamodelFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@CompilationTest
class ProcessingOrderTest {
	@Test
	@WithClasses({Post.class, PostRepository.class, Topic.class})
	void test() {
		assertMetamodelClassGeneratedFor( PostRepository.class );

		assertPresenceOfMethodInMetamodelFor( PostRepository.class, "getPostsByTopic", Topic.class );
		final Method method = getMethodFromMetamodelFor( PostRepository.class, "getPostsByTopic", Topic.class );
		assertEquals( Topic.class, method.getParameterTypes()[0] );
		if ( method.getGenericReturnType() instanceof ParameterizedType parameterizedType ) {
			assertEquals( List.class, parameterizedType.getRawType() );
			assertEquals( Post.class, parameterizedType.getActualTypeArguments()[0] );
		}
		else {
			fail();
		}
	}
}
