/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Comment;
import org.hibernate.annotations.SecondaryRow;
import org.hibernate.annotations.Subselect;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitEntityNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.models.AnnotationPlacementException;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.bind.spi.PhysicalTableReference;
import org.hibernate.boot.models.bind.spi.QuotedIdentifierTarget;
import org.hibernate.boot.models.bind.spi.TableReference;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.DenormalizedTable;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;

import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
public class TableHelper {

	public static TableReference bindPrimaryTable(
			EntityTypeMetadata entityTypeMetadata,
			EntityHierarchy.HierarchyRelation hierarchyRelation,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final ClassDetails typeClassDetails = entityTypeMetadata.getClassDetails();
		final AnnotationUsage<Table> tableAnn = typeClassDetails.getAnnotationUsage( Table.class );
		final AnnotationUsage<JoinTable> joinTableAnn = typeClassDetails.getAnnotationUsage( JoinTable.class );
		final AnnotationUsage<Subselect> subselectAnn = typeClassDetails.getAnnotationUsage( Subselect.class );

		if ( tableAnn != null && joinTableAnn != null ) {
			throw new AnnotationPlacementException( "Illegal combination of @Table and @JoinTable on " + typeClassDetails.getName() );
		}
		if ( tableAnn != null && subselectAnn != null ) {
			throw new AnnotationPlacementException( "Illegal combination of @Table and @Subselect on " + typeClassDetails.getName() );
		}
		if ( joinTableAnn != null && subselectAnn != null ) {
			throw new AnnotationPlacementException( "Illegal combination of @JoinTable and @Subselect on " + typeClassDetails.getName() );
		}

		final TableReference tableReference;

		if ( entityTypeMetadata.getHierarchy().getInheritanceType() == InheritanceType.TABLE_PER_CLASS ) {
			assert subselectAnn == null;

			if ( hierarchyRelation == EntityHierarchy.HierarchyRelation.ROOT ) {
				tableReference = bindPhysicalTable(
						entityTypeMetadata,
						tableAnn,
						Table.class,
						true,
						bindingOptions,
						bindingState,
						bindingContext
				);
			}
			else {
				tableReference = bindUnionTable(
						entityTypeMetadata,
						tableAnn,
						bindingOptions,
						bindingState,
						bindingContext
				);
			}
		}
		else if ( entityTypeMetadata.getHierarchy().getInheritanceType() == InheritanceType.SINGLE_TABLE ) {
			if ( hierarchyRelation == EntityHierarchy.HierarchyRelation.ROOT ) {
				tableReference = normalTableDetermination(
						entityTypeMetadata,
						subselectAnn,
						tableAnn,
						Table.class,
						typeClassDetails,
						bindingOptions,
						bindingState,
						bindingContext
				);
			}
			else {
				tableReference = null;
			}
		}
		else {
			tableReference = normalTableDetermination(
					entityTypeMetadata,
					subselectAnn,
					joinTableAnn,
					JoinTable.class,
					typeClassDetails,
					bindingOptions,
					bindingState,
					bindingContext
			);
		}

		if ( tableReference != null ) {
			bindingState.addTable( entityTypeMetadata, tableReference );

			final PrimaryKey primaryKey = new PrimaryKey( tableReference.table() );
			tableReference.table().setPrimaryKey( primaryKey );
		}

		return tableReference;
	}

	public static void bindSecondaryTables(
			EntityBinding entityBinding,
			TableReference primaryTableReference,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final ClassDetails typeClassDetails = entityBinding.getTypeMetadata().getClassDetails();
		final List<AnnotationUsage<jakarta.persistence.SecondaryTable>> secondaryTableAnns = typeClassDetails.getRepeatedAnnotationUsages( jakarta.persistence.SecondaryTable.class );

		secondaryTableAnns.forEach( (secondaryTableAnn) -> {
			final AnnotationUsage<SecondaryRow> secondaryRowAnn = typeClassDetails.getNamedAnnotationUsage(
					SecondaryRow.class,
					secondaryTableAnn.getString( "name" ),
					"table"
			);
			final SecondaryTable binding = bindSecondaryTable(
					entityBinding,
					primaryTableReference,
					secondaryTableAnn,
					secondaryRowAnn,
					bindingOptions,
					bindingState,
					bindingContext
			);
			bindingState.addSecondaryTable( binding );
		} );
	}

