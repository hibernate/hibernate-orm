/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

import java.util.Set;

/**
 * Defines the steps in processing metadata sources.  The steps are performed
 * in a prerequisite series across all sources.  For example, the basic
 * requirement is custom types, so {@link #processTypeDefinitions()} is performed
 * first across all sources to build a complete set of types.  Then the next steps
 * can be performed.
 *
 * @author Steve Ebersole
 */
public interface MetadataSourceProcessor {
	/**
	 * A general preparation step.  Called first.
	 */
	void prepare();

	/**
	 * Process all custom Type definitions.  This step has no
	 * prerequisites.
	 */
	void processTypeDefinitions();

	/**
	 * Process all explicit query renames (imports).  This step has no
	 * prerequisites.
	 */
	void processQueryRenames();

	/**
	 * Process all "root" named queries.  These are named queries not defined on
	 * a specific entity (which will be handled later during
	 * {@link #processEntityHierarchies}.
	 * <p/>
	 * This step has no prerequisites.  The returns associated with named native
	 * queries can depend on entity binding being complete, but those are handled
	 * later during {@link #processResultSetMappings()}.
	 */
	void processNamedQueries();

	/**
	 * Process all {@link org.hibernate.mapping.AuxiliaryDatabaseObject} definitions.
	 * <p/>
	 * This step has no prerequisites.
	 */
	void processAuxiliaryDatabaseObjectDefinitions();

	/**
	 * Process all custom identifier generator declarations,
	 * <p/>
	 * Depends on {@link #processTypeDefinitions()}
	 */
	void processIdentifierGenerators();

	/**
	 * Process all filter definitions.
	 * <p/>
	 * This step depends on {@link #processTypeDefinitions()}
	 */
	void processFilterDefinitions();

	/**
	 * Process all fetch profiles.
	 * <p/>
	 * todo : does this step depend on any others??
	 */
	void processFetchProfiles();

	void prepareForEntityHierarchyProcessing();

	void processEntityHierarchies(Set<String> processedEntityNames);

	void postProcessEntityHierarchies();

	/**
	 * Process ResultSet mappings for native queries.  At the moment, this
	 * step has {@link #processEntityHierarchies} as a prerequisite because
	 * the parsing of the returns access the entity bindings.
	 */
	void processResultSetMappings();

	/**
	 * General finish up step.  Called last.
	 */
	void finishUp();
}
