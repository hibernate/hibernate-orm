/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.jpa.test.persistenceunit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.ConfigHelper;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceXmlParser;
import org.hibernate.testing.TestForIssue;
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
@TestForIssue(jiraKey = "HHH-8364")
public class ExcludeUnlistedClassesTest extends BaseUnitTestCase {
	
	@Test
	public void testExcludeUnlistedClasses() {
		// see src/test/resources/org/hibernate/jpa/test/persistenceunit/persistence.xml
		
		final Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( AvailableSettings.RESOURCES_CLASSLOADER, new TestClassLoader() );
		final List<ParsedPersistenceXmlDescriptor> parsedDescriptors = PersistenceXmlParser.locatePersistenceUnits(
				properties );
		
		doTest( parsedDescriptors, "ExcludeUnlistedClassesTest1", false );
		doTest( parsedDescriptors, "ExcludeUnlistedClassesTest2", true );
		doTest( parsedDescriptors, "ExcludeUnlistedClassesTest3", false );
		doTest( parsedDescriptors, "ExcludeUnlistedClassesTest4", true );
	}
	
	private void doTest(List<ParsedPersistenceXmlDescriptor> parsedDescriptors,
			final String persistenceUnitName, final boolean shouldExclude) {
		for (final ParsedPersistenceXmlDescriptor descriptor : parsedDescriptors) {
			if (descriptor.getName().equals( persistenceUnitName )) {
				assertEquals(descriptor.isExcludeUnlistedClasses(), shouldExclude);
				return;
			}
		}
		fail("Could not find the persistence unit: " + persistenceUnitName);
	}

    private static class TestClassLoader extends ClassLoader {
    	
    	@Override
        protected Enumeration<URL> findResources(String name) throws IOException {
    		if (name.equals( "META-INF/persistence.xml" )) {
    			final URL puUrl = ConfigHelper.findAsResource(
    					"org/hibernate/jpa/test/persistenceunit/META-INF/persistence.xml" );
    			return new Enumeration<URL>() {
        			boolean hasMore = true;
        			
    				@Override
    				public boolean hasMoreElements() {
    					return hasMore;
    				}

    				@Override
    				public URL nextElement() {
    					hasMore = false;
    					return puUrl;
    				}
    			};
    		}
    		else {
    			return java.util.Collections.emptyEnumeration();
    		}
        }
    }
}
