/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.persistenceunit;

import static org.hibernate.internal.util.ConfigHelper.findAsResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.List;

import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceXmlParser;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * HHH-8364 discusses the use of <exclude-unlisted-classes> within Java SE environments.  It was intended for Java EE
 * only, but was probably supported in Java SE/Hibernate for user friendliness.  If we are going to supports its use
 * like that, the following should happen:
 *
 * Omitted == do scan
 * <exclude-unlisted-classes /> == don't scan
 * <exclude-unlisted-classes>false</exclude-unlisted-classes> == do scan
 * <exclude-unlisted-classes>true</exclude-unlisted-classes> == don't scan
 *
 * This is true for both JPA 1 & 2.  The "false" default in the JPA 1.0 XSD was a bug.
 *
 * Note that we're ignoring the XSD "true" default if the element is omitted.  Due to the negation semantics, I thought
 * it made more sense from a user standpoint.
 *
 * @author Brett Meyer
 */
@JiraKey(value = "HHH-8364")
public class ExcludeUnlistedClassesTest extends BaseUnitTestCase {

	@Test
	public void testExcludeUnlistedClasses() {
		final Collection<PersistenceUnitDescriptor> parsedDescriptors = PersistenceXmlParser.create()
				.parse( List.of( findAsResource( "org/hibernate/jpa/test/persistenceunit/META-INF/persistence.xml" ) ) )
				.values();

		doTest( parsedDescriptors, "ExcludeUnlistedClassesTest1", false );
		doTest( parsedDescriptors, "ExcludeUnlistedClassesTest2", true );
		doTest( parsedDescriptors, "ExcludeUnlistedClassesTest3", false );
		doTest( parsedDescriptors, "ExcludeUnlistedClassesTest4", true );
	}

	private void doTest(Collection<PersistenceUnitDescriptor> parsedDescriptors,
			final String persistenceUnitName, final boolean shouldExclude) {
		for (final PersistenceUnitDescriptor descriptor : parsedDescriptors) {
			if (descriptor.getName().equals( persistenceUnitName )) {
				assertEquals(descriptor.isExcludeUnlistedClasses(), shouldExclude);
				return;
			}
		}
		fail("Could not find the persistence unit: " + persistenceUnitName);
	}
}
