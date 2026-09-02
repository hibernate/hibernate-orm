/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.jdbc.spi;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.results.spi.ResultSetMapping;
import org.hibernate.service.Service;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;

/**
 * Pluggable contract for providing custom {@link JdbcValuesMappingProducer} implementations.
 * This is intended for use by hibernate-reactive to provide its custom implementations.
 *
 * @see org.hibernate.boot.registry.StandardServiceRegistryBuilder#addService(Class, org.hibernate.service.Service)
 *
 * @author Steve Ebersole
 */
@Incubating
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT, org.hibernate.SPI.Role.SUPPLY })
public interface JdbcValuesMappingProducerProvider extends Service {
	/**
	 * Provide the JdbcValuesMappingProducer to use for the given SQL AST
	 *
	 * @see JdbcValuesMappingProducer
	 */
	@org.hibernate.SPI(org.hibernate.SPI.Role.SUPPLY)
	JdbcValuesMappingProducer buildMappingProducer(SelectStatement sqlAst, SessionFactoryImplementor sessionFactory);

	/**
	 * Provide a dynamically built JdbcValuesMappingProducer
	 *
	 * @see ResultSetMapping
	 */
	@org.hibernate.SPI(org.hibernate.SPI.Role.SUPPLY)
	ResultSetMapping buildResultSetMapping(String name, boolean isDynamic, SessionFactoryImplementor sessionFactory);
}
