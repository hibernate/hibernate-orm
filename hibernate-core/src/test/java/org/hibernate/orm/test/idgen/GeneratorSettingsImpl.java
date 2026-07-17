/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.internal.GeneratorBinder;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.mapping.GeneratorSettings;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.property.access.spi.PropertyAccessStrategyResolver;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.dialect.Dialect;
import org.hibernate.generator.Generator;

import static org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl.fromExplicit;

/**
 * @author Steve Ebersole
 */
public class GeneratorSettingsImpl implements GeneratorSettings {
	private final String defaultCatalog;
	private final String defaultSchema;
	private final SqlStringGenerationContext sqlStringGenerationContext;

	public GeneratorSettingsImpl(Metadata domainModel) {
		final Database database = domainModel.getDatabase();
		final Namespace defaultNamespace = database.getDefaultNamespace();
		final Namespace.Name defaultNamespaceName = defaultNamespace.getName();

		defaultCatalog = defaultNamespaceName.catalog() == null
				? ""
				: defaultNamespaceName.catalog().render( database.getDialect() );
		defaultSchema = defaultNamespaceName.schema() == null
				? ""
				: defaultNamespaceName.schema().render( database.getDialect() );

		sqlStringGenerationContext = fromExplicit(
				database.getJdbcEnvironment(),
				database,
				defaultCatalog,
				defaultSchema
		);
	}

	public static Generator createIdentifierGenerator(
			KeyValue identifierValue,
			Dialect dialect,
			RootClass rootClass,
			Property property,
			Metadata domainModel) {
		return createIdentifierGenerator(
				identifierValue,
				dialect,
				rootClass,
				property,
				new GeneratorSettingsImpl( domainModel ),
				domainModel
		);
	}

	public static Generator createIdentifierGenerator(
			KeyValue identifierValue,
			Dialect dialect,
			RootClass rootClass,
			Property property,
			GeneratorSettings defaults,
			Metadata domainModel) {
		final Database database = domainModel.getDatabase();
		final ServiceRegistry serviceRegistry = database.getServiceRegistry();
		return GeneratorBinder.createIdentifierGenerator(
				identifierValue,
				dialect,
				rootClass,
				property,
				defaults,
				database,
				serviceRegistry,
				serviceRegistry.requireService( PropertyAccessStrategyResolver.class )
		);
	}

	@Override
	public String getDefaultCatalog() {
		return defaultCatalog;
	}

	@Override
	public String getDefaultSchema() {
		return defaultSchema;
	}

	@Override
	public SqlStringGenerationContext getSqlStringGenerationContext() {
		return sqlStringGenerationContext;
	}
}
