/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.reflection;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.model.internal.XMLContext;
import org.hibernate.orm.test.internal.util.xml.XMLMappingHelper;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.boot.BootstrapContextImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Bernard
 */
@JiraKey(value = "HHH-14529")
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
