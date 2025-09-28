/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id;

import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import org.hibernate.boot.model.relational.Database;
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
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.id.uuid.UuidGenerator;
import org.hibernate.persister.entity.EntityPersister;

import java.lang.reflect.Member;
import java.util.EnumSet;
import java.util.Properties;

import static org.hibernate.boot.model.internal.GeneratorParameters.collectParameters;
import static org.hibernate.id.IdentifierGenerator.GENERATOR_NAME;
import static org.hibernate.id.OptimizableGenerator.INCREMENT_PARAM;

/**
 * Generator that picks a strategy based on the {@linkplain Dialect#getNativeValueGenerationStrategy() dialect}.
 *
 * @see org.hibernate.annotations.NativeGenerator
 * @since 7.0
 *
 * @author Steve Ebersole
 */
public class NativeGenerator
		implements OnExecutionGenerator, BeforeExecutionGenerator, Configurable, ExportableProducer,
						AnnotationBasedGenerator<org.hibernate.annotations.NativeGenerator> {
	private GenerationType generationType;
	private org.hibernate.annotations.NativeGenerator annotation;
	private Generator dialectNativeGenerator;

	public GenerationType getGenerationType() {
		return generationType;
	}

	@Override
	public EnumSet<EventType> getEventTypes() {
		return dialectNativeGenerator.getEventTypes();
	}

	@Override
	public boolean generatedOnExecution() {
		return dialectNativeGenerator.generatedOnExecution();
	}

	@Override
	public void initialize(
			org.hibernate.annotations.NativeGenerator annotation,
			Member member,
			GeneratorCreationContext context) {
		this.annotation = annotation;
		generationType =
				context.getDatabase().getDialect()
						.getNativeValueGenerationStrategy();
		switch ( generationType ) {
			case TABLE: {
				dialectNativeGenerator = new TableGenerator();
				break;
			}
			case IDENTITY: {
				dialectNativeGenerator = new IdentityGenerator();
				context.getProperty().getValue().getColumns().get( 0 ).setIdentity( true );
				break;
			}
			case UUID: {
				dialectNativeGenerator = new UuidGenerator( context.getType().getReturnedClass() );
				break;
			}
			default: {
				assert generationType == GenerationType.AUTO || generationType == GenerationType.SEQUENCE;
				dialectNativeGenerator = new SequenceStyleGenerator();
			}
		}
	}

	@Override
	public void configure(GeneratorCreationContext creationContext, Properties parameters) {
		if ( dialectNativeGenerator instanceof SequenceStyleGenerator sequenceStyleGenerator ) {
			applyProperties( parameters, annotation.sequenceForm(), creationContext );
			sequenceStyleGenerator.configure( creationContext, parameters );
		}
		else if ( dialectNativeGenerator instanceof TableGenerator tableGenerator ) {
			applyProperties( parameters, annotation.tableForm(), creationContext );
			tableGenerator.configure( creationContext, parameters );
		}
	}

	@Override
	public void registerExportables(Database database) {
		if ( dialectNativeGenerator instanceof ExportableProducer exportableProducer ) {
			exportableProducer.registerExportables(database);
		}
	}

	@Override
	public void initialize(SqlStringGenerationContext context) {
		if ( dialectNativeGenerator instanceof Configurable configurable ) {
			configurable.initialize(context);
		}
	}

	@Override
	public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
		return ((BeforeExecutionGenerator) dialectNativeGenerator).generate(session, owner, currentValue, eventType);
	}

	@Override
	public boolean referenceColumnsInSql(Dialect dialect) {
		return ((OnExecutionGenerator) dialectNativeGenerator).referenceColumnsInSql(dialect);
	}

	@Override
	public boolean writePropertyValue() {
		return ((OnExecutionGenerator) dialectNativeGenerator).writePropertyValue();
	}

	@Override
	public String[] getReferencedColumnValues(Dialect dialect) {
		return ((OnExecutionGenerator) dialectNativeGenerator).getReferencedColumnValues(dialect);
	}

	@Override
	public InsertGeneratedIdentifierDelegate getGeneratedIdentifierDelegate(EntityPersister persister) {
		return ((OnExecutionGenerator) dialectNativeGenerator).getGeneratedIdentifierDelegate(persister);
	}

	private void applyProperties(
			Properties properties,
			SequenceGenerator sequenceAnnotation,
			GeneratorCreationContext creationContext) {
		properties.put( GENERATOR_NAME, sequenceAnnotation.name() );
		applyCommonConfiguration( properties, creationContext );
		SequenceStyleGenerator.applyConfiguration( sequenceAnnotation, properties::put );
	}

	private void applyProperties(
			Properties properties,
			jakarta.persistence.TableGenerator tableGenerator,
			GeneratorCreationContext creationContext) {
		properties.put( GENERATOR_NAME, tableGenerator.name() );
		applyCommonConfiguration( properties, creationContext );
		TableGenerator.applyConfiguration( tableGenerator, properties::put );
	}

	private static void applyCommonConfiguration(
			Properties properties,
			GeneratorCreationContext context) {
		collectParameters(
				context.getProperty().getValue(),
				context.getDatabase().getDialect(),
				context.getRootClass(),
				properties::put,
				context.getServiceRegistry()
						.requireService( ConfigurationService.class )
		);
		properties.put( INCREMENT_PARAM, 1 );
	}
}