	public static SecondaryTable bindSecondaryTable(
			EntityBinding entityBinding,
			TableReference primaryTableReference,
			AnnotationUsage<jakarta.persistence.SecondaryTable> secondaryTableAnn,
			AnnotationUsage<SecondaryRow> secondaryRowAnn,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final Identifier logicalName = determineLogicalName(
				entityBinding.getTypeMetadata(),
				secondaryTableAnn,
				bindingOptions,
				bindingState,
				bindingContext
		);
		final Identifier schemaName = resolveDatabaseIdentifier(
				secondaryTableAnn,
				"schema",
				jakarta.persistence.SecondaryTable.class,
				bindingOptions.getDefaultSchemaName(),
				QuotedIdentifierTarget.SCHEMA_NAME,
				bindingOptions,
				bindingState,
				bindingContext
		);
		final Identifier catalogName = resolveDatabaseIdentifier(
				secondaryTableAnn,
				"catalog",
				jakarta.persistence.SecondaryTable.class,
				bindingOptions.getDefaultCatalogName(),
				QuotedIdentifierTarget.CATALOG_NAME,
				bindingOptions,
				bindingState,
				bindingContext
		);

		final var secondaryTable = bindingState.getMetadataBuildingContext().getMetadataCollector().addTable(
				toCanonicalName( schemaName ),
				toCanonicalName( catalogName ),
				logicalName.getCanonicalName(),
				null,
				false,
				bindingState.getMetadataBuildingContext()
		);

		applyComment( secondaryTable, secondaryTableAnn, findCommentAnnotation(
				entityBinding.getTypeMetadata(),
				logicalName,
				false
		) );
		applyOptions( secondaryTable, secondaryTableAnn );

		final Join join = new Join();
		join.setTable( secondaryTable );
		join.setOptional( BindingHelper.getValue( secondaryRowAnn, "optional", true ) );
		join.setInverse( !BindingHelper.getValue( secondaryRowAnn, "owned", true ) );
		join.setPersistentClass( entityBinding.getPersistentClass() );
		entityBinding.getPersistentClass().addJoin( join );

		final JdbcEnvironment jdbcEnvironment = jdbcEnvironment( bindingContext );
		final PhysicalNamingStrategy physicalNamingStrategy = physicalNamingStrategy( bindingContext );
		return new SecondaryTable(
				logicalName,
				catalogName,
				schemaName,
				physicalNamingStrategy.toPhysicalTableName( logicalName, jdbcEnvironment ),
				physicalNamingStrategy.toPhysicalCatalogName( catalogName, jdbcEnvironment ),
				physicalNamingStrategy.toPhysicalSchemaName( schemaName, jdbcEnvironment ),
				BindingHelper.getValue( secondaryRowAnn, "optional", true ),
				BindingHelper.getValue( secondaryRowAnn, "owned", true ),
				secondaryTable
		);
	}

	private static <A extends Annotation> TableReference normalTableDetermination(
			EntityTypeMetadata type,
			AnnotationUsage<Subselect> subselectAnn,
			AnnotationUsage<A> tableAnn,
			Class<A> annotationType,
			ClassDetails typeClassDetails,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final TableReference tableReference;
		if ( subselectAnn != null ) {
			tableReference = bindVirtualTable( type, subselectAnn, bindingOptions, bindingState, bindingContext );
		}
		else {
			// either an explicit or implicit @Table
			tableReference = bindPhysicalTable( type, tableAnn, annotationType, true, bindingOptions, bindingState, bindingContext );
		}
		return tableReference;
	}

	private static InLineView bindVirtualTable(
			EntityTypeMetadata type,
			AnnotationUsage<Subselect> subselectAnn,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final Identifier logicalName = implicitNamingStrategy( bindingContext ).determinePrimaryTableName(
				new ImplicitEntityNameSource() {
					@Override
					public EntityNaming getEntityNaming() {
						return type;
					}

					@Override
					public MetadataBuildingContext getBuildingContext() {
						return bindingState.getMetadataBuildingContext();
					}
				}
		);

		// todo : get rid of metadata-collector handling of tables and handle via table-state
		return new InLineView(
				logicalName,
				bindingState.getMetadataBuildingContext().getMetadataCollector().addTable(
						null,
						null,
						logicalName.getCanonicalName(),
						BindingHelper.getString( subselectAnn, "value", Subselect.class, bindingContext ),
						true,
						bindingState.getMetadataBuildingContext()
				)
		);
	}

	private static <A extends Annotation> PhysicalTableReference bindPhysicalTable(
			EntityTypeMetadata type,
			AnnotationUsage<A> tableAnn,
			Class<A> annotationType,
			boolean isPrimary,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		if ( tableAnn != null ) {
			return bindExplicitPhysicalTable( type, tableAnn, annotationType, isPrimary, bindingOptions, bindingState, bindingContext );
		}
		else {
			return bindImplicitPhysicalTable( type, isPrimary, bindingOptions, bindingState, bindingContext );
		}
	}

