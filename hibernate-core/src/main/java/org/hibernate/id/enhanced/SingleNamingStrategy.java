/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.enhanced;

import java.util.Map;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.service.ServiceRegistry;

import static org.hibernate.id.enhanced.TableGenerator.DEF_TABLE;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;

/**
 * An {@link ImplicitDatabaseObjectNamingStrategy} using a single structure for all
 * implicit names:
 * <ul>
 *     <li>{@value ImplicitDatabaseObjectNamingStrategy#DEF_SEQUENCE} for sequences
 *     <li>{@value TableGenerator#DEF_TABLE} for tables
 * </ul>
 *
 * @author Andrea Boriero
 */
public class SingleNamingStrategy implements ImplicitDatabaseObjectNamingStrategy {
	public static final String STRATEGY_NAME = "single";

	@Override
	public QualifiedName determineSequenceName(
			Identifier catalogName,
			Identifier schemaName,
			Map<?, ?> configValues,
			ServiceRegistry serviceRegistry) {
		DEPRECATION_LOGGER.logDeprecatedImplicitDatabaseObjectNamingStrategyMethod();
		return new QualifiedSequenceName(
				catalogName,
				schemaName,
				serviceRegistry.requireService( JdbcEnvironment.class )
						.getIdentifierHelper()
						.toIdentifier( DEF_SEQUENCE )
		);
	}

	@Override
	public QualifiedName determineSequenceName(
			Identifier catalogName,
			Identifier schemaName,
			Map<?, ?> configValues,
			Database database) {

		return new QualifiedSequenceName(
				catalogName,
				schemaName,
				database.toIdentifier( DEF_SEQUENCE )
		);
	}

	public QualifiedName determineTableName(
			Identifier catalogName,
			Identifier schemaName,
			Map<?, ?> configValues,
			ServiceRegistry serviceRegistry) {
		DEPRECATION_LOGGER.logDeprecatedImplicitDatabaseObjectNamingStrategyMethod();
		return new QualifiedTableName(
				catalogName,
				schemaName,
				serviceRegistry.requireService( JdbcEnvironment.class )
						.getIdentifierHelper()
						.toIdentifier( DEF_TABLE )
		);
	}

	public QualifiedName determineTableName(
			Identifier catalogName,
			Identifier schemaName,
			Map<?, ?> configValues,
			Database database) {
		return new QualifiedTableName(
				catalogName,
				schemaName,
				database.toIdentifier( DEF_TABLE )
		);
	}
}
