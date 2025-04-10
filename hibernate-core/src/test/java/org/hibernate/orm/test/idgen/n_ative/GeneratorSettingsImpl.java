/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.n_ative;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.mapping.GeneratorSettings;

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
