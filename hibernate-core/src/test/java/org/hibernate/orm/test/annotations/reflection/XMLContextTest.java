/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.annotations.reflection;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.model.internal.XMLContext;
import org.hibernate.orm.test.internal.util.xml.XMLMappingHelper;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.boot.BootstrapContextImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Bernard
 */
@TestForIssue(jiraKey = "HHH-14529")
public class XMLContextTest {

	private BootstrapContextImpl bootstrapContext;

	@BeforeEach
	public void init() {
		bootstrapContext = new BootstrapContextImpl();
	}

	@AfterEach
	public void destroy() {
		bootstrapContext.close();
	}

	@Test
	public void testAll() throws Exception {
		XMLMappingHelper xmlHelper = new XMLMappingHelper();
		final XMLContext context = new XMLContext( bootstrapContext );

		JaxbEntityMappingsImpl mappings = xmlHelper.readOrmXmlMappings(
				"org/hibernate/orm/test/annotations/reflection/orm.xml" );
		context.addDocument( mappings );
	}
}
