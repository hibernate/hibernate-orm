/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.flush;

import java.util.Map;

import org.hibernate.testing.TestForIssue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-7017")
public class ManualFlushAutoCommitDisabled extends ManualFlush {
	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( "hibernate.connection.autocommit", "false" );
	}
}
