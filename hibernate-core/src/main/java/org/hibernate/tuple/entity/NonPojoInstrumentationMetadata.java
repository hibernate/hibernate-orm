/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
