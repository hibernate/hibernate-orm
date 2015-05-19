/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
