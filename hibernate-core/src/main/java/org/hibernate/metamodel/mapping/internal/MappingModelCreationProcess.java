/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Map;

import org.hibernate.metamodel.mapping.MappingModelCreationContext;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Steve Ebersole
 */
public class MappingModelCreationProcess {
	/**
	 * Triggers creation of the mapping model
	 */
	public static void process(
			Map<String,EntityPersister> entityPersisterMap,
			MappingModelCreationContext creationContext) {
		final MappingModelCreationProcess process = new MappingModelCreationProcess(
				entityPersisterMap,
				creationContext
		);
		process.execute();
	}

	private final Map<String,EntityPersister> entityPersisterMap;

	private final MappingModelCreationContext creationContext;

	private String currentlyProcessingRole;

	private MappingModelCreationProcess(
			Map<String,EntityPersister> entityPersisterMap,
			MappingModelCreationContext creationContext) {
		this.entityPersisterMap = entityPersisterMap;
		this.creationContext = creationContext;
	}

	public MappingModelCreationContext getCreationContext() {
		return creationContext;
	}

	public EntityPersister getEntityPersister(String name) {
		return entityPersisterMap.get( name );
	}

	/**
	 * Instance-level trigger for {@link #process}
	 */
	private void execute() {
		for ( EntityPersister entityPersister : entityPersisterMap.values() ) {
			entityPersister.linkWithSuperType( this );
		}

		for ( EntityPersister entityPersister : entityPersisterMap.values() ) {
			currentlyProcessingRole = entityPersister.getEntityName();

			entityPersister.prepareMappingModel( this );
		}
	}

	public <T extends ModelPart> T processSubPart(
			String localName,
			SubPartMappingProducer<T> subPartMappingProducer) {
		assert currentlyProcessingRole != null;

		final String initialRole = currentlyProcessingRole;
		currentlyProcessingRole = currentlyProcessingRole + '#' + localName;

		try {
			return subPartMappingProducer.produceSubMapping( currentlyProcessingRole, this );
		}
		finally {
			currentlyProcessingRole = initialRole;
		}
	}

	/**
	 * Explicitly defined to better control (for now) the args
	 */
	@FunctionalInterface
	public interface SubPartMappingProducer<T> {
		T produceSubMapping(String role, MappingModelCreationProcess creationProcess);
	}
}
