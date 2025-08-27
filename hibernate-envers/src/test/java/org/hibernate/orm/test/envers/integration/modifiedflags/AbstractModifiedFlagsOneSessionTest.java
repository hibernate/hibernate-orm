/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import org.hibernate.cfg.Configuration;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.AbstractOneSessionTest;

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
