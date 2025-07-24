/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.generator.Generator;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Contributable;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.mapping.Association;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.boot.model.internal.BinderHelper.findPropertyByName;

/**
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class TemporaryTable implements Exportable, Contributable {

	public static final String ID_TABLE_PREFIX = "HT_";
	public static final String ENTITY_TABLE_PREFIX = "HTE_";
	public static final String DEFAULT_ALIAS = "temptable_";
	public static final String ENTITY_TABLE_IDENTITY_COLUMN = "HTE_IDENTITY";

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( TemporaryTable.class );

	private final EntityMappingType entityDescriptor;
	private final String qualifiedTableName;
	private final TemporaryTableKind temporaryTableKind;

	private final TemporaryTableSessionUidColumn sessionUidColumn;
	private final List<TemporaryTableColumn> columns;
	private final List<TemporaryTableColumn> columnsForExport;

	private final Dialect dialect;

	private TemporaryTable(
			EntityMappingType entityDescriptor,
			Function<String, String> temporaryTableNameAdjuster,
			TemporaryTableKind temporaryTableKind,
			Dialect dialect,
			RuntimeModelCreationContext creationContext,
			Function<TemporaryTable, List<TemporaryTableColumn>> columnInitializer) {
		this.entityDescriptor = entityDescriptor;
		final EntityPersister entityPersister = entityDescriptor.getEntityPersister();
		final EntityPersister rootEntityPersister = entityDescriptor.getRootEntityDescriptor().getEntityPersister();
		final String persisterQuerySpace = entityPersister.getSynchronizedQuerySpaces()[0];
		final QualifiedNameParser.NameParts nameParts = QualifiedNameParser.INSTANCE.parse( persisterQuerySpace );
		// The table name might be a sub-query, which is inappropriate for a temporary table name
		final String tableBaseName;
		if ( rootEntityPersister != entityPersister
				&& rootEntityPersister instanceof SingleTableEntityPersister singleTableEntityPersister ) {
			// In this case, the descriptor is a subclass of a single table inheritance.
			// To avoid name collisions, we suffix the table name with the subclass number
			tableBaseName = nameParts.getObjectName().getText() + ArrayHelper.indexOf(
					singleTableEntityPersister.getSubclassClosure(),
					entityPersister.getEntityName()
			);
		}
		else {
			tableBaseName = nameParts.getObjectName().getText();
		}
		final QualifiedNameParser.NameParts adjustedNameParts = QualifiedNameParser.INSTANCE.parse(
				temporaryTableNameAdjuster.apply( tableBaseName )
		);
		final String temporaryTableName = adjustedNameParts.getObjectName().getText();
		final Identifier tableNameIdentifier;
		if ( temporaryTableName.length() > dialect.getMaxIdentifierLength() ) {
			tableNameIdentifier = new Identifier(
					temporaryTableName.substring( 0, dialect.getMaxIdentifierLength() ),
					nameParts.getObjectName().isQuoted()
			);
		}
		else {
			tableNameIdentifier = new Identifier( temporaryTableName, nameParts.getObjectName().isQuoted() );
		}
		this.qualifiedTableName = creationContext.getSqlStringGenerationContext().format(
				new QualifiedTableName(
						adjustedNameParts.getCatalogName() != null
								? adjustedNameParts.getCatalogName()
								: nameParts.getCatalogName(),
						adjustedNameParts.getSchemaName() != null
								? adjustedNameParts.getSchemaName()
								: nameParts.getSchemaName(),
						tableNameIdentifier
				)
		);
		this.temporaryTableKind = temporaryTableKind;
		this.dialect = dialect;
		if ( temporaryTableKind == TemporaryTableKind.PERSISTENT ) {
			final TypeConfiguration typeConfiguration = entityPersister
					.getFactory()
					.getTypeConfiguration();
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
				entityDescriptor,
				temporaryTableNameAdjuster,
				dialect.getSupportedTemporaryTableKind(),
				dialect,
				runtimeModelCreationContext
		);
	}

	public static TemporaryTable createIdTable(
			EntityMappingType entityDescriptor,
			Function<String, String> temporaryTableNameAdjuster,
			TemporaryTableKind temporaryTableKind,
			Dialect dialect,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new TemporaryTable(
				entityDescriptor,
				temporaryTableNameAdjuster,
				temporaryTableKind,
				dialect,
				runtimeModelCreationContext,
				temporaryTable -> {
					final List<TemporaryTableColumn> columns = new ArrayList<>();
					final PersistentClass entityBinding = runtimeModelCreationContext.getBootModel()
							.getEntityBinding( entityDescriptor.getEntityName() );

					final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
					int idIdx = 0;
					for ( Column column : entityBinding.getKey().getColumns() ) {
						final JdbcMapping jdbcMapping = identifierMapping.getJdbcMapping( idIdx++ );
						columns.add(
								new TemporaryTableColumn(
										temporaryTable,
										column.getText( dialect ),
										jdbcMapping,
										column.getSqlType(
												runtimeModelCreationContext.getMetadata()
										),
										column.getColumnSize(
												dialect,
												runtimeModelCreationContext.getMetadata()
										),
										column.isNullable(),
										true
								)
						);
					}

					visitPluralAttributes( entityDescriptor, (pluralAttribute, attributeName) -> {
						if ( pluralAttribute.getSeparateCollectionTable() != null ) {
							// Ensure that the FK target columns are available
							final ForeignKeyDescriptor keyDescriptor = pluralAttribute.getKeyDescriptor();
							if ( keyDescriptor == null ) {
								// This is expected to happen when processing a
								// PostInitCallbackEntry because the callbacks
								// are not ordered. The exception is caught in
								// MappingModelCreationProcess.executePostInitCallbacks()
								// and the callback is re-queued.
								throw new IllegalStateException( "Not yet ready: " + pluralAttribute );
							}
							final ModelPart fkTarget = keyDescriptor.getTargetPart();
							if ( !fkTarget.isEntityIdentifierMapping() ) {
								final PersistentClass declaringClass = runtimeModelCreationContext.getBootModel()
										.getEntityBinding( pluralAttribute.findContainingEntityMapping().getEntityName() );
								final Property property = findPropertyByName( declaringClass, attributeName );
								assert property != null;
								final Collection collection = (Collection) property.getValue();
								final Iterator<Selectable> columnIterator = collection.getKey().getSelectables().iterator();
								fkTarget.forEachSelectable(
										(columnIndex, selection) -> {
											final Selectable selectable = columnIterator.next();
											if ( selectable instanceof Column column ) {
												columns.add(
														new TemporaryTableColumn(
																temporaryTable,
																column.getText( dialect ),
																selection.getJdbcMapping(),
																column.getSqlType(
																		runtimeModelCreationContext.getMetadata()
																),
																column.getColumnSize(
																		dialect,
																		runtimeModelCreationContext.getMetadata()
																),
																column.isNullable()
														)
												);
											}
										}
								);
							}
						}
					} );
					return columns;
				}
		);
	}

	private static void visitPluralAttributes(
			EntityMappingType entityDescriptor,
			BiConsumer<PluralAttributeMapping, String> consumer) {
		entityDescriptor.visitSubTypeAttributeMappings(
				attribute -> {
					if ( attribute instanceof PluralAttributeMapping pluralAttributeMapping ) {
						consumer.accept( pluralAttributeMapping, attribute.getAttributeName() );
					}
					else if ( attribute instanceof EmbeddedAttributeMapping embeddedAttributeMapping ) {
						visitPluralAttributes(
								embeddedAttributeMapping,
								attribute.getAttributeName(),
								consumer
						);
					}
				}
		);
	}

	private static void visitPluralAttributes(
			EmbeddedAttributeMapping attributeMapping,
			String attributeName,
			BiConsumer<PluralAttributeMapping, String> consumer) {
		attributeMapping.visitSubParts(
				modelPart -> {
					if ( modelPart instanceof PluralAttributeMapping pluralAttribute ) {
						consumer.accept( pluralAttribute, attributeName + "." + pluralAttribute.getAttributeName() );
					}
					else if ( modelPart instanceof EmbeddedAttributeMapping embeddedAttribute ) {
						visitPluralAttributes(
								embeddedAttribute,
								attributeName + "." + embeddedAttribute.getAttributeName(),
								consumer
						);
					}
				},
				null
		);
	}

	@Deprecated(forRemoval = true, since = "7.1")
	public static TemporaryTable createEntityTable(
			EntityMappingType entityDescriptor,
			Function<String, String> temporaryTableNameAdjuster,
			Dialect dialect,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return createIdTable(
				entityDescriptor,
				temporaryTableNameAdjuster,
				dialect.getSupportedTemporaryTableKind(),
				dialect,
				runtimeModelCreationContext
		);
	}

	public static TemporaryTable createEntityTable(
			EntityMappingType entityDescriptor,
			Function<String, String> temporaryTableNameAdjuster,
			TemporaryTableKind temporaryTableKind,
			Dialect dialect,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new TemporaryTable(
				entityDescriptor,
				temporaryTableNameAdjuster,
				temporaryTableKind,
				dialect,
				runtimeModelCreationContext,
				temporaryTable -> {
					final List<TemporaryTableColumn> columns = new ArrayList<>();
					final PersistentClass entityBinding = runtimeModelCreationContext.getBootModel()
							.getEntityBinding( entityDescriptor.getEntityName() );

					final Generator identifierGenerator = entityDescriptor.getEntityPersister().getGenerator();
					final boolean identityColumn = identifierGenerator.generatedOnExecution();
					final boolean hasOptimizer;
					if ( identityColumn ) {
						hasOptimizer = false;
						final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
						int idIdx = 0;
						for ( Column column : entityBinding.getKey().getColumns() ) {
							final JdbcMapping jdbcMapping = identifierMapping.getJdbcMapping( idIdx++ );
							String sqlTypeName = "";
							if ( dialect.getIdentityColumnSupport().hasDataTypeInIdentityColumn() ) {
								sqlTypeName = column.getSqlType( runtimeModelCreationContext.getMetadata() ) + " ";
							}
							sqlTypeName = sqlTypeName + dialect.getIdentityColumnSupport().getIdentityColumnString( column.getSqlTypeCode( runtimeModelCreationContext.getMetadata() ) );
							columns.add(
									new TemporaryTableColumn(
											temporaryTable,
											ENTITY_TABLE_IDENTITY_COLUMN,
											jdbcMapping,
											sqlTypeName,
											column.getColumnSize(
													dialect,
													runtimeModelCreationContext.getMetadata()
											),
											// Always report as nullable as the identity column string usually includes the not null constraint
											true,//column.isNullable()
											true
									)
							);
						}
					}
					else {
						if ( identifierGenerator instanceof OptimizableGenerator optimizableGenerator ) {
							final Optimizer optimizer = optimizableGenerator.getOptimizer();
							hasOptimizer = optimizer != null && optimizer.getIncrementSize() > 1;
						}
						else {
							hasOptimizer = false;
						}
					}
					final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
					final String idName;
					if ( identifierMapping instanceof SingleAttributeIdentifierMapping ) {
						idName = identifierMapping.getAttributeName();
					}
					else {
						idName = "id";
					}
					forEachTemporaryTableColumn( runtimeModelCreationContext, temporaryTable, idName, identifierMapping, temporaryTableColumn -> {
						columns.add( new TemporaryTableColumn(
								temporaryTableColumn.getContainingTable(),
								temporaryTableColumn.getColumnName(),
								temporaryTableColumn.getJdbcMapping(),
								temporaryTableColumn.getSqlTypeDefinition(),
								temporaryTableColumn.getSize(),
								// We have to set the identity column after the root table insert
								identityColumn || hasOptimizer,
								!identityColumn && !hasOptimizer
						) );
					});

					final EntityDiscriminatorMapping discriminatorMapping = entityDescriptor.getDiscriminatorMapping();
					if ( discriminatorMapping != null && discriminatorMapping.hasPhysicalColumn() && !discriminatorMapping.isFormula() ) {
						forEachTemporaryTableColumn( runtimeModelCreationContext, temporaryTable, "class", discriminatorMapping, temporaryTableColumn -> {
							columns.add( new TemporaryTableColumn(
									temporaryTableColumn.getContainingTable(),
									temporaryTableColumn.getColumnName(),
									temporaryTableColumn.getJdbcMapping(),
									temporaryTableColumn.getSqlTypeDefinition(),
									temporaryTableColumn.getSize(),
									// We have to set the identity column after the root table insert
									discriminatorMapping.isNullable()
							) );
						} );
					}

					// Collect all columns for all entity subtype attributes
					entityDescriptor.visitSubTypeAttributeMappings(
							attribute -> {
								if ( !( attribute instanceof PluralAttributeMapping ) ) {
									forEachTemporaryTableColumn( runtimeModelCreationContext, temporaryTable, attribute.getAttributeName(), attribute, columns::add );
								}
							}
					);
					if ( hasOptimizer ) {
						final TypeConfiguration typeConfiguration = runtimeModelCreationContext.getTypeConfiguration();
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
							LOG.multiTableInsertNotAvailable( entityBinding.getEntityName() );
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
										"rn_",
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

	private static void forEachTemporaryTableColumn(RuntimeModelCreationContext runtimeModelCreationContext, TemporaryTable temporaryTable, String prefix, ModelPart modelPart, Consumer<TemporaryTableColumn> consumer) {
		final Dialect dialect = runtimeModelCreationContext.getDialect();
		final EntityPersister declaringPersister = modelPart.findContainingEntityMapping().getEntityPersister();
		SqmMutationStrategyHelper.forEachSelectableMapping( prefix, modelPart, (columnName, selectableMapping) -> {
			final String tableExpression = selectableMapping.getContainingTableExpression();
			final String tableName =
					tableExpression.charAt( 0 ) == '(' && declaringPersister instanceof UnionSubclassEntityPersister
							? declaringPersister.getRootTableName() : tableExpression;
			final Table table = findTable( runtimeModelCreationContext, tableName );
			final Column column = table.getColumn( Identifier.toIdentifier( selectableMapping.getSelectionExpression() ) );
			consumer.accept(
					new TemporaryTableColumn(
							temporaryTable,
							columnName,
							selectableMapping.getJdbcMapping(),
							column.getSqlType(
									runtimeModelCreationContext.getMetadata()
							),
							column.getColumnSize(
									dialect,
									runtimeModelCreationContext.getMetadata()
							),
							// Treat regular temporary table columns as nullable for simplicity
							true
					)
			);
		} );
	}

	private static Table findTable(RuntimeModelCreationContext runtimeModelCreationContext, String tableName) {
		final Database database = runtimeModelCreationContext.getMetadata().getDatabase();
		final QualifiedNameParser.NameParts nameParts = QualifiedNameParser.INSTANCE.parse( tableName );
		// Strip off the default catalog and schema names since these are not reflected in the Database#namespaces
		final SqlStringGenerationContext sqlContext = runtimeModelCreationContext.getSqlStringGenerationContext();
		final Identifier catalog =
				nameParts.getCatalogName() == null || nameParts.getCatalogName().equals( sqlContext.getDefaultCatalog() )
						? null : nameParts.getCatalogName();
		final Identifier schema =
				nameParts.getSchemaName() == null || nameParts.getSchemaName().equals( sqlContext.getDefaultSchema() )
						? null : nameParts.getSchemaName();
		final Identifier tableNameIdentifier = nameParts.getObjectName();
		Namespace namespace = database.findNamespace( catalog, schema );
		if ( namespace == null ) {
			// When parsing a name, we assume a qualifier is a schema, but could maybe be a catalog
			if ( schema != null && catalog == null ) {
				final Identifier alternativeCatalog = schema.equals( sqlContext.getDefaultCatalog() ) ? null : schema;
				namespace = database.findNamespace( alternativeCatalog, null );
			}
			if ( namespace == null ) {
				throw new IllegalArgumentException( "Unable to find namespace for " + tableName );
			}
		}
		final Table table = namespace.locateTable( tableNameIdentifier );
		// The tableNameIdentifier is a physical name, so double check if it is correct
		if ( table != null && table.getNameIdentifier().equals( tableNameIdentifier ) ) {
			return table;
		}
		else {
			// Fallback to searching for table by comparing physical names
			for ( Table namespaceTable : namespace.getTables() ) {
				if ( tableNameIdentifier.equals( namespaceTable.getNameIdentifier() ) ) {
					return namespaceTable;
				}
			}

			throw new IllegalArgumentException( "No table with name [" + nameParts.getObjectName() + "] found in namespace [" + namespace.getName() + "]" );
		}
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

	public EntityMappingType getEntityDescriptor() {
		return entityDescriptor;
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
		return entityDescriptor.getContributor();
	}

	@Override
	public String getExportIdentifier() {
		return getQualifiedTableName();
	}

	public Dialect getDialect() {
		return this.dialect;
	}
}
