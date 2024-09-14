/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.categorize;

import org.hibernate.Internal;

import org.jboss.logging.Logger;

/**
 * todo : find the proper min/max id range
 *
 * @author Steve Ebersole
 */
@Internal
public interface ModelCategorizationLogging {
	String NAME = "org.hibernate.models.orm";

	Logger MODEL_CATEGORIZATION_LOGGER = Logger.getLogger( NAME );
}
