/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.List;
import java.util.Set;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.JdbcValuesMapping;
import org.hibernate.sql.results.spi.JdbcValuesMappingProducer;
import org.hibernate.sql.results.spi.JdbcValuesMetadata;

/**
 * ResultSetMapping for handling selections for a {@link org.hibernate.query.NativeQuery}
 * which (partially) defines its result mappings.  At the very least we will need
 * to resolve the `columnAlias` to its ResultSet index.  For scalar results
 * ({@link javax.persistence.ColumnResult}) we may additionally need to resolve
 * its "type" for reading.
 *
 * Specifically needs to
 * @author Steve Ebersole
 */
public class JdbcValuesMappingProducerDefined implements JdbcValuesMappingProducer {
	private final Set<SqlSelection> selections;
	private final List<DomainResult> domainResults;

	public JdbcValuesMappingProducerDefined(Set<SqlSelection> selections, List<DomainResult> domainResults) {
		this.selections = selections;
		this.domainResults = domainResults;
	}

	@Override
	public JdbcValuesMapping resolve(
			JdbcValuesMetadata jdbcResultsMetadata,
			SessionFactoryImplementor sessionFactory) {

		throw new NotYetImplementedFor6Exception( getClass() );

//		for ( SqlSelection sqlSelection : selections ) {
//			sqlSelection.prepare( jdbcResultsMetadata, sessionFactory );
//		}
//
//		return new StandardResultSetMapping( selections, domainResults );
	}
}
