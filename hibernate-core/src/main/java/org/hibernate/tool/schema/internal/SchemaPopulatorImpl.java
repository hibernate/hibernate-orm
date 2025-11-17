/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.Internal;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.dialect.Dialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.GenerationTarget;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaPopulator;
import org.hibernate.tool.schema.spi.SqlScriptCommandExtractor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.tool.schema.internal.Helper.interpretFormattingEnabled;

/**
 * Basic implementation of {@link SchemaPopulator}.
 *
 * @author Gavin King
 */
public class SchemaPopulatorImpl extends AbstractSchemaPopulator implements SchemaPopulator {

	private final HibernateSchemaManagementTool tool;

	public SchemaPopulatorImpl(HibernateSchemaManagementTool tool) {
		this.tool = tool;
	}

	public SchemaPopulatorImpl(ServiceRegistry serviceRegistry) {
		if ( serviceRegistry.getService( SchemaManagementTool.class )
				instanceof HibernateSchemaManagementTool schemaManagementTool ) {
			tool = schemaManagementTool;
		}
		else {
			tool = new HibernateSchemaManagementTool();
			tool.injectServices( (ServiceRegistryImplementor) serviceRegistry );
		}
	}

	@Override
	public void doPopulation(ExecutionOptions options, TargetDescriptor targetDescriptor) {
		if ( !targetDescriptor.getTargetTypes().isEmpty() ) {
			final var configuration = options.getConfigurationValues();
			final var context = tool.resolveJdbcContext( configuration );
			doPopulation( context.getDialect(), options,
					tool.buildGenerationTargets( targetDescriptor, context, configuration, true ) );
		}
	}

	@Internal
	public void doPopulation(Dialect dialect, ExecutionOptions options, GenerationTarget... targets) {
		for ( var target : targets ) {
			target.prepare();
		}

		try {
			performPopulation( dialect, options, targets );
		}
		finally {
			for ( var target : targets ) {
				try {
					target.release();
				}
				catch (Exception e) {
					CORE_LOGGER.problemReleasingGenerationTarget( target, e );
				}
			}
		}
	}

	private void performPopulation(
			Dialect dialect,
			ExecutionOptions options,
			GenerationTarget... targets) {
		final boolean format = interpretFormattingEnabled( options.getConfigurationValues() );
		final var commandExtractor = tool.getServiceRegistry().getService( SqlScriptCommandExtractor.class );
		applyImportSources( options, commandExtractor, format, dialect, targets );
	}

	@Override
	ClassLoaderService getClassLoaderService() {
		return tool.getServiceRegistry().getService( ClassLoaderService.class );
	}

}