	private static <A extends Annotation> PhysicalTable bindExplicitPhysicalTable(
			EntityTypeMetadata type,
			AnnotationUsage<A> tableAnn,
			Class<A> annotationType,
			boolean isPrimary,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final Identifier logicalName = determineLogicalName( type, tableAnn, bindingOptions, bindingState, bindingContext );
		final Identifier logicalSchemaName = resolveDatabaseIdentifier(
				tableAnn,
				"schema",
				annotationType,
				bindingOptions.getDefaultSchemaName(),
				QuotedIdentifierTarget.SCHEMA_NAME,
				bindingOptions,
				bindingState,
				bindingContext
		);
		final Identifier logicalCatalogName = resolveDatabaseIdentifier(
				tableAnn,
				"catalog",
				annotationType,
				bindingOptions.getDefaultCatalogName(),
				QuotedIdentifierTarget.CATALOG_NAME,
				bindingOptions,
				bindingState,
				bindingContext
		);

		final var binding = bindingState.getMetadataBuildingContext().getMetadataCollector().addTable(
				logicalSchemaName == null ? null : logicalSchemaName.getCanonicalName(),
				logicalCatalogName == null  ? null : logicalCatalogName.getCanonicalName(),
				logicalName.getCanonicalName(),
				null,
				type.isAbstract(),
				bindingState.getMetadataBuildingContext()
		);

		applyComment( binding, tableAnn, findCommentAnnotation( type, logicalName, isPrimary ) );
		applyOptions( binding, tableAnn );

		final JdbcEnvironment jdbcEnvironment = jdbcEnvironment( bindingContext );
		final PhysicalNamingStrategy physicalNamingStrategy = physicalNamingStrategy( bindingContext );

		return new PhysicalTable(
				logicalName,
				logicalCatalogName,
				logicalSchemaName,
				physicalNamingStrategy.toPhysicalTableName( logicalName, jdbcEnvironment ),
				logicalCatalogName == null ? null : physicalNamingStrategy.toPhysicalCatalogName( logicalCatalogName, jdbcEnvironment ),
				logicalSchemaName == null ? null : physicalNamingStrategy.toPhysicalSchemaName( logicalSchemaName, jdbcEnvironment ),
				binding
		);
	}

	private static PhysicalTable bindImplicitPhysicalTable(
			EntityTypeMetadata type,
			boolean isPrimary,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final Identifier logicalName = determineLogicalName( type, null, bindingOptions, bindingState, bindingContext );

		final org.hibernate.mapping.Table binding = bindingState.getMetadataBuildingContext().getMetadataCollector().addTable(
				bindingOptions.getDefaultSchemaName() == null ? null : bindingOptions.getDefaultSchemaName().getCanonicalName(),
				bindingOptions.getDefaultCatalogName() == null ? null : bindingOptions.getDefaultCatalogName().getCanonicalName(),
				logicalName.getCanonicalName(),
				null,
				type.isAbstract(),
				bindingState.getMetadataBuildingContext()
		);

		applyComment(
				binding,
				null,
				findCommentAnnotation( type, logicalName, isPrimary )
		);

		final JdbcEnvironment jdbcEnvironment = jdbcEnvironment( bindingContext );
		final PhysicalNamingStrategy physicalNamingStrategy = physicalNamingStrategy( bindingContext );

		return new PhysicalTable(
				logicalName,
				bindingOptions.getDefaultCatalogName(),
				bindingOptions.getDefaultSchemaName(),
				physicalNamingStrategy.toPhysicalTableName( logicalName, jdbcEnvironment ),
				physicalNamingStrategy.toPhysicalCatalogName( bindingOptions.getDefaultCatalogName(), jdbcEnvironment ),
				physicalNamingStrategy.toPhysicalSchemaName( bindingOptions.getDefaultSchemaName(), jdbcEnvironment ),
				binding
		);
	}

	private static TableReference bindUnionTable(
			EntityTypeMetadata type,
			AnnotationUsage<Table> tableAnn,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		assert type.getSuperType() != null;

		final TableReference superTypeTable = bindingState.getTableByOwner( type.getSuperType() );
		final org.hibernate.mapping.Table unionBaseTable = superTypeTable.table();

		final Identifier logicalName = determineLogicalName( type, tableAnn, bindingOptions, bindingState, bindingContext );
		final Identifier logicalSchemaName = resolveDatabaseIdentifier(
				tableAnn,
				"schema",
				Table.class,
				bindingOptions.getDefaultSchemaName(),
				QuotedIdentifierTarget.SCHEMA_NAME,
				bindingOptions,
				bindingState,
				bindingContext
		);
		final Identifier logicalCatalogName = resolveDatabaseIdentifier(
				tableAnn,
				"catalog",
				Table.class,
				bindingOptions.getDefaultCatalogName(),
				QuotedIdentifierTarget.CATALOG_NAME,
				bindingOptions,
				bindingState,
				bindingContext
		);

		final DenormalizedTable binding = (DenormalizedTable) bindingState.getMetadataBuildingContext().getMetadataCollector().addDenormalizedTable(
				logicalSchemaName == null ? null : logicalSchemaName.getCanonicalName(),
				logicalCatalogName == null  ? null : logicalCatalogName.getCanonicalName(),
				logicalName.getCanonicalName(),
				type.isAbstract(),
				null,
				unionBaseTable,
				bindingState.getMetadataBuildingContext()
		);

		return new UnionTable( logicalName, superTypeTable, binding, !type.hasSubTypes() );
	}






