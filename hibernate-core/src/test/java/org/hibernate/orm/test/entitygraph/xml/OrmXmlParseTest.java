/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
