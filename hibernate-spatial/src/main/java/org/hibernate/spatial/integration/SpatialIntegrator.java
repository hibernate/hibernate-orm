/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2007-2012 Geovise BVBA
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.hibernate.spatial.integration;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.TypeContributingIntegrator;
import org.hibernate.metamodel.spi.source.MetadataImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.hibernate.spatial.GeometryType;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 7/27/11
 */
public class SpatialIntegrator implements TypeContributingIntegrator {

	private static final String UNLOCK_ERROR_MSG = "SpatialIntegrator failed to unlock BasicTypeRegistry";

	@Override
	public void integrate(Configuration configuration, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		//do nothing
	}

	@Override
	public void integrate(MetadataImplementor metadata, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		//do nothing
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		//do nothing.
	}

    @Override
    public void prepareTypes(MetadataImplementor builder) {
        builder.getTypeResolver().registerTypeOverride(GeometryType.INSTANCE);
    }

}
