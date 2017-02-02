/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.annotations.entityGraph;

import org.hibernate.cfg.Configuration;
import org.hibernate.internal.util.ConfigHelper;

import org.hibernate.testing.TestForIssue;

import org.junit.Test;


/**
 * @author Etienne Miret
 */
public class OrmXmlParseTest {

	@Test
	@TestForIssue(jiraKey = "HHH-9247")
	public void parseNamedAttributeNode() {
		final Configuration cfg = new Configuration();
		cfg.addURL( ConfigHelper.findAsResource( "org/hibernate/test/annotations/entityGraph/orm.xml" ) );
		cfg.buildMappings();
	}

}
