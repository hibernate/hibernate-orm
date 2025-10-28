/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.resultclass;

import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfMethodInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.hibernate.processor.test.util.TestUtil.getMethodFromMetamodelFor;
import static org.junit.jupiter.api.Assertions.assertEquals;

@CompilationTest
class ResultClassTest {
	@Test
	@WithClasses({Post.class, NameValue.class})
	void test() {
		System.out.println( getMetaModelSourceAsString( Post.class ) );
		assertMetamodelClassGeneratedFor( Post.class );

		assertPresenceOfMethodInMetamodelFor( Post.class, "getNameValue", EntityManager.class );
		final Method method = getMethodFromMetamodelFor( Post.class, "getNameValue", EntityManager.class );
		if ( method.getGenericReturnType() instanceof ParameterizedType parameterized ) {
			assertEquals( List.class, parameterized.getRawType() );
			assertEquals( NameValue.class, parameterized.getActualTypeArguments()[0] );
		}
		else {
			Assertions.fail();
		}
	}
}
