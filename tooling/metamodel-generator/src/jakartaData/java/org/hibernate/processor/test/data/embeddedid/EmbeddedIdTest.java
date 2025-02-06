/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.embeddedid;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;

/**
 * @author Gavin King
 */
public class EmbeddedIdTest extends CompilationTest {
	@Test
	@WithClasses({ Thing.class, ThingRepo.class })
	public void test() {
		System.out.println( getMetaModelSourceAsString( ThingRepo.class ) );
		assertMetamodelClassGeneratedFor( Thing.class, true );
		assertMetamodelClassGeneratedFor( Thing.class );
		assertMetamodelClassGeneratedFor( ThingRepo.class );
	}
}
