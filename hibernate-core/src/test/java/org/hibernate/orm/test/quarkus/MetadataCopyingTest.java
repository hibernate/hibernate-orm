/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.quarkus;

import org.hibernate.boot.internal.MetadataImpl;

/**
 * Quarkus needs to be able to make a deep copy of MetadataImpl;
 * for this to be possible, it needs to expose some of its state.
 * Adding a test to help remembering about this requirement: we
 * only need this to compile so that it serves as a reminder.
 */
public class MetadataCopyingTest {

	public void copyGettersAreExposed() {
		MetadataImpl existingInstance = fetchSomehowOldCopy();
		//Test that for each constructor parameter needed to create a new MetadataImpl,
		//we can actually read the matching state from an existing MetadataImpl instance.
		MetadataImpl newcopy = new MetadataImpl(
				existingInstance.getUUID(),
				existingInstance.getMetadataBuildingOptions(),
				existingInstance.getEntityBindingMap(),
				existingInstance.getComposites(),
				existingInstance.getGenericComponentsMap(),
				existingInstance.getEmbeddableDiscriminatorTypesMap(),
				existingInstance.getMappedSuperclassMap(),
				existingInstance.getCollectionBindingMap(),
				existingInstance.getTypeDefinitionMap(),
				existingInstance.getFilterDefinitions(),
				existingInstance.getFetchProfileMap(),
				existingInstance.getImports(),
				existingInstance.getIdGeneratorDefinitionMap(),
				existingInstance.getNamedQueryMap(),
				existingInstance.getNamedNativeQueryMap(),
				existingInstance.getNamedProcedureCallMap(),
				existingInstance.getSqlResultSetMappingMap(),
				existingInstance.getNamedEntityGraphs(),
				existingInstance.getSqlFunctionMap(),
				existingInstance.getDatabase(),
				existingInstance.getBootstrapContext()
		);
	}

	private MetadataImpl fetchSomehowOldCopy() {
		return null;
	}

}
