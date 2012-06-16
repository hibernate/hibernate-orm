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

import java.io.InputStream;

import org.junit.Test;

import org.hibernate.MappingException;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertTrue;

@TestForIssue(jiraKey = "HHH-6271")
public class NonExistentOrmVersionTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testNonExistentOrmVersion() {
		try {
			Configuration config = buildConfiguration();
			String xmlFileName = "org/hibernate/test/annotations/xml/ejb3/orm5.xml";
			InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( xmlFileName );
			config.addInputStream( is );
			config.buildMappings();
		}
		catch ( MappingException mappingException ) {
			Throwable cause = mappingException.getCause();
			assertTrue(
					cause.getMessage().contains(
							"Value '3.0' of attribute 'version' of element 'entity-mappings' is not valid"
					)
			);
		}
	}
}
