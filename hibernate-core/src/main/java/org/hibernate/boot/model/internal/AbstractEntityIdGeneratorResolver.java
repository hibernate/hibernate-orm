/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.generator.Generator;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.GeneratedValue;

import static org.hibernate.boot.model.internal.GeneratorAnnotationHelper.handleIdGeneratorType;
import static org.hibernate.boot.model.internal.GeneratorParameters.identityTablesString;
import static org.hibernate.boot.model.internal.GeneratorStrategies.mapLegacyNamedGenerator;
import static org.hibernate.id.IdentifierGenerator.ENTITY_NAME;
import static org.hibernate.id.IdentifierGenerator.JPA_ENTITY_NAME;
import static org.hibernate.id.OptimizableGenerator.IMPLICIT_NAME_BASE;
import static org.hibernate.id.PersistentIdentifierGenerator.PK;
import static org.hibernate.id.PersistentIdentifierGenerator.TABLE;
import static org.hibernate.id.PersistentIdentifierGenerator.TABLES;

/**
 * Template support for IdGeneratorResolver implementations dealing with entity identifiers
 *
 * @author Steve Ebersole
 */
public abstract class AbstractEntityIdGeneratorResolver implements IdGeneratorResolver {
	protected final PersistentClass entityMapping;
	protected final SimpleValue idValue;
	protected final MemberDetails idMember;
	protected final GeneratedValue generatedValue;
	protected final MetadataBuildingContext buildingContext;

	public AbstractEntityIdGeneratorResolver(
			PersistentClass entityMapping,
			SimpleValue idValue,
			MemberDetails idMember,
			GeneratedValue generatedValue,
			MetadataBuildingContext buildingContext) {
		this.entityMapping = entityMapping;
		this.idValue = idValue;
		this.idMember = idMember;
		this.generatedValue = generatedValue;
		this.buildingContext = buildingContext;
	}

	@Override
	public final void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
		switch ( generatedValue.strategy() ) {
			case UUID -> handleUuidStrategy();
			case IDENTITY -> GeneratorAnnotationHelper.handleIdentityStrategy( idValue );
			case SEQUENCE -> handleSequenceStrategy();
			case TABLE -> handleTableStrategy();
			case AUTO -> handleAutoStrategy();
		}
	}

	private void handleUuidStrategy() {
		GeneratorAnnotationHelper.handleUuidStrategy(
				idValue,
				idMember,
				buildingContext.getMetadataCollector().getClassDetailsRegistry()
						.getClassDetails( entityMapping.getClassName() ),
				buildingContext
		);
	}

	private void handleSequenceStrategy() {
		if ( generatedValue.generator().isBlank() ) {
			handleUnnamedSequenceGenerator();
		}
		else {
			handleNamedSequenceGenerator();
		}
	}

	protected abstract void handleUnnamedSequenceGenerator();

	protected abstract void handleNamedSequenceGenerator();

	private void handleTableStrategy() {
		if ( generatedValue.generator().isBlank() ) {
			handleUnnamedTableGenerator();
		}
		else {
			handleNamedTableGenerator();
		}
	}

	protected abstract void handleUnnamedTableGenerator();

	protected abstract void handleNamedTableGenerator();

	private void handleAutoStrategy() {
		if ( generatedValue.generator().isBlank() ) {
			handleUnnamedAutoGenerator();
		}
		else {
			handleNamedAutoGenerator();
		}
	}

	protected abstract void handleUnnamedAutoGenerator();

	protected abstract void handleNamedAutoGenerator();

	protected boolean handleAsMetaAnnotated() {
		final Annotation fromMember = findGeneratorAnnotation( idMember );
		if ( fromMember != null ) {
			handleIdGeneratorType( fromMember, idValue, idMember, buildingContext );
			return true;
		}

		final Annotation fromClass = findGeneratorAnnotation( idMember.getDeclaringType() );
		if ( fromClass != null ) {
			handleIdGeneratorType( fromClass, idValue, idMember, buildingContext );
			return true;
		}

		final ClassDetails packageInfoDetails = GeneratorAnnotationHelper.locatePackageInfoDetails( idMember.getDeclaringType(), buildingContext );
		if ( packageInfoDetails != null ) {
			final Annotation fromPackage = findGeneratorAnnotation( packageInfoDetails );
			if ( fromPackage != null ) {
				handleIdGeneratorType( fromPackage, idValue, idMember, buildingContext );
				return true;
			}
		}

		return false;
	}

	private Annotation findGeneratorAnnotation(AnnotationTarget annotationTarget) {
		final List<? extends Annotation> metaAnnotated =
				annotationTarget.getMetaAnnotated( IdGeneratorType.class,
						buildingContext.getBootstrapContext().getModelsContext() );
		if ( CollectionHelper.size( metaAnnotated ) > 0 ) {
			return metaAnnotated.get( 0 );
		}

		return null;
	}

	protected boolean handleAsLegacyGenerator() {
		// Handle a few legacy Hibernate generators...
		final String nameFromGeneratedValue = generatedValue.generator();
		if ( !nameFromGeneratedValue.isBlank() ) {
			final Class<? extends Generator> legacyNamedGenerator =
					mapLegacyNamedGenerator( nameFromGeneratedValue, idValue );
			if ( legacyNamedGenerator != null ) {
				final Map<String,String> configuration = buildLegacyGeneratorConfig();
				//noinspection unchecked,rawtypes
				GeneratorBinder.createGeneratorFrom(
						new IdentifierGeneratorDefinition( nameFromGeneratedValue,
								legacyNamedGenerator.getName(), configuration ),
						idValue,
						(Map) configuration,
						buildingContext
				);
				return true;
			}
		}

		return false;
	}

	private HashMap<String, String> buildLegacyGeneratorConfig() {
		final Database database = buildingContext.getMetadataCollector().getDatabase();
		final Dialect dialect = database.getDialect();

		final HashMap<String, String> configuration = new HashMap<>();

		final String tableName = idValue.getTable().getQuotedName( dialect );
		configuration.put( TABLE, tableName );

		final Column idColumn = (Column) idValue.getSelectables().get( 0);
		final String idColumnName = idColumn.getQuotedName( dialect );
		configuration.put( PK, idColumnName );

		configuration.put( ENTITY_NAME, entityMapping.getEntityName() );
		configuration.put( JPA_ENTITY_NAME, entityMapping.getJpaEntityName() );

		// The table name is not really a good default for subselect entities,
		// so use the JPA entity name which is short
		configuration.put(
				IMPLICIT_NAME_BASE,
				idValue.getTable().isSubselect()
						? entityMapping.getJpaEntityName()
						: idValue.getTable().getName()
		);

		configuration.put( TABLES, identityTablesString( dialect, entityMapping.getRootClass() ) );

		return configuration;
	}
}
