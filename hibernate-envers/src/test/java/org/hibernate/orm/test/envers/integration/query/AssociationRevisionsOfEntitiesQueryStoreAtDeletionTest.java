/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.envers.integration.query;

import java.util.Map;

import org.hibernate.envers.configuration.EnversSettings;

import org.hibernate.testing.orm.junit.JiraKey;

/**
 * @author Chris Cranford
 */
@JiraKey( value = "HHH-13817" )
public class AssociationRevisionsOfEntitiesQueryStoreAtDeletionTest extends AssociationRevisionsOfEntitiesQueryTest {
	@Override
	protected void addSettings(Map<String,Object> settings) {
		super.addSettings( settings );
		settings.put( EnversSettings.STORE_DATA_AT_DELETE, true );
	}
}
