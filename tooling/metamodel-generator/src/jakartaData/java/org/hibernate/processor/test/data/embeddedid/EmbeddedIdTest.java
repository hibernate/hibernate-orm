/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.embeddedid;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;

/**
 * @author Gavin King
 */
@CompilationTest
class EmbeddedIdTest {
	@Test
	@WithClasses({ Thing.class, ThingRepo.class })
	void test() {
		System.out.println( getMetaModelSourceAsString( ThingRepo.class ) );
		assertMetamodelClassGeneratedFor( Thing.class, true );
		assertMetamodelClassGeneratedFor( Thing.class );
		assertMetamodelClassGeneratedFor( ThingRepo.class );
	}
}
