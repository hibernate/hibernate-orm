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
package org.hibernate.bytecode.spi;

import java.util.Set;

import org.hibernate.bytecode.instrumentation.spi.FieldInterceptor;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * Encapsulates bytecode instrumentation information about a particular entity.
 *
 * @author Steve Ebersole
 */
public interface EntityInstrumentationMetadata {
	/**
	 * The name of the entity to which this metadata applies.
	 *
	 * @return The entity name
	 */
	public String getEntityName();

	/**
	 * Has the entity class been bytecode instrumented?
	 *
	 * @return {@code true} indicates the entity class is instrumented for Hibernate use; {@code false}
	 * indicates it is not
	 */
	public boolean isInstrumented();

	/**
	 * Build and inject a field interceptor instance into the instrumented entity.
	 *
	 * @param entity The entity into which built interceptor should be injected
	 * @param entityName The name of the entity
	 * @param uninitializedFieldNames The name of fields marked as lazy
	 * @param session The session to which the entity instance belongs.
	 *
	 * @return The built and injected interceptor
	 *
	 * @throws NotInstrumentedException Thrown if {@link #isInstrumented()} returns {@code false}
	 */
	public FieldInterceptor injectInterceptor(
			Object entity,
			String entityName,
			Set uninitializedFieldNames,
			SessionImplementor session) throws NotInstrumentedException;

	/**
	 * Extract the field interceptor instance from the instrumented entity.
	 *
	 * @param entity The entity from which to extract the interceptor
	 *
	 * @return The extracted interceptor
	 *
	 * @throws NotInstrumentedException Thrown if {@link #isInstrumented()} returns {@code false}
	 */
	public FieldInterceptor extractInterceptor(Object entity) throws NotInstrumentedException;
}
