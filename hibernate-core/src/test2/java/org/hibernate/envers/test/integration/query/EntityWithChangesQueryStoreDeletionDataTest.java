/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.query;

import java.util.Map;

import org.hibernate.envers.configuration.EnversSettings;

import org.hibernate.testing.TestForIssue;

/**
 * @author Chris Cranford
 */
@TestForIssue( jiraKey = "HHH-8058" )
public class EntityWithChangesQueryStoreDeletionDataTest extends AbstractEntityWithChangesQueryTest {
	@Override
	protected void addConfigOptions(Map options) {
		options.put( EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, Boolean.TRUE );
		options.put( EnversSettings.STORE_DATA_AT_DELETE, Boolean.TRUE );
		super.addConfigOptions( options );
	}
}
