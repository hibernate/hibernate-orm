/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Contributable;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.mapping.Association;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;


/**
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class TemporaryTable implements Exportable, Contributable {

	public static final String ID_TABLE_PREFIX = "HT_";
	public static final String ENTITY_TABLE_PREFIX = "HTE_";
	public static final String DEFAULT_ALIAS = "temptable_";
	public static final String ENTITY_TABLE_IDENTITY_COLUMN = "HTE_IDENTITY";
	public static final String ENTITY_ROW_NUMBER_COLUMN = "rn_";

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( TemporaryTable.class );

	private final String contributor;
	private final String qualifiedTableName;
	private final TemporaryTableKind temporaryTableKind;

	private final TemporaryTableSessionUidColumn sessionUidColumn;
	private final List<TemporaryTableColumn> columns;
	private final List<TemporaryTableColumn> columnsForExport;

	private final Dialect dialect;

	private TemporaryTable(
			PersistentClass persistentClass,
			Function<String, String> temporaryTableNameAdjuster,
			TemporaryTableKind temporaryTableKind,
			Dialect dialect,
			RuntimeModelCreationContext creationContext,
			Function<TemporaryTable, List<TemporaryTableColumn>> columnInitializer) {
		this.contributor = persistentClass.getContributor();
		final Identifier tableNameIdentifier;
		if ( persistentClass instanceof SingleTableSubclass ) {
			// In this case, the descriptor is a subclass of a single table inheritance.
			// To avoid name collisions, we suffix the table name with the subclass number
			tableNameIdentifier = new Identifier(
					persistentClass.getTable().getNameIdentifier().getText() + persistentClass.getSubclassId(),
					persistentClass.getTable().getNameIdentifier().isQuoted()
			);
		}
		else {
			tableNameIdentifier = persistentClass.getTable().getNameIdentifier();
		}
		// Have to parse the adjusted name, since it could be prepended by a schema
		final QualifiedNameParser.NameParts nameParts = QualifiedNameParser.INSTANCE
				.parse( temporaryTableNameAdjuster.apply( tableNameIdentifier.getText() ) );
		final Identifier catalogIdentifier = nameParts.getCatalogName() != null ? nameParts.getCatalogName()
				: persistentClass.getTable().getCatalogIdentifier();
		final Identifier schemaIdentifier = nameParts.getSchemaName() != null ? nameParts.getSchemaName()
				: persistentClass.getTable().getSchemaIdentifier();
		final String adjustedName = nameParts.getObjectName().getText();
		final Identifier temporaryTableNameIdentifier = new Identifier(
				adjustedName.substring( 0, Math.min( dialect.getMaxIdentifierLength(), adjustedName.length() ) ),
				tableNameIdentifier.isQuoted()
		);
		this.qualifiedTableName = creationContext.getSqlStringGenerationContext().format(
				new QualifiedTableName( catalogIdentifier, schemaIdentifier, temporaryTableNameIdentifier )
		);
		this.temporaryTableKind = temporaryTableKind;
		this.dialect = dialect;
		if ( temporaryTableKind == TemporaryTableKind.PERSISTENT ) {
			final TypeConfiguration typeConfiguration = creationContext.getTypeConfiguration();
			final BasicType<UUID> uuidType = typeConfiguration.getBasicTypeRegistry().resolve(
					StandardBasicTypes.UUID_CHAR
			);
			final Size size = dialect.getSizeStrategy().resolveSize(
					uuidType.getJdbcType(),
					uuidType.getJavaTypeDescriptor(),
					null,
					null,
					null
			);
			this.sessionUidColumn = new TemporaryTableSessionUidColumn(
					this,
					uuidType,
					typeConfiguration.getDdlTypeRegistry().getTypeName(
							uuidType.getJdbcType().getDdlTypeCode(),
							size,
							uuidType
					),
					size
			);
		}
		else {
			this.sessionUidColumn = null;
		}
		final List<TemporaryTableColumn> columns = columnInitializer.apply( this );
		if ( sessionUidColumn != null ) {
			columns.add( sessionUidColumn );
		}
		this.columns = columns;

		if ( columns.size() > 1 ) {
			final ArrayList<TemporaryTableColumn> columnsForExport = new ArrayList<>( columns );
			creationContext.getBootModel().getMetadataBuildingOptions().getColumnOrderingStrategy()
					.orderTemporaryTableColumns( columnsForExport, creationContext.getMetadata() );
			this.columnsForExport = columnsForExport;
		}
		else {
			this.columnsForExport = columns;
		}
	}

	@Deprecated(forRemoval = true, since = "7.1")
	public static TemporaryTable createIdTable(
			EntityMappingType entityDescriptor,
			Function<String, String> temporaryTableNameAdjuster,
			Dialect dialect,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return createIdTable(
				runtimeModelCreationContext.getBootModel()
						.getEntityBinding( entityDescriptor.getEntityName() ),
				temporaryTableNameAdjuster,
				dialect.getSupportedTemporaryTableKind(),
				dialect,
				runtimeModelCreationContext
		);
	}

	@Deprecated(forRemoval = true, since = "7.1")
	public static TemporaryTable createEntityTable(
			EntityMappingType entityDescriptor,
			Function<String, String> temporaryTableNameAdjuster,
			Dialect dialect,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return createIdTable(
				runtimeModelCreationContext.getBootModel()
						.getEntityBinding( entityDescriptor.getEntityName() ),
				temporaryTableNameAdjuster,
				dialect.getSupportedTemporaryTableKind(),
				dialect,
				runtimeModelCreationContext
		);
	}

	public static TemporaryTable createIdTable(
			PersistentClass persistentClass,
			Function<String, String> temporaryTableNameAdjuster,
			TemporaryTableKind temporaryTableKind,
			Dialect dialect,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new TemporaryTable(
				persistentClass,
				temporaryTableNameAdjuster,
				temporaryTableKind,
				dialect,
				runtimeModelCreationContext,
				temporaryTable -> {
					final MetadataImplementor metadata = runtimeModelCreationContext.getMetadata();
					final List<TemporaryTableColumn> columns = new ArrayList<>();

					for ( Column column : persistentClass.getKey().getColumns() ) {
						columns.add(
								new TemporaryTableColumn(
										temporaryTable,
										column.getText( dialect ),
										column.getType(),
										column.getSqlType( metadata ),
										column.getColumnSize( dialect, metadata ),
										column.isNullable(),
										true
								)
						);
					}

					visitPluralAttributes( persistentClass.getPropertyClosure(), collection -> {
						if ( collection.getCollectionTable() != null && collection.getReferencedPropertyName() != null ) {
							final KeyValue collectionKey = collection.getKey();
							for ( Selectable selectable : collectionKey.getSelectables() ) {
								if ( selectable instanceof Column column ) {
									columns.add(
											new TemporaryTableColumn(
													temporaryTable,
													column.getText( dialect ),
													column.getType(),
													column.getSqlType( metadata ),
													column.getColumnSize( dialect, metadata ),
													column.isNullable()
											)
									);
								}
							}
						}
					} );
					return columns;
				}
		);
	}

	private static void visitPluralAttributes(List<Property> properties, Consumer<Collection> consumer) {
		for ( Property property : properties ) {
			final Value value = property.getValue();
			if ( value instanceof Collection collection ) {
				consumer.accept( collection );
			}
			else if ( value instanceof Component component ) {
				visitPluralAttributes( component.getProperties(), consumer );
			}
		}
	}

	public static TemporaryTable createEntityTable(
			PersistentClass persistentClass,
			Function<String, String> temporaryTableNameAdjuster,
			TemporaryTableKind temporaryTableKind,
			Dialect dialect,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new TemporaryTable(
				persistentClass,
				temporaryTableNameAdjuster,
				temporaryTableKind,
				dialect,
				runtimeModelCreationContext,
				temporaryTable -> {
					final MetadataImplementor metadata = runtimeModelCreationContext.getMetadata();
					final List<TemporaryTableColumn> columns = new ArrayList<>();
					final List<Column> rootKeyColumns = persistentClass.getRootClass().getKey().getColumns();
					final boolean identityColumn = rootKeyColumns.size() == 1 && rootKeyColumns.get( 0 ).isIdentity();
					final boolean isExternallyGenerated;
					if ( identityColumn ) {
						isExternallyGenerated = false;
						for ( Column column : persistentClass.getKey().getColumns() ) {
							String sqlTypeName = "";
							if ( dialect.getIdentityColumnSupport().hasDataTypeInIdentityColumn() ) {
								sqlTypeName = column.getSqlType( metadata ) + " ";
							}
							sqlTypeName = sqlTypeName + dialect.getIdentityColumnSupport()
									.getIdentityColumnString( column.getSqlTypeCode( metadata ) );
							columns.add(
									new TemporaryTableColumn(
											temporaryTable,
											ENTITY_TABLE_IDENTITY_COLUMN,
											column.getType(),
											sqlTypeName,
											column.getColumnSize( dialect, metadata ),
											// Always report as nullable as the identity column string usually includes the not null constraint
											true,//column.isNullable()
											true
									)
							);
						}
					}
					else {
						// This is a bit fishy, because for the generator to exist in this map,
						// the EntityPersister already has to be built. Currently, we have
						// no other way to understand what generators do until we have a boot
						// model representation of the generator information, so this will have
						// to do
						final Generator identifierGenerator = runtimeModelCreationContext.getGenerators()
								.get( persistentClass.getRootClass().getEntityName() );
						assert identifierGenerator != null;

						isExternallyGenerated = !(identifierGenerator instanceof OnExecutionGenerator generator
							&& generator.generatedOnExecution());
					}
					final Property identifierProperty = persistentClass.getIdentifierProperty();
					final String idName;
					if ( identifierProperty != null ) {
						idName = identifierProperty.getName();
					}
					else {
						idName = "id";
					}
					forEachTemporaryTableColumn( metadata, temporaryTable, idName, persistentClass.getIdentifier(), temporaryTableColumn -> {
						columns.add( new TemporaryTableColumn(
								temporaryTableColumn.getContainingTable(),
								temporaryTableColumn.getColumnName(),
								temporaryTableColumn.getJdbcMapping(),
								temporaryTableColumn.getSqlTypeDefinition(),
								temporaryTableColumn.getSize(),
								// We have to set the identity column after the root table insert
								identityColumn || isExternallyGenerated,
								!identityColumn && !isExternallyGenerated
						) );
					});

					final Value discriminator = persistentClass.getDiscriminator();
					if ( discriminator != null && !discriminator.getSelectables().get( 0 ).isFormula() ) {
						forEachTemporaryTableColumn( metadata, temporaryTable, "class", discriminator, temporaryTableColumn -> {
							columns.add( new TemporaryTableColumn(
									temporaryTableColumn.getContainingTable(),
									temporaryTableColumn.getColumnName(),
									temporaryTableColumn.getJdbcMapping(),
									temporaryTableColumn.getSqlTypeDefinition(),
									temporaryTableColumn.getSize(),
									// We have to set the identity column after the root table insert
									discriminator.isNullable()
							) );
						} );
					}

					// Collect all columns for all entity subtype attributes
					for ( Property property : persistentClass.getPropertyClosure() ) {
						if ( !property.isSynthetic() ) {
							forEachTemporaryTableColumn(
									metadata,
									temporaryTable,
									property.getName(),
									property.getValue(),
									columns::add
							);
						}
					}
					if ( isExternallyGenerated ) {
						final TypeConfiguration typeConfiguration = metadata.getTypeConfiguration();
						// We add a special row number column that we can use to identify and join rows
						final BasicType<Integer> integerBasicType = typeConfiguration.getBasicTypeForJavaType( Integer.class );
						final String rowNumberType;
						if ( dialect.supportsWindowFunctions() ) {
							rowNumberType = typeConfiguration.getDdlTypeRegistry().getTypeName(
									integerBasicType.getJdbcType().getDdlTypeCode(),
									dialect.getSizeStrategy().resolveSize(
											integerBasicType.getJdbcType(),
											integerBasicType.getJavaTypeDescriptor(),
											null,
											null,
											null
									),
									integerBasicType
							);
						}
						else if ( dialect.getIdentityColumnSupport().supportsIdentityColumns() ) {
							rowNumberType = typeConfiguration.getDdlTypeRegistry().getTypeName(
									integerBasicType.getJdbcType().getDdlTypeCode(),
									dialect.getSizeStrategy().resolveSize(
											integerBasicType.getJdbcType(),
											integerBasicType.getJavaTypeDescriptor(),
											null,
											null,
											null
									),
									integerBasicType
							) + " " + dialect.getIdentityColumnSupport()
													.getIdentityColumnString( integerBasicType.getJdbcType().getDdlTypeCode() );
						}
						else {
							LOG.multiTableInsertNotAvailable( persistentClass.getEntityName() );
							rowNumberType = typeConfiguration.getDdlTypeRegistry().getTypeName(
									integerBasicType.getJdbcType().getDdlTypeCode(),
									dialect.getSizeStrategy().resolveSize(
											integerBasicType.getJdbcType(),
											integerBasicType.getJavaTypeDescriptor(),
											null,
											null,
											null
									),
									integerBasicType
							);
						}
						columns.add(
								new TemporaryTableColumn(
										temporaryTable,
										ENTITY_ROW_NUMBER_COLUMN,
										integerBasicType,
										rowNumberType,
										Size.nil(),
										false,
										true
								)
						);
					}
					return columns;
				}
		);
	}

	private static void forEachTemporaryTableColumn(Metadata metadata, TemporaryTable temporaryTable, String prefix, Value value, Consumer<TemporaryTableColumn> consumer) {
		final Dialect dialect = metadata.getDatabase().getDialect();
		SqmMutationStrategyHelper.forEachSelectableMapping( prefix, value, (columnName, selectable) -> {
			consumer.accept(
					new TemporaryTableColumn(
							temporaryTable,
							columnName,
							selectable.getType(),
							selectable.getSqlType( metadata ),
							selectable.getColumnSize( dialect, metadata ),
							// Treat regular temporary table columns as nullable for simplicity
							true
					)
			);
		} );
	}

	public List<TemporaryTableColumn> findTemporaryTableColumns(EntityPersister entityDescriptor, ModelPart modelPart) {
		final int offset = determineModelPartStartIndex( entityDescriptor, modelPart );
		if ( offset == -1 ) {
			throw new IllegalStateException( "Couldn't find matching temporary table columns for: " + modelPart );
		}
		final int end = offset + modelPart.getJdbcTypeCount();
		// Find a matching cte table column and set that at the current index
		return getColumns().subList( offset, end );
	}

	private static int determineModelPartStartIndex(EntityPersister entityDescriptor, ModelPart modelPart) {
		boolean hasIdentity = entityDescriptor.getGenerator().generatedOnExecution();
		// Entity with an identity column get HTE_IDENTITY as first column in the temporary table that we skip
		int offset = hasIdentity ? 1 : 0;
		final int idResult = determineIdStartIndex( offset, entityDescriptor, modelPart );
		if ( idResult <= 0 ) {
			return -idResult;
		}
		offset = idResult;
		final EntityDiscriminatorMapping discriminatorMapping = entityDescriptor.getDiscriminatorMapping();
		if ( discriminatorMapping != null && discriminatorMapping.hasPhysicalColumn() && !discriminatorMapping.isFormula() ) {
			if ( modelPart == discriminatorMapping ) {
				return offset;
			}
			offset += discriminatorMapping.getJdbcTypeCount();
		}
		final AttributeMappingsList attributeMappings = entityDescriptor.getAttributeMappings();
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			AttributeMapping attribute = attributeMappings.get( i );
			if ( !( attribute instanceof PluralAttributeMapping ) ) {
				final int result = determineModelPartStartIndex( offset, attribute, modelPart );
				if ( result <= 0 ) {
					return -result;
				}
				offset = result;
			}
		}
		return -1;
	}

	private static int determineIdStartIndex(int offset, EntityPersister entityDescriptor, ModelPart modelPart) {
		final int originalOffset = offset;
		do {
			final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
			final int result = determineModelPartStartIndex( originalOffset, identifierMapping, modelPart );
			offset = result;
			if ( result <= 0 ) {
				break;
			}
			entityDescriptor = (EntityPersister) entityDescriptor.getSuperMappingType();
		} while ( entityDescriptor != null );

		return offset;
	}

	private static int determineModelPartStartIndex(int offset, ModelPart modelPart, ModelPart modelPartToFind) {
		if ( modelPart == modelPartToFind ) {
			return -offset;
		}
		if ( modelPart instanceof EntityValuedModelPart entityValuedModelPart ) {
			final ModelPart keyPart =
					modelPart instanceof Association association
							? association.getForeignKeyDescriptor()
							: entityValuedModelPart.getEntityMappingType().getIdentifierMapping();
			return determineModelPartStartIndex( offset, keyPart, modelPartToFind );
		}
		else if ( modelPart instanceof EmbeddableValuedModelPart embeddablePart ) {
			final AttributeMappingsList attributeMappings =
					embeddablePart.getEmbeddableTypeDescriptor().getAttributeMappings();
			for ( int i = 0; i < attributeMappings.size(); i++ ) {
				final AttributeMapping mapping = attributeMappings.get( i );
				final int result = determineModelPartStartIndex( offset, mapping, modelPartToFind );
				if ( result <= 0 ) {
					return result;
				}
				offset = result;
			}
			return offset;
		}
		else if ( modelPart instanceof BasicValuedModelPart basicModelPart ) {
			return offset + (basicModelPart.isInsertable() ? modelPart.getJdbcTypeCount() : 0);
		}
		return offset + modelPart.getJdbcTypeCount();
	}

	public boolean isRowNumberGenerated() {
		// Only assign a value for the rowNumber column if it isn't using an identity insert
		return !dialect.supportsWindowFunctions()
			&& dialect.getIdentityColumnSupport().supportsIdentityColumns();
	}

	public String getQualifiedTableName() {
		return qualifiedTableName;
	}

	public TemporaryTableKind getTemporaryTableKind() {
		return temporaryTableKind;
	}

	public List<TemporaryTableColumn> getColumns() {
		return columns;
	}

	public List<TemporaryTableColumn> getColumnsForExport() {
		return columnsForExport;
	}

	public TemporaryTableSessionUidColumn getSessionUidColumn() {
		return sessionUidColumn;
	}

	public String getTableExpression() {
		return qualifiedTableName;
	}

	@Override
	public String getContributor() {
		return contributor;
	}

	@Override
	public String getExportIdentifier() {
		return getQualifiedTableName();
	}

	public Dialect getDialect() {
		return this.dialect;
	}
}
