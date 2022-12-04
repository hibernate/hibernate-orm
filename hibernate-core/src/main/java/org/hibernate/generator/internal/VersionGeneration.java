/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.generator.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.InMemoryGenerator;
import org.hibernate.metamodel.mapping.EntityVersionMapping;

import java.util.EnumSet;

import static org.hibernate.engine.internal.Versioning.increment;
import static org.hibernate.engine.internal.Versioning.seed;
import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.generator.EventTypeSets.INSERT_AND_UPDATE;

/**
 * @author Gavin King
 */
public class VersionGeneration implements InMemoryGenerator {
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
