/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.hhh20221;

import jakarta.persistence.TypedQuery;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.hibernate.processor.test.util.TestUtil.getMetamodelClassFor;
import static org.junit.jupiter.api.Assertions.assertEquals;

@CompilationTest
class HHH20221Test {
	@Test
	@WithClasses({ HHH20221Entity.class, HHH20221Repository.class })
	void test() throws Exception {
		System.out.println( getMetaModelSourceAsString( HHH20221Repository.class ) );
		assertMetamodelClassGeneratedFor( HHH20221Repository.class );
		assertEquals(
				TypedQuery.class,
				getMetamodelClassFor( HHH20221Repository.class )
						.getDeclaredMethod( "findByIdOrdered", Long.class )
						.getReturnType()
		);
	}
}
