/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.Incubating;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;

/**
 * Acts as the {@link JdbcValuesMappingProducer} for {@link org.hibernate.query.NativeQuery}
 * or {@link org.hibernate.procedure.ProcedureCall} / {@link jakarta.persistence.StoredProcedureQuery}
 * instances.
 *
 * Can be defined<ul>
 *     <li>
 *         statically via {@link jakarta.persistence.SqlResultSetMapping} or `hbm.xml` mapping
 *     </li>
 *     <li>
 *         dynamically via Hibernate-specific APIs:<ul>
 *             <li>{@link org.hibernate.query.NativeQuery#addScalar}</li>
 *             <li>{@link org.hibernate.query.NativeQuery#addEntity}</li>
 *             <li>{@link org.hibernate.query.NativeQuery#addJoin}</li>
 *             <li>{@link org.hibernate.query.NativeQuery#addFetch}</li>
 *             <li>{@link org.hibernate.query.NativeQuery#addRoot}</li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * @author Steve Ebersole
 */
@Incubating
public interface ResultSetMapping extends JdbcValuesMappingProducer {
	int getNumberOfResultBuilders();

	void visitResultBuilders(BiConsumer<Integer, ResultBuilder> resultBuilderConsumer);
	void visitLegacyFetchBuilders(Consumer<DynamicFetchBuilderLegacy> resultBuilderConsumer);

	void addResultBuilder(ResultBuilder resultBuilder);
	void addLegacyFetchBuilder(DynamicFetchBuilderLegacy fetchBuilder);

	NamedResultSetMappingMemento toMemento(String name);
}
