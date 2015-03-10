/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.xml.ejb3;

import org.hibernate.InvalidMappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.internal.util.xml.UnsupportedOrmXsdVersionException;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.fail;

@TestForIssue(jiraKey = "HHH-6271")
public class NonExistentOrmVersionTest extends BaseUnitTestCase {
	@Test
	public void testNonExistentOrmVersion() {
		try {
			new MetadataSources()
					.addResource( "org/hibernate/test/annotations/xml/ejb3/orm5.xml" )
					.buildMetadata();
			fail( "Expecting failure due to unsupported xsd version" );
		}
		catch ( InvalidMappingException expected ) {
		}
		catch ( UnsupportedOrmXsdVersionException expected ) {
		}
	}
}
