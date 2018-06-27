/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idgen.foreign;

import java.util.Map;

import org.hibernate.cfg.Environment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaBootstrap;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-12738")
public class ForeignGeneratorJtaTest extends ForeignGeneratorResourceLocalTest {

	@Override
	protected void addConfigOptions(Map options) {
		TestingJtaBootstrap.prepare( options );
		options.put( Environment.TRANSACTION_COORDINATOR_STRATEGY, "jta" );
	}
}
