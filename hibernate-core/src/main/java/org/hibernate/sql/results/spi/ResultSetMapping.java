/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The "resolved" form of {@link ResultSetMappingDescriptor} providing access
 * to resolved JDBC results ({@link SqlSelection}) descriptors and resolved
 * domain results ({@link DomainResult}) descriptors.
 *
 * @see ResultSetMappingDescriptor#resolve
 *
 * @author Steve Ebersole
 */
public interface ResultSetMapping {
	/**
	 * The JDBC selection descriptors.  Used to read ResultSet values and build
	 * the "JDBC values array"
	 */
	Set<SqlSelection> getSqlSelections();

	List<DomainResult> getDomainResults();

	List<DomainResultAssembler> resolveAssemblers(
			Consumer<Initializer> initializerConsumer,
			AssemblerCreationState creationState,
			AssemblerCreationContext creationContext);
}
