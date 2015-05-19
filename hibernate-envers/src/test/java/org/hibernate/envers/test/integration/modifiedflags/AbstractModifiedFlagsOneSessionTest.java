/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.modifiedflags;

import org.hibernate.cfg.Configuration;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.test.AbstractOneSessionTest;

/**
 * Base test for modified flags feature
 *
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public abstract class AbstractModifiedFlagsOneSessionTest extends
														  AbstractOneSessionTest {

	@Override
	protected void addProperties(Configuration configuration) {
		super.addProperties( configuration );
		if ( forceModifiedFlags() ) {
			configuration.setProperty( EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, "true" );
		}
	}

	public boolean forceModifiedFlags() {
		return true;
	}

}