	private static Identifier determineLogicalName(
			EntityTypeMetadata type,
			AnnotationUsage<?> tableAnn,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		if ( tableAnn != null ) {
			final String name = StringHelper.nullIfEmpty( tableAnn.getString( "name" ) );
			if ( name != null ) {
				return BindingHelper.toIdentifier( name, QuotedIdentifierTarget.TABLE_NAME, bindingOptions, jdbcEnvironment( bindingContext ) );
			}
		}

		return implicitNamingStrategy( bindingContext ).determinePrimaryTableName(
				new ImplicitEntityNameSource() {
					@Override
					public EntityNaming getEntityNaming() {
						return type;
					}

					@Override
					public MetadataBuildingContext getBuildingContext() {
						return bindingState.getMetadataBuildingContext();
					}
				}
		);
	}

	private static String toCanonicalName(Identifier name) {
		if ( name == null ) {
			return null;
		}
		return name.getCanonicalName();
	}

	private static <A extends Annotation> Identifier resolveDatabaseIdentifier(
			AnnotationUsage<A> annotationUsage,
			String attributeName,
			Class<A> annotationType,
			Identifier fallback,
			QuotedIdentifierTarget target,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final String explicit = BindingHelper.getStringOrNull( annotationUsage, attributeName );
		if ( StringHelper.isNotEmpty( explicit ) ) {
			return BindingHelper.toIdentifier( explicit, target, bindingOptions, jdbcEnvironment( bindingContext ) );
		}

		if ( fallback != null ) {
			return fallback;
		}

		final String defaultValue = BindingHelper.getDefaultValue( attributeName, annotationType, bindingContext );
		return BindingHelper.toIdentifier(defaultValue, target, bindingOptions, jdbcEnvironment( bindingContext ) );
	}

	private static AnnotationUsage<Comment> findCommentAnnotation(
			EntityTypeMetadata type,
			Identifier logicalTableName,
			boolean isPrimary) {
		if ( isPrimary ) {
			final AnnotationUsage<Comment> unnamed = type.getClassDetails().getNamedAnnotationUsage(
					Comment.class,
					"",
					"on"
			);
			if ( unnamed != null ) {
				return unnamed;
			}
		}

		return type.getClassDetails().getNamedAnnotationUsage(
				Comment.class,
				logicalTableName.getCanonicalName(),
				"on"
		);
	}

	private static void applyComment(
			org.hibernate.mapping.Table table,
			AnnotationUsage<?> tableAnn,
			AnnotationUsage<Comment> commentAnn) {
		if ( commentAnn != null ) {
			table.setComment( commentAnn.getString( "value" ) );
		}
		else if ( tableAnn != null ) {
			final String comment = tableAnn.getString( "comment" );
			if ( StringHelper.isNotEmpty( comment ) ) {
				table.setComment( comment );
			}
		}
	}

	private static void applyOptions(org.hibernate.mapping.Table table, AnnotationUsage<?> tableAnn) {
		if ( tableAnn != null ) {
			final String options = tableAnn.getString( "options" );
			if ( StringHelper.isNotEmpty( options ) ) {
				// todo : add this to Table
//				table.setOptions( options );
				throw new UnsupportedOperationException( "Not yet implemented" );
			}
		}
	}

	private static ImplicitNamingStrategy implicitNamingStrategy(BindingContext bindingContext) {
		return bindingContext
				.getBootstrapContext()
				.getMetadataBuildingOptions()
				.getImplicitNamingStrategy();
	}

	private static PhysicalNamingStrategy physicalNamingStrategy(BindingContext bindingContext) {
		return bindingContext
				.getBootstrapContext()
				.getMetadataBuildingOptions()
				.getPhysicalNamingStrategy();
	}

	private static JdbcEnvironment jdbcEnvironment(BindingContext bindingContext) {
		return bindingContext.getServiceRegistry().getService( JdbcEnvironment.class );
	}

}
