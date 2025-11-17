/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.xml;

import org.hibernate.cfg.Configuration;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

/**
 * @author Etienne Miret
 */
public class OrmXmlParseTest {

	@Test
	@JiraKey(value = "HHH-9247")
	void parseNamedAttributeNode() {
		final Configuration cfg = new Configuration();
		cfg.addURL( getClass().getResource( "orm.xml" ) );
	}

}
