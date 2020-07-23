/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import java.util.function.BiConsumer;

import org.hibernate.Incubating;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;

/**
 * Acts as the {@link JdbcValuesMappingProducer} for {@link org.hibernate.query.NativeQuery}
 * or {@link org.hibernate.procedure.ProcedureCall} / {@link javax.persistence.StoredProcedureQuery}
 * instances.
 *
 * Collects builders for results and fetches and manages resolving them via JdbcValuesMappingProducer
 *
 * @see org.hibernate.query.NativeQuery#addScalar
 * @see org.hibernate.query.NativeQuery#addEntity
 * @see org.hibernate.query.NativeQuery#addJoin
 * @see org.hibernate.query.NativeQuery#addFetch
 * @see org.hibernate.query.NativeQuery#addRoot
 *
 * @author Steve Ebersole
 */
@Incubating
public interface ResultSetMapping extends JdbcValuesMappingProducer {
	int getNumberOfResultBuilders();

	void visitResultBuilders(BiConsumer<Integer, ResultBuilder> resultBuilderConsumer);

	void addResultBuilder(ResultBuilder resultBuilder);
	void addLegacyFetchBuilder(LegacyFetchBuilder fetchBuilder);

	NamedResultSetMappingMemento toMemento(String name);
}
