/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.process.spi;

import java.util.Set;

import org.hibernate.boot.model.source.spi.MetadataSourceProcessor;

/**
 * A no-op implementation of MetadataSourceProcessor.
 * This is useful to replace other processors when they are disabled.
 */
final class NoOpMetadataSourceProcessorImpl implements MetadataSourceProcessor {

	@Override
	public void prepare() {
	}

	@Override
	public void processTypeDefinitions() {
	}

	@Override
	public void processQueryRenames() {
	}

	@Override
	public void processNamedQueries() {
	}

	@Override
	public void processAuxiliaryDatabaseObjectDefinitions() {
	}

	@Override
	public void processIdentifierGenerators() {
	}

	@Override
	public void processFilterDefinitions() {
	}

	@Override
	public void processFetchProfiles() {
	}

	@Override
	public void prepareForEntityHierarchyProcessing() {
	}

	@Override
	public void processEntityHierarchies(Set<String> processedEntityNames) {
	}

	@Override
	public void postProcessEntityHierarchies() {
	}

	@Override
	public void processResultSetMappings() {
	}

	@Override
	public void finishUp() {
	}

}
