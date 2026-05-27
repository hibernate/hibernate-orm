/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.dollar;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.hibernate.processor.test.util.TestUtil.getOutBaseDir;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CompilationTest
class DollarSignRepoTest {
	@Test
	@WithClasses({ Thing.class, ThingRepository.class, ThingRepository$.class })
	void test() throws IOException {
		File generated = new File( getOutBaseDir( DollarSignRepoTest.class ),
				"org/hibernate/processor/test/data/dollar/_ThingRepository.java" );
		assertTrue( generated.exists(), "_ThingRepository.java was not generated" );
		String source = Files.readString( generated.toPath() );
		assertFalse( source.contains( "ThingRepository..class" ),
				"Generated source should not contain 'ThingRepository..class'" );
		assertFalse( source.contains( "ThingRepository.;" ),
				"Generated source should not contain invalid import with trailing dot" );
		assertTrue( source.contains( "implements ThingRepository" ),
				"Generated source should implement ThingRepository" );
	}
}
