/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.tuple.entity;

import java.util.Set;

import org.hibernate.bytecode.instrumentation.spi.FieldInterceptor;
import org.hibernate.bytecode.spi.EntityInstrumentationMetadata;
import org.hibernate.bytecode.spi.NotInstrumentedException;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * @author Steve Ebersole
 */
public class NonPojoInstrumentationMetadata implements EntityInstrumentationMetadata {
	private final String entityName;
	private final String errorMsg;

	public NonPojoInstrumentationMetadata(String entityName) {
		this.entityName = entityName;
		this.errorMsg = "Entity [" + entityName + "] is non-pojo, and therefore not instrumented";
	}

	@Override
	public String getEntityName() {
		return entityName;
	}

	@Override
	public boolean isInstrumented() {
		return false;
	}

	@Override
	public FieldInterceptor extractInterceptor(Object entity) throws NotInstrumentedException {
		throw new NotInstrumentedException( errorMsg );
	}

	@Override
	public FieldInterceptor injectInterceptor(
			Object entity, String entityName, Set uninitializedFieldNames, SessionImplementor session)
			throws NotInstrumentedException {
		throw new NotInstrumentedException( errorMsg );
	}
}
