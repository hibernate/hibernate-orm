/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
