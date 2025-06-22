/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;

import java.util.Map;

import org.hibernate.Incubating;
import org.hibernate.service.Service;
import org.hibernate.tool.schema.internal.exec.JdbcContext;

/**
 * Contract for schema management tool integration.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SchemaManagementTool extends Service {
	SchemaCreator getSchemaCreator(Map<String,Object> options);
	SchemaDropper getSchemaDropper(Map<String,Object> options);
	SchemaMigrator getSchemaMigrator(Map<String,Object> options);
	SchemaValidator getSchemaValidator(Map<String,Object> options);
	default SchemaPopulator getSchemaPopulator(Map<String,Object> options) {
		throw new UnsupportedOperationException("Schema populator is not supported by this schema management tool.");
	}
	default SchemaTruncator getSchemaTruncator(Map<String,Object> options) {
		throw new UnsupportedOperationException("Schema truncator is not supported by this schema management tool.");
	}

	/**
	 * This allows to set an alternative implementation for the Database
	 * generation target.
	 * Used by Hibernate Reactive so that it can use the reactive database
	 * access rather than needing a JDBC connection.
	 * @param generationTarget the custom instance to use.
	 */
	void setCustomDatabaseGenerationTarget(GenerationTarget generationTarget);

	ExtractionTool getExtractionTool();

	/**
	 * Resolves the {@linkplain GenerationTarget targets} to which to
	 * send the DDL commands based on configuration
	 */
	default GenerationTarget[] buildGenerationTargets(
			TargetDescriptor targetDescriptor,
			JdbcContext jdbcContext,
			Map<String, Object> options,
			boolean needsAutoCommit) {
		throw new UnsupportedOperationException("Building generation targets is not supported by this schema management tool.");
	}
}
