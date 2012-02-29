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
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.hibernate.spatial.GeometryType;
import org.hibernate.type.TypeResolver;

import java.lang.reflect.Field;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 7/27/11
 */
public class SpatialIntegrator implements Integrator {

	private static final String UNLOCK_ERROR_MSG = "SpatialIntegrator failed to unlock BasicTypeRegistry";

	@Override
	public void integrate(Configuration configuration, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		addType(sessionFactory.getTypeResolver());
	}

	@Override
	public void integrate(MetadataImplementor metadata, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		addType(metadata.getTypeResolver());
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		//do nothing.
	}

	private void addType(TypeResolver typeResolver) {
		unlock(typeResolver);
		typeResolver.registerTypeOverride(GeometryType.INSTANCE);
		lock(typeResolver);
	}

	private void lock(TypeResolver typeResolver) {
		setLocked(typeResolver, true);
	}

	private void unlock(TypeResolver typeResolver) {
		setLocked(typeResolver, false);
	}

	private void setLocked(TypeResolver typeResolver, boolean locked) {
		try {
			Field registryFld = typeResolver.getClass().getDeclaredField("basicTypeRegistry");
			registryFld.setAccessible(true);
			Object registry = registryFld.get(typeResolver);
			Field lockedFld = registry.getClass().getDeclaredField("locked");
			lockedFld.setAccessible(true);
			lockedFld.setBoolean(registry, locked);
			lockedFld.setAccessible(false);
			registryFld.setAccessible(true);
		} catch (NoSuchFieldException e) {
			throw new IllegalStateException(UNLOCK_ERROR_MSG, e);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(UNLOCK_ERROR_MSG, e);
		}

	}
}
