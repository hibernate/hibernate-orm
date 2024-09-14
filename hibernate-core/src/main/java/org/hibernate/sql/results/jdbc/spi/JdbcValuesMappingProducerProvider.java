/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.results.jdbc.spi;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.results.ResultSetMapping;
import org.hibernate.service.Service;
import org.hibernate.sql.ast.tree.select.SelectStatement;

/**
 * Pluggable contract for providing custom {@link JdbcValuesMappingProducer} implementations.
 * This is intended for use by hibernate-reactive to provide its custom implementations.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface JdbcValuesMappingProducerProvider extends Service {
	/**
	 * Provide the JdbcValuesMappingProducer to use for the given SQL AST
	 */
	JdbcValuesMappingProducer buildMappingProducer(SelectStatement sqlAst, SessionFactoryImplementor sessionFactory);

	/**
	 * Provide a dynamically built JdbcValuesMappingProducer
	 */
	ResultSetMapping buildResultSetMapping(String name, boolean isDynamic, SessionFactoryImplementor sessionFactory);
}
