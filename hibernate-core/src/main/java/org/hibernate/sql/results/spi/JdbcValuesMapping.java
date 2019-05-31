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
import java.util.stream.Collectors;

/**
 * The "resolved" form of {@link JdbcValuesMappingProducer} providing access
 * to resolved JDBC results ({@link SqlSelection}) descriptors and resolved
 * domain results ({@link DomainResult}) descriptors.
 *
 * @see JdbcValuesMappingProducer#resolve
 *
 * @author Steve Ebersole
 */
public interface JdbcValuesMapping {
	/**
	 * The JDBC selection descriptors.  Used to read ResultSet values and build
	 * the "JDBC values array"
	 */
	Set<SqlSelection> getSqlSelections();

	List<DomainResult<?>> getDomainResults();

	default List<DomainResultAssembler<?>> resolveAssemblers(
			Consumer<Initializer> initializerConsumer,
			AssemblerCreationState creationState) {
		return getDomainResults().stream()
				.map( domainResult -> domainResult.createResultAssembler( initializerConsumer, creationState ) )
				.collect( Collectors.toList() );
	}
}
