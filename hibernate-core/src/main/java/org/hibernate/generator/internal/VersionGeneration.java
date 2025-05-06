/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.generator.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.metamodel.mapping.EntityVersionMapping;

import java.util.EnumSet;

import static org.hibernate.engine.internal.Versioning.increment;
import static org.hibernate.engine.internal.Versioning.seed;
import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.generator.EventTypeSets.INSERT_AND_UPDATE;

/**
 * A default {@link org.hibernate.generator.Generator} for {@link jakarta.persistence.Version @Version}
 * properties. This implementation simply delegates back to:
 * <ul>
 * <li>{@link org.hibernate.type.descriptor.java.VersionJavaType#seed} to seed an initial version, and
 * <li>{@link org.hibernate.type.descriptor.java.VersionJavaType#next} to increment a version.
 * </ul>
 * <p>
 * Thus, this implementation reproduces the "classic" behavior of Hibernate. A custom generator specified
 * using a {@linkplain org.hibernate.annotations.ValueGenerationType generator annotation} will override
 * this implementation, allowing customized versioning.
 *
 * @author Gavin King
 */
public class VersionGeneration implements BeforeExecutionGenerator {
	private final EntityVersionMapping versionMapping;

	public VersionGeneration(EntityVersionMapping versionMapping) {
		this.versionMapping = versionMapping;
	}

	@Override
	public EnumSet<EventType> getEventTypes() {
		return INSERT_AND_UPDATE;
	}

	@Override
	public Object generate(SharedSessionContractImplementor session, Object owner, Object current, EventType eventType) {
		return eventType == INSERT
				? seed( versionMapping, session )
				: increment( current, versionMapping, session );
	}
}
