/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.loading.internal;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.Logger;

/**
 * Tracks information about loading of entities specific to a given result set.  These can be hierarchical.
 *
 * @author Steve Ebersole
 */
public class EntityLoadContext {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, EntityLoadContext.class.getName() );

	private final LoadContexts loadContexts;
	private final ResultSet resultSet;
	// todo : need map? the prob is a proper key, right?
	private final List hydratingEntities = new ArrayList( 20 );

	public EntityLoadContext(LoadContexts loadContexts, ResultSet resultSet) {
		this.loadContexts = loadContexts;
		this.resultSet = resultSet;
	}

	void cleanup() {
		if ( !hydratingEntities.isEmpty() ) {
			LOG.hydratingEntitiesCount( hydratingEntities.size() );
		}
		hydratingEntities.clear();
	}


	@Override
	public String toString() {
		return super.toString() + "<rs=" + resultSet + ">";
	}
}
