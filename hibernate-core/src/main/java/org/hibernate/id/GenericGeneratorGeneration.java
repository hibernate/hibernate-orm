/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.boot.model.internal.GeneratorBinder;
import org.hibernate.boot.model.internal.GeneratorParameters;
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.AnnotationBasedGenerator;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.Generator;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.mapping.Value;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.resource.beans.internal.Helper;

/**
 * Generator backing {@link GenericGenerator}.
 *
 * @author Gavin King
 *
 * @since 8.0
 */
@Internal
public class GenericGeneratorGeneration
		implements IdentifierGenerator, OnExecutionGenerator, BulkInsertionCapableIdentifierGenerator,
				AnnotationBasedGenerator<GenericGenerator> {
	private Generator delegate;

	@Internal
	public Generator getDelegate() {
		return delegate;
	}

	@Override
	public void initialize(GenericGenerator annotation, GeneratorCreationContext context) {
		delegate = GeneratorBinder.instantiateGenerator(
				Helper.getBeanContainer( context.getServiceRegistry() ),
				generatorType( annotation, context )
		);

		configure( annotation, context );

		if ( delegate.requiresIdentityColumn() ) {
			setColumnToIdentity( context.getValue() );
		}
	}

	private static Class<? extends Generator> generatorType(
			GenericGenerator annotation,
			GeneratorCreationContext context) {
		final var type = annotation.type();
		if ( type != null ) {
			return type;
		}
		else {
			throw new MappingException( "No generator type specified by @GenericGenerator" );
		}
	}

	private void configure(GenericGenerator annotation, GeneratorCreationContext context) {
		if ( delegate instanceof Configurable configurable ) {
			configurable.configure( context, collectParameters( annotation, context ) );
		}
		if ( delegate instanceof ExportableProducer exportableProducer ) {
			exportableProducer.registerExportables( context.getDatabase() );
		}
		if ( delegate instanceof Configurable configurable ) {
			configurable.initialize( context.getSqlStringGenerationContext() );
		}
	}

	private static Properties collectParameters(
			GenericGenerator annotation,
			GeneratorCreationContext context) {
		final Map<String,Object> configuration = new HashMap<>();
		for ( var parameter : annotation.parameters() ) {
			configuration.put( parameter.name(), parameter.value() );
		}
		return GeneratorParameters.collectParameters(
				context,
				configuration,
				context.getServiceRegistry().requireService( ConfigurationService.class )
		);
	}

	private static void setColumnToIdentity(Value value) {
		if ( value.getColumnSpan() != 1 ) {
			throw new MappingException( "Identity generation requires exactly one column" );
		}
		else {
			value.getColumns().get( 0 ).setIdentity( true );
		}
	}

	@Override
	public boolean generatedOnExecution() {
		return delegate.generatedOnExecution();
	}

	@Override
	public boolean generatedOnExecution(Object entity, SharedSessionContractImplementor session) {
		return delegate.generatedOnExecution( entity, session );
	}

	@Override
	public boolean generatedBeforeExecution(Object entity, SharedSessionContractImplementor session) {
		return delegate.generatedBeforeExecution( entity, session );
	}

	@Override
	public EnumSet<EventType> getEventTypes() {
		return delegate.getEventTypes();
	}

	@Override
	public Class<?> getGeneratedType() {
		return delegate.getGeneratedType();
	}

	@Override
	public boolean allowAssignedIdentifiers() {
		return delegate.allowAssignedIdentifiers();
	}

	@Override
	public boolean allowMutation() {
		return delegate.allowMutation();
	}

	@Override
	public boolean requiresIdentityColumn() {
		return delegate.requiresIdentityColumn();
	}

	@Override
	public Object generate(SharedSessionContractImplementor session, Object object) {
		return delegate instanceof IdentifierGenerator identifierGenerator
				? identifierGenerator.generate( session, object )
				: beforeExecutionGenerator().generate( session, object, null, EventType.INSERT );
	}

	@Override
	public Object generate(
			SharedSessionContractImplementor session,
			Object owner,
			Object currentValue,
			EventType eventType) {
		return beforeExecutionGenerator().generate( session, owner, currentValue, eventType );
	}

	@Override
	public boolean referenceColumnsInSql(Dialect dialect) {
		return onExecutionGenerator().referenceColumnsInSql( dialect );
	}

	@Override
	public boolean referenceColumnsInSql(Dialect dialect, EventType eventType) {
		return onExecutionGenerator().referenceColumnsInSql( dialect, eventType );
	}

	@Override
	public boolean writePropertyValue() {
		return onExecutionGenerator().writePropertyValue();
	}

	@Override
	public boolean writePropertyValue(EventType eventType) {
		return onExecutionGenerator().writePropertyValue( eventType );
	}

	@Override
	public String[] getReferencedColumnValues(Dialect dialect) {
		return onExecutionGenerator().getReferencedColumnValues( dialect );
	}

	@Override
	public String[] getReferencedColumnValues(Dialect dialect, EventType eventType) {
		return onExecutionGenerator().getReferencedColumnValues( dialect, eventType );
	}

	@Override
	public boolean[] getColumnInclusions(Dialect dialect, EventType eventType) {
		return onExecutionGenerator().getColumnInclusions( dialect, eventType );
	}

	@Override
	public InsertGeneratedIdentifierDelegate getGeneratedIdentifierDelegate(EntityPersister persister) {
		return onExecutionGenerator().getGeneratedIdentifierDelegate( persister );
	}

	@Override
	public String[] getUniqueKeyPropertyNames(EntityPersister persister) {
		return onExecutionGenerator().getUniqueKeyPropertyNames( persister );
	}

	@Override
	public boolean supportsBulkInsertionIdentifierGeneration() {
		return delegate instanceof BulkInsertionCapableIdentifierGenerator generator
				&& generator.supportsBulkInsertionIdentifierGeneration();
	}

	@Override
	public String determineBulkInsertionIdentifierGenerationSelectFragment(SqlStringGenerationContext context) {
		return delegate instanceof BulkInsertionCapableIdentifierGenerator generator
				? generator.determineBulkInsertionIdentifierGenerationSelectFragment( context )
				: null;
	}

	private BeforeExecutionGenerator beforeExecutionGenerator() {
		if ( delegate instanceof BeforeExecutionGenerator beforeExecutionGenerator ) {
			return beforeExecutionGenerator;
		}
		throw new HibernateException( "Generator does not generate before execution: " + delegate.getClass().getName() );
	}

	private OnExecutionGenerator onExecutionGenerator() {
		if ( delegate instanceof OnExecutionGenerator onExecutionGenerator ) {
			return onExecutionGenerator;
		}
		throw new HibernateException( "Generator does not generate on execution: " + delegate.getClass().getName() );
	}
}
