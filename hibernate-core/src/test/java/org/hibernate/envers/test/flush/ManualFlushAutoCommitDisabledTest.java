/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.flush;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-7017")
@Disabled("Manual FlushMode does not work properly.")
public class ManualFlushAutoCommitDisabledTest extends ManualFlushTest {
	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );
		settings.put( AvailableSettings.AUTOCOMMIT, "false" );
	}
}
