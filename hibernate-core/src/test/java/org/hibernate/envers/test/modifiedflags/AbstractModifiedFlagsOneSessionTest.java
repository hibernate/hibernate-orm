/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.modifiedflags;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.test.EnversSingleSessionBasedFunctionalTest;

/**
 * Base test for modified flags feature
 *
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public abstract class AbstractModifiedFlagsOneSessionTest extends EnversSingleSessionBasedFunctionalTest {
	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		if ( forceModifiedFlags() ) {
			settings.put( EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, "true" );
		}
	}

	public boolean forceModifiedFlags() {
		return true;
	}

	@SuppressWarnings("WeakerAccess")
	protected static List<Integer> extractRevisions(List queryResults) {
		final List<Integer> results = new ArrayList<>();
		for ( Object queryResult : queryResults ) {
			results.add( ( (SequenceIdRevisionEntity) ( (Object[]) queryResult )[ 1 ] ).getId() );
		}
		return results;
	}
}
