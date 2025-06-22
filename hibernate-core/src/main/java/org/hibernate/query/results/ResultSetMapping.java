/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * JdbcValuesMappingProducer implementation based on a graph of {@linkplain ResultBuilder}
 * and {@linkplain FetchBuilder} reference. Used to model result-set mappings from:<ul>
 *    <li>{@link jakarta.persistence.SqlResultSetMapping}</li>
 *    <li>{@code orm.xml}</li>
 *    <li>{@code mapping.xml}</li>
 *    <li>{@code hbm.xml}</li>
 *    <li>
 *        Hibernate-specific APIs:<ul>
 *            <li>{@link NativeQuery#addScalar}</li>
 *            <li>{@link NativeQuery#addEntity}</li>
 *            <li>{@link NativeQuery#addJoin}</li>
 *            <li>{@link NativeQuery#addFetch}</li>
 *            <li>{@link NativeQuery#addRoot}</li>
 *        </ul>
 *    </li>
 * </ul>
 *
 * @see NativeQuery
 * @see org.hibernate.procedure.ProcedureCall
 * @see jakarta.persistence.StoredProcedureQuery
 *
 * @author Steve Ebersole
 */
@Incubating
public interface ResultSetMapping extends JdbcValuesMappingProducer {
	/**
	 * An identifier for the mapping
	 */
	String getMappingIdentifier();

	/**
	 * Indicates whether the mapping is dynamic per {@link ResultSetMapping}
	 */
	boolean isDynamic();

	/**
	 * The number of result builders currently associated with this mapping
	 */
	int getNumberOfResultBuilders();

	/**
	 * The result builders currently associated with this mapping
	 */
	List<ResultBuilder> getResultBuilders();

	/**
	 * Visit each result builder
	 */
	void visitResultBuilders(BiConsumer<Integer, ResultBuilder> resultBuilderConsumer);

	/**
	 * Visit the "legacy" fetch builders.
	 * <p/>
	 * Historically these mappings in Hibernate were defined such that results and fetches are
	 * unaware of each other.  So while {@link ResultBuilder} encapsulates the fetches (see
	 * {@link ResultBuilder#visitFetchBuilders}), fetches defined in the legacy way are unassociated
	 * to their "parent".
	 */
	void visitLegacyFetchBuilders(Consumer<LegacyFetchBuilder> resultBuilderConsumer);

	/**
	 * Add a builder
	 */
	void addResultBuilder(ResultBuilder resultBuilder);

	/**
	 * Add a legacy fetch builder
	 */
	void addLegacyFetchBuilder(LegacyFetchBuilder fetchBuilder);

	/**
	 * Create a memento from this mapping.
	 */
	NamedResultSetMappingMemento toMemento(String name);

	static ResultSetMapping resolveResultSetMapping(String name, SessionFactoryImplementor sessionFactory) {
		return resolveResultSetMapping( name, false, sessionFactory );
	}

	static ResultSetMapping resolveResultSetMapping(String name, boolean isDynamic, SessionFactoryImplementor sessionFactory) {
		return sessionFactory.getJdbcValuesMappingProducerProvider()
				.buildResultSetMapping( name, isDynamic, sessionFactory );
	}
}
