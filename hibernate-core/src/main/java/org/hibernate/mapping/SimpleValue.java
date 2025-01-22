/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.hibernate.AssertionFailure;
import org.hibernate.FetchMode;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.type.TimeZoneStorageStrategy;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.boot.model.convert.internal.ConverterDescriptors;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
import org.hibernate.boot.model.internal.AnnotatedJoinColumns;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.generator.Generator;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.JdbcTypeNameMapper;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;
import org.hibernate.type.internal.ParameterizedTypeImpl;
import org.hibernate.type.MappingContext;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.DynamicParameterizedType;

import jakarta.persistence.AttributeConverter;
import org.hibernate.usertype.DynamicParameterizedType.ParameterType;

import static java.lang.Boolean.parseBoolean;
import static org.hibernate.boot.model.convert.spi.ConverterDescriptor.TYPE_NAME_PREFIX;
import static org.hibernate.boot.model.internal.GeneratorBinder.ASSIGNED_GENERATOR_NAME;
import static org.hibernate.boot.model.internal.GeneratorBinder.ASSIGNED_IDENTIFIER_GENERATOR_CREATOR;
import static org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl.fromExplicit;
import static org.hibernate.internal.util.ReflectHelper.reflectedPropertyClass;
import static org.hibernate.internal.util.collections.ArrayHelper.toBooleanArray;
import static org.hibernate.mapping.MappingHelper.classForName;
import static org.hibernate.models.spi.TypeDetails.Kind.PARAMETERIZED_TYPE;
import static org.hibernate.type.descriptor.jdbc.LobTypeMappings.getLobCodeTypeMapping;
import static org.hibernate.type.descriptor.jdbc.LobTypeMappings.isMappedToKnownLobCode;
import static org.hibernate.type.descriptor.jdbc.NationalizedTypeMappings.toNationalizedTypeCode;

/**
 * A mapping model object that represents any value that maps to columns.
 *
 * @author Gavin King
 * @author Yanming Zhou
 */
public abstract class SimpleValue implements KeyValue {

	@Deprecated(since = "7.0", forRemoval = true)
	public static final String DEFAULT_ID_GEN_STRATEGY = ASSIGNED_GENERATOR_NAME;

	private final MetadataBuildingContext buildingContext;
	private final MetadataImplementor metadata;

	private final List<Selectable> columns = new ArrayList<>();
	private final List<Boolean> insertability = new ArrayList<>();
	private final List<Boolean> updatability = new ArrayList<>();
	private boolean partitionKey;

	private String typeName;
	private Properties typeParameters;
	private Annotation typeAnnotation;
	private boolean isVersion;
	private boolean isNationalized;
	private boolean isLob;

	private NullValueSemantic nullValueSemantic;
	private String nullValue;

	private Table table;
	private String foreignKeyName;
	private String foreignKeyDefinition;
	private String foreignKeyOptions;
	private boolean alternateUniqueKey;
	private OnDeleteAction onDeleteAction;
	private boolean foreignKeyEnabled = true;

	private ConverterDescriptor<?,?> attributeConverterDescriptor;
	private Type type;

	private GeneratorCreator customIdGeneratorCreator = ASSIGNED_IDENTIFIER_GENERATOR_CREATOR;

	public SimpleValue(MetadataBuildingContext buildingContext) {
		this.buildingContext = buildingContext;
		this.metadata = buildingContext.getMetadataCollector();
	}

	public SimpleValue(MetadataBuildingContext buildingContext, Table table) {
		this( buildingContext );
		this.table = table;
	}

	protected SimpleValue(SimpleValue original) {
		this.buildingContext = original.buildingContext;
		this.metadata = original.metadata;
		this.columns.addAll( original.columns );
		this.insertability.addAll( original.insertability );
		this.updatability.addAll( original.updatability );
		this.partitionKey = original.partitionKey;
		this.typeName = original.typeName;
		this.typeParameters = original.typeParameters == null ? null : new Properties( original.typeParameters );
		this.typeAnnotation = original.typeAnnotation;
		this.isVersion = original.isVersion;
		this.isNationalized = original.isNationalized;
		this.isLob = original.isLob;
		this.nullValue = original.nullValue;
		this.table = original.table;
		this.foreignKeyName = original.foreignKeyName;
		this.foreignKeyDefinition = original.foreignKeyDefinition;
		this.foreignKeyEnabled = original.foreignKeyEnabled;
		this.alternateUniqueKey = original.alternateUniqueKey;
		this.onDeleteAction = original.onDeleteAction;
		this.attributeConverterDescriptor = original.attributeConverterDescriptor;
		this.type = original.type;
		this.customIdGeneratorCreator = original.customIdGeneratorCreator;
		this.nullValueSemantic = original.nullValueSemantic;
		this.foreignKeyOptions = original.foreignKeyOptions;
	}

	@Override
	public MetadataBuildingContext getBuildingContext() {
		return buildingContext;
	}

	BootstrapContext getBootstrapContext() {
		return getBuildingContext().getBootstrapContext();
	}

	public MetadataImplementor getMetadata() {
		return metadata;
	}

	InFlightMetadataCollector getMetadataCollector() {
		return getBuildingContext().getMetadataCollector();
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return getMetadata().getMetadataBuildingOptions().getServiceRegistry();
	}

	public TypeConfiguration getTypeConfiguration() {
		return getBootstrapContext().getTypeConfiguration();
	}

	public void setOnDeleteAction(OnDeleteAction onDeleteAction) {
		this.onDeleteAction = onDeleteAction;
	}

	public OnDeleteAction getOnDeleteAction() {
		return onDeleteAction;
	}

	@Override
	public boolean isCascadeDeleteEnabled() {
		return onDeleteAction == OnDeleteAction.CASCADE;
	}

	public void addColumn(Column column) {
		addColumn( column, true, true );
	}

	public void addColumn(Column column, boolean isInsertable, boolean isUpdatable) {
		justAddColumn( column, isInsertable, isUpdatable );
		column.setValue( this );
		column.setTypeIndex( columns.size() - 1 );
	}

	public void addFormula(Formula formula) {
		justAddFormula( formula );
	}

	protected void justAddColumn(Column column) {
		justAddColumn( column, true, true );
	}

	protected void justAddColumn(Column column, boolean insertable, boolean updatable) {
		final int index = columns.indexOf( column );
		if ( index == -1 ) {
			columns.add( column );
			insertability.add( insertable );
			updatability.add( updatable );
		}
		else {
			if ( insertability.get( index ) != insertable ) {
				throw new IllegalStateException( "Same column is added more than once with different values for isInsertable" );
			}
			if ( updatability.get( index ) != updatable ) {
				throw new IllegalStateException( "Same column is added more than once with different values for isUpdatable" );
			}
		}
	}

	protected void justAddFormula(Formula formula) {
		columns.add( formula );
		insertability.add( false );
		updatability.add( false );
	}

	public void sortColumns(int[] originalOrder) {
		if ( columns.size() > 1 ) {
			final var originalColumns = columns.toArray( new Selectable[0] );
			final boolean[] originalInsertability = toBooleanArray( insertability );
			final boolean[] originalUpdatability = toBooleanArray( updatability );
			for ( int i = 0; i < originalOrder.length; i++ ) {
				final int originalIndex = originalOrder[i];
				final var selectable = originalColumns[i];
				if ( selectable instanceof Column column ) {
					column.setTypeIndex( originalIndex );
				}
				columns.set( originalIndex, selectable );
				insertability.set( originalIndex, originalInsertability[i] );
				updatability.set( originalIndex, originalUpdatability[i] );
			}
		}
	}

	@Override
	public boolean hasFormula() {
		for ( var selectable : getSelectables() ) {
			if ( selectable instanceof Formula ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int getColumnSpan() {
		return columns.size();
	}

	protected Selectable getColumn(int position){
		return columns.get( position );
	}

	@Override
	public List<Selectable> getSelectables() {
		return columns;
	}

	@Override
	public List<Column> getColumns() {
		if ( hasFormula() ) {
			// in principle this method should never get called
			// if we have formulas in the mapping
			throw new AssertionFailure("value involves formulas");
		}
		//noinspection unchecked, rawtypes
		return (List) columns;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		if ( typeName != null && typeName.startsWith( TYPE_NAME_PREFIX ) ) {
			setAttributeConverterDescriptor( typeName );
		}
		else {
			this.typeName = typeName;
		}
	}

	void setAttributeConverterDescriptor(String typeName) {
		final String converterClassName = typeName.substring( TYPE_NAME_PREFIX.length() );
		final var bootstrapContext = getBootstrapContext();
		@SuppressWarnings("unchecked") // Completely safe
		final var clazz =
				(Class<? extends AttributeConverter<?,?>>)
						classForName( AttributeConverter.class, converterClassName, bootstrapContext );
		attributeConverterDescriptor =
				ConverterDescriptors.of( clazz, null, false,
						bootstrapContext.getClassmateContext() );
	}

	ClassLoaderService classLoaderService() {
		return getBootstrapContext().getClassLoaderService();
	}

	public void makeVersion() {
		this.isVersion = true;
	}

	public boolean isVersion() {
		return isVersion;
	}

	public void makeNationalized() {
		this.isNationalized = true;
	}

	public boolean isNationalized() {
		return isNationalized;
	}

	public void makeLob() {
		this.isLob = true;
	}

	public boolean isLob() {
		return isLob;
	}

	public void setTable(Table table) {
		this.table = table;
	}

	@Override
	public void createForeignKey() throws MappingException {}

	public void createForeignKey(PersistentClass referencedEntity, AnnotatedJoinColumns joinColumns) {}

	@Override
	public ForeignKey createForeignKeyOfEntity(String entityName) {
		if ( isConstrained() ) {
			final var foreignKey = table.createForeignKey(
					getForeignKeyName(),
					getConstraintColumns(),
					entityName,
					getForeignKeyDefinition(),
					getForeignKeyOptions()
			);
			foreignKey.setOnDeleteAction( onDeleteAction );
			return foreignKey;
		}
		else {
			return null;
		}
	}

	@Override
	public ForeignKey createForeignKeyOfEntity(String entityName, List<Column> referencedColumns) {
		if ( isConstrained() ) {
			final var foreignKey = table.createForeignKey(
					getForeignKeyName(),
					getConstraintColumns(),
					entityName,
					getForeignKeyDefinition(),
					getForeignKeyOptions(),
					referencedColumns
			);
			foreignKey.setOnDeleteAction( onDeleteAction );
			return foreignKey;
		}

		return null;
	}

	@Override
	public void createUniqueKey(MetadataBuildingContext context) {
		if ( hasFormula() ) {
			throw new MappingException( "Unique key constraint involves formulas" );
		}
		getTable().createUniqueKey( getConstraintColumns(), context );
	}

	@Internal
	public void setCustomIdGeneratorCreator(GeneratorCreator customIdGeneratorCreator) {
		this.customIdGeneratorCreator = customIdGeneratorCreator;
	}

	@Internal
	public GeneratorCreator getCustomIdGeneratorCreator() {
		return customIdGeneratorCreator;
	}

	@Deprecated(since = "7.0", forRemoval = true)
	@Override @SuppressWarnings("removal")
	public Generator createGenerator(Dialect dialect, RootClass rootClass) {
		return createGenerator( dialect, rootClass, null, new GeneratorSettings() {
			@Override
			public String getDefaultCatalog() {
				return null;
			}

			@Override
			public String getDefaultSchema() {
				return null;
			}

			@Override
			public SqlStringGenerationContext getSqlStringGenerationContext() {
				final var database = buildingContext.getMetadataCollector().getDatabase();
				return fromExplicit( database.getJdbcEnvironment(), database, getDefaultCatalog(), getDefaultSchema() );
			}
		} );
	}

	@Override
	public Generator createGenerator(
			Dialect dialect,
			RootClass rootClass,
			Property property,
			GeneratorSettings defaults) {
		if ( customIdGeneratorCreator != null ) {
			final var context = new IdGeneratorCreationContext( this, rootClass, property, defaults );
			final var generator = customIdGeneratorCreator.createGenerator( context );
			if ( generator.allowAssignedIdentifiers() && nullValue == null ) {
				setNullValueUndefined();
			}
			return generator;
		}
		else {
			return null;
		}
	}

	@Internal
	public void setColumnToIdentity() {
		if ( getColumnSpan() != 1 ) {
			throw new MappingException( "Identity generation requires exactly one column" );
		}
		else if ( getColumn(0) instanceof Column column ) {
			column.setIdentity( true );
		}
		else {
			throw new MappingException( "Identity generation requires a column" );
		}
	}

	@Override
	public boolean isUpdateable() {
		//needed to satisfy KeyValue
		return true;
	}

	@Override
	public FetchMode getFetchMode() {
		return FetchMode.SELECT;
	}

	@Override
	public Table getTable() {
		return table;
	}

	/**
	 * The property or field value which indicates that field
	 * or property has never been set.
	 *
	 * @see org.hibernate.engine.internal.UnsavedValueFactory
	 * @see org.hibernate.engine.spi.IdentifierValue
	 * @see org.hibernate.engine.spi.VersionValue
	 */
	@Override
	public String getNullValue() {
		return nullValue;
	}

	/**
	 * Set the property or field value indicating that field
	 * or property has never been set.
	 *
	 * @see org.hibernate.engine.internal.UnsavedValueFactory
	 * @see org.hibernate.engine.spi.IdentifierValue
	 * @see org.hibernate.engine.spi.VersionValue
	 */
	public void setNullValue(String nullValue) {
		nullValueSemantic = decodeNullValueSemantic( nullValue );
		if ( nullValueSemantic == NullValueSemantic.VALUE ) {
			this.nullValue = nullValue;
		}
	}

	private static NullValueSemantic decodeNullValueSemantic(String nullValue) {
		return switch ( nullValue ) {
			// magical values (legacy of hbm.xml)
			case "null" -> NullValueSemantic.NULL;
			case "none" -> NullValueSemantic.NONE;
			case "any" -> NullValueSemantic.ANY;
			case "undefined" -> NullValueSemantic.UNDEFINED;
			default -> NullValueSemantic.VALUE;
		};
	}

	/**
	 * The rule for determining if the field or
	 * property has been set.
	 *
	 * @see org.hibernate.engine.internal.UnsavedValueFactory
	 */
	@Override
	public NullValueSemantic getNullValueSemantic() {
		return nullValueSemantic;
	}

	/**
	 * Specifies the rule for determining if the field or
	 * property has been set.
	 *
	 * @see org.hibernate.engine.internal.UnsavedValueFactory
	 */
	public void setNullValueSemantic(NullValueSemantic nullValueSemantic) {
		this.nullValueSemantic = nullValueSemantic;
	}

	/**
	 * Specifies that there is no well-defined property or
	 * field value indicating that field or property has never
	 * been set.
	 *
	 * @see org.hibernate.engine.internal.UnsavedValueFactory
	 * @see org.hibernate.engine.spi.IdentifierValue#UNDEFINED
	 * @see org.hibernate.engine.spi.VersionValue#UNDEFINED
	 */
	public void setNullValueUndefined() {
		nullValueSemantic = NullValueSemantic.UNDEFINED;
	}

	public String getForeignKeyName() {
		return foreignKeyName;
	}

	public void setForeignKeyName(String foreignKeyName) {
		this.foreignKeyName = foreignKeyName;
	}

	public boolean isForeignKeyEnabled() {
		return foreignKeyEnabled;
	}

	public void disableForeignKey() {
		this.foreignKeyEnabled = false;
	}

	public boolean isConstrained() {
		return isForeignKeyEnabled() && !hasFormula();
	}

	public String getForeignKeyOptions() {
		return foreignKeyOptions;
	}

	public void setForeignKeyOptions(String foreignKeyOptions) {
		this.foreignKeyOptions = foreignKeyOptions;
	}

	public String getForeignKeyDefinition() {
		return foreignKeyDefinition;
	}

	public void setForeignKeyDefinition(String foreignKeyDefinition) {
		this.foreignKeyDefinition = foreignKeyDefinition;
	}

	@Override
	public boolean isAlternateUniqueKey() {
		return alternateUniqueKey;
	}

	public void setAlternateUniqueKey(boolean unique) {
		this.alternateUniqueKey = unique;
	}

	@Override
	public boolean isNullable() {
		for ( var selectable : getSelectables() ) {
			if ( selectable instanceof Formula ) {
				// if there are *any* formulas, then the Value overall is
				// considered nullable
				return true;
			}
			else if ( selectable instanceof Column column ) {
				if ( !column.isNullable() ) {
					// if there is a single non-nullable column, the Value
					// overall is considered non-nullable.
					return false;
				}
			}
		}
		// nullable by default
		return true;
	}

	@Override
	public boolean isSimpleValue() {
		return true;
	}

	@Override
	public boolean isValid(MappingContext mappingContext) throws MappingException {
		return getColumnSpan() == getType().getColumnSpan( mappingContext );
	}

	protected void setAttributeConverterDescriptor(ConverterDescriptor<?,?> descriptor) {
		this.attributeConverterDescriptor = descriptor;
	}

	protected ConverterDescriptor<?,?> getAttributeConverterDescriptor() {
		return attributeConverterDescriptor;
	}

	@Override
	public void setTypeUsingReflection(String className, String propertyName) throws MappingException {
		// NOTE: this is called as the last piece in setting SimpleValue type information,
		//       and implementations rely on that fact, using it as a signal that all
		//       the information it is going to get is already specified at this point
		if ( typeName == null && type == null ) {
			if ( attributeConverterDescriptor == null ) {
				// This is here to work like legacy. This should change when we integrate with metamodel
				// to look for JdbcType and JavaType individually and create the BasicType (well, really
				// keep a registry of [JdbcType,JavaType] -> BasicType...)
				if ( className == null ) {
					throw new MappingException(
							"Attribute types for a dynamic entity must be explicitly specified: " + propertyName );
				}
				typeName = getClass( className, propertyName ).getName();
				// TODO: To fully support isNationalized here we need to do the process hinted at above
				// 		 essentially, much of the logic from #buildAttributeConverterTypeAdapter wrt
				// 		 resolving a (1) JdbcType, a (2) JavaType and dynamically building a BasicType
				// 		 combining them.
			}
			else {
				// we had an AttributeConverter
				type = buildAttributeConverterTypeAdapter();
			}
		}
		// otherwise assume either
		// (a) explicit type was specified or
		// (b) determine was already performed
	}

	private Class<?> getClass(String className, String propertyName) {
		return reflectedPropertyClass( className, propertyName, classLoaderService() );
	}

	/**
	 * Build a Hibernate Type that incorporates the JPA AttributeConverter.  AttributeConverter works totally in
	 * memory, meaning it converts between one Java representation (the entity attribute representation) and another
	 * (the value bound into JDBC statements or extracted from results).  However, the Hibernate Type system operates
	 * at the lower level of actually dealing directly with those JDBC objects.  So even though we have an
	 * AttributeConverter, we still need to "fill out" the rest of the BasicType data and bridge calls
	 * to bind/extract through the converter.
	 * <p>
	 * Essentially the idea here is that an intermediate Java type needs to be used.  Let's use an example as a means
	 * to illustrate...  Consider an {@code AttributeConverter<Integer,String>}.  This tells Hibernate that the domain
	 * model defines this attribute as an Integer value (the 'entityAttributeJavaType'), but that we need to treat the
	 * value as a String (the 'databaseColumnJavaType') when dealing with JDBC (aka, the database type is a
	 * VARCHAR/CHAR):<ul>
	 *     <li>
	 *         When binding values to PreparedStatements we need to convert the Integer value from the entity
	 *         into a String and pass that String to setString.  The conversion is handled by calling
	 *         {@link AttributeConverter#convertToDatabaseColumn(Object)}
	 *     </li>
	 *     <li>
	 *         When extracting values from ResultSets (or CallableStatement parameters) we need to handle the
	 *         value via getString, and convert that returned String to an Integer.  That conversion is handled
	 *         by calling {@link AttributeConverter#convertToEntityAttribute(Object)}
	 *     </li>
	 * </ul>
	 *
	 * @return The built AttributeConverter -> Type adapter
	 */
	// @todo : ultimately I want to see attributeConverterJavaType and attributeConverterJdbcTypeCode specifiable separately
	//         then we can "play them against each other" in terms of determining proper typing
	// @todo : see if we already have previously built a custom on-the-fly BasicType for this AttributeConverter;
	//         see note below about caching
	private Type buildAttributeConverterTypeAdapter() {
		// todo : validate the number of columns present here?
		return buildAttributeConverterTypeAdapter( attributeConverterDescriptor.createJpaAttributeConverter(
				new JpaAttributeConverterCreationContext() {
					@Override
					public ManagedBeanRegistry getManagedBeanRegistry() {
						return getBootstrapContext().getManagedBeanRegistry();
					}

					@Override
					public TypeConfiguration getTypeConfiguration() {
						return getMetadata().getTypeConfiguration();
					}
				}
		) );
	}

	private <T> Type buildAttributeConverterTypeAdapter(
			JpaAttributeConverter<T, ?> jpaAttributeConverter) {
		final var domainJavaType = jpaAttributeConverter.getDomainJavaType();
		final var relationalJavaType = jpaAttributeConverter.getRelationalJavaType();

		// build the SqlTypeDescriptor adapter ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Going back to the illustration, this should be a SqlTypeDescriptor that handles the Integer <-> String
		//		conversions.  This is the more complicated piece.  First we need to determine the JDBC type code
		//		corresponding to the AttributeConverter's declared "databaseColumnJavaType" (how we read that value out
		// 		of ResultSets).  See JdbcTypeJavaClassMappings for details.  Again, given example, this should return
		// 		VARCHAR/CHAR
		final var recommendedJdbcType = relationalJavaType.getRecommendedJdbcType(
				// todo (6.0) : handle the other JdbcRecommendedSqlTypeMappingContext methods
				new JdbcTypeIndicators() {
					@Override
					public TypeConfiguration getTypeConfiguration() {
						return metadata.getTypeConfiguration();
					}

					@Override
					public TimeZoneStorageStrategy getDefaultTimeZoneStorageStrategy() {
						return buildingContext.getBuildingOptions().getDefaultTimeZoneStorage();
					}

					@Override
					public Dialect getDialect() {
						return buildingContext.getMetadataCollector().getDatabase().getDialect();
					}
				}
		);

		// todo : cache the AttributeConverterTypeAdapter in case that AttributeConverter is applied multiple times.
		return new ConvertedBasicTypeImpl<>(
				TYPE_NAME_PREFIX
						+ jpaAttributeConverter.getConverterJavaType().getTypeName(),
				String.format(
						"BasicType adapter for AttributeConverter<%s,%s>",
						domainJavaType.getTypeName(),
						relationalJavaType.getTypeName()
				),
				metadata.getTypeConfiguration().getJdbcTypeRegistry()
						.getDescriptor( jdbcTypeCode( recommendedJdbcType, domainJavaType ) ),
				jpaAttributeConverter
		);
	}

	private <T> int jdbcTypeCode(JdbcType recommendedJdbcType, JavaType<T> domainJavaType) {
		final int recommendedDdlTypeCode = recommendedJdbcType.getDdlTypeCode();
		final int jdbcTypeCode;
		if ( isLob() ) {
			if ( isMappedToKnownLobCode( recommendedDdlTypeCode ) ) {
				jdbcTypeCode = getLobCodeTypeMapping( recommendedDdlTypeCode );
			}
			else {
				if ( Serializable.class.isAssignableFrom( domainJavaType.getJavaTypeClass() ) ) {
					jdbcTypeCode = Types.BLOB;
				}
				else {
					throw new IllegalArgumentException(
							String.format(
									Locale.ROOT,
									"JDBC type-code [%s (%s)] not known to have a corresponding LOB equivalent, and Java type is not Serializable (to use BLOB)",
									recommendedDdlTypeCode,
									JdbcTypeNameMapper.getTypeName( recommendedDdlTypeCode )
							)
					);
				}
			}
		}
		else {
			jdbcTypeCode = recommendedDdlTypeCode;
		}
		return isNationalized() ? toNationalizedTypeCode( jdbcTypeCode ) : jdbcTypeCode;
	}

	public boolean isTypeSpecified() {
		return typeName != null;
	}

	public void setTypeParameters(Properties parameterMap) {
		this.typeParameters = parameterMap;
	}

	public void setTypeParameters(Map<String, ?> parameters) {
		if ( parameters != null ) {
			final var properties = new Properties();
			properties.putAll( parameters );
			setTypeParameters( properties );
		}
	}

	public void setTypeAnnotation(Annotation typeAnnotation) {
		this.typeAnnotation = typeAnnotation;
	}

	public Properties getTypeParameters() {
		return typeParameters;
	}

	public Annotation getTypeAnnotation() {
		return typeAnnotation;
	}

	public void copyTypeFrom(SimpleValue sourceValue ) {
		setTypeName( sourceValue.getTypeName() );
		setTypeParameters( sourceValue.getTypeParameters() );

		type = sourceValue.type;
		attributeConverterDescriptor = sourceValue.attributeConverterDescriptor;
	}

	@Override
	public boolean isSame(Value other) {
		return this == other
			|| other instanceof SimpleValue simpleValue && isSame( simpleValue );
	}

	protected static boolean isSame(Value v1, Value v2) {
		return v1 == v2 || v1 != null && v2 != null && v1.isSame( v2 );
	}

	public boolean isSame(SimpleValue other) {
		return Objects.equals( columns, other.columns )
			&& Objects.equals( typeName, other.typeName )
			&& Objects.equals( typeParameters, other.typeParameters )
			&& Objects.equals( typeAnnotation, other.typeAnnotation )
			&& Objects.equals( table, other.table )
			&& Objects.equals( foreignKeyName, other.foreignKeyName )
			&& Objects.equals( foreignKeyDefinition, other.foreignKeyDefinition );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '(' + columns + ')';
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	@Override
	public boolean[] getColumnInsertability() {
		return extractBooleansFromList( insertability );
	}

	@Override
	public boolean hasAnyInsertableColumns() {
		//noinspection ForLoopReplaceableByForEach
		for ( int i = 0; i < insertability.size(); i++ ) {
			if ( insertability.get( i ) ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean[] getColumnUpdateability() {
		return extractBooleansFromList( updatability );
	}

	@Override
	public boolean hasAnyUpdatableColumns() {
		for ( int i = 0; i < updatability.size(); i++ ) {
			if ( updatability.get( i ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isColumnInsertable(int index) {
		return !insertability.isEmpty() && insertability.get( index );
	}

	@Override
	public boolean isColumnUpdateable(int index) {
		return !updatability.isEmpty() && updatability.get( index );
	}

	public boolean isPartitionKey() {
		return partitionKey;
	}

	public void setPartitionKey(boolean partitionColumn) {
		this.partitionKey = partitionColumn;
	}

	private static boolean[] extractBooleansFromList(List<Boolean> list) {
		final boolean[] array = new boolean[ list.size() ];
		int i = 0;
		for ( Boolean value : list ) {
			array[ i++ ] = value;
		}
		return array;
	}

	public ConverterDescriptor<?,?> getJpaAttributeConverterDescriptor() {
		return attributeConverterDescriptor;
	}

	public void setJpaAttributeConverterDescriptor(ConverterDescriptor<?,?> descriptor) {
		this.attributeConverterDescriptor = descriptor;
	}

	private static final Annotation[] NO_ANNOTATIONS = new Annotation[0];
	private static Annotation[] getAnnotations(MemberDetails memberDetails) {
		final var directAnnotationUsages =
				memberDetails == null
						? null
						: memberDetails.getDirectAnnotationUsages();
		return directAnnotationUsages == null
				? NO_ANNOTATIONS
				: directAnnotationUsages.toArray( Annotation[]::new );
	}

	protected ParameterType createParameterType() {
		try {
			final String[] columnNames = new String[ columns.size() ];
			final Long[] columnLengths = new Long[ columns.size() ];
			for ( int i = 0; i < columns.size(); i++ ) {
				final var selectable = columns.get(i);
				if ( selectable instanceof Column column ) {
					columnNames[i] = column.getName();
					columnLengths[i] = column.getLength();
				}
			}
			// todo : not sure this works for handling @MapKeyEnumerated
			return createParameterType( columnNames, columnLengths );
		}
		catch ( ClassLoadingException e ) {
			throw new MappingException( "Could not create DynamicParameterizedType for type: " + typeName, e );
		}
	}

	private ParameterType createParameterType(String[] columnNames, Long[] columnLengths) {
		final var attribute = (MemberDetails) typeParameters.get( DynamicParameterizedType.XPROPERTY );
		return new ParameterTypeImpl(
				classLoaderService()
						.classForTypeName( typeParameters.getProperty( DynamicParameterizedType.RETURNED_CLASS ) ),
				attribute != null ? attribute.getType() : null,
				getAnnotations( attribute ),
				table.getCatalog(),
				table.getSchema(),
				table.getName(),
				parseBoolean( typeParameters.getProperty( DynamicParameterizedType.IS_PRIMARY_KEY ) ),
				columnNames,
				columnLengths
		);
	}

	private static final class ParameterTypeImpl implements ParameterType {

		private final Class<?> returnedClass;
		private final java.lang.reflect.Type returnedJavaType;
		private final Annotation[] annotationsMethod;
		private final String catalog;
		private final String schema;
		private final String table;
		private final boolean primaryKey;
		private final String[] columns;
		private final Long[] columnLengths;

		private ParameterTypeImpl(
				Class<?> returnedClass,
				TypeDetails returnedTypeDetails,
				Annotation[] annotationsMethod,
				String catalog,
				String schema,
				String table,
				boolean primaryKey,
				String[] columns,
				Long[] columnLengths) {
			this.returnedClass = returnedClass;
			this.annotationsMethod = annotationsMethod;
			this.catalog = catalog;
			this.schema = schema;
			this.table = table;
			this.primaryKey = primaryKey;
			this.columns = columns;
			this.columnLengths = columnLengths;

			if ( returnedTypeDetails == null ) {
				returnedJavaType = null;
			}
			else {
				returnedJavaType =
						returnedTypeDetails.getTypeKind() == PARAMETERIZED_TYPE
								? ParameterizedTypeImpl.from( returnedTypeDetails.asParameterizedType() )
								: returnedTypeDetails.determineRawClass().toJavaClass();
			}
		}

		@Override
		public Class<?> getReturnedClass() {
			return returnedClass;
		}

		@Override
		public java.lang.reflect.Type getReturnedJavaType() {
			return returnedJavaType;
		}

		@Override
		public Annotation[] getAnnotationsMethod() {
			return annotationsMethod;
		}

		@Override
		public String getCatalog() {
			return catalog;
		}

		@Override
		public String getSchema() {
			return schema;
		}

		@Override
		public String getTable() {
			return table;
		}

		@Override
		public boolean isPrimaryKey() {
			return primaryKey;
		}

		@Override
		public String[] getColumns() {
			return columns;
		}

		@Override
		public Long[] getColumnLengths() {
			return columnLengths;
		}
	}

	private class IdGeneratorCreationContext implements GeneratorCreationContext {
		private final SimpleValue identifier;
		private final RootClass rootClass;
		private final Property property;
		private final GeneratorSettings defaults;

		public IdGeneratorCreationContext(SimpleValue identifier, RootClass rootClass, Property property, GeneratorSettings defaults) {
			this.identifier = identifier;
			this.rootClass = rootClass;
			this.property = property;
			this.defaults = defaults;
		}

		@Override
		public Database getDatabase() {
			return buildingContext.getMetadataCollector().getDatabase();
		}

		@Override
		public ServiceRegistry getServiceRegistry() {
			return buildingContext.getBootstrapContext().getServiceRegistry();
		}

		@Override
		public SqlStringGenerationContext getSqlStringGenerationContext() {
			return defaults.getSqlStringGenerationContext();
		}

		@Override
		public String getDefaultCatalog() {
			return defaults.getDefaultCatalog();
		}

		@Override
		public String getDefaultSchema() {
			return defaults.getDefaultSchema();
		}

		@Override
		public RootClass getRootClass() {
			return rootClass;
		}

		@Override
		public PersistentClass getPersistentClass() {
			return rootClass;
		}

		@Override
		public Property getProperty() {
			return property;
		}

		@Override
		public Value getValue() {
			return identifier;
		}

		@Override
		public Type getType() {
			return SimpleValue.this.getType();
		}

		// we could add this if it helps integrate old infrastructure
//		@Override
//		public Properties getParameters() {
//			final Value value = getProperty().getValue();
//			if ( !value.isSimpleValue() ) {
//				throw new IllegalStateException( "not a simple-valued property" );
//			}
//			final Dialect dialect = getDatabase().getDialect();
//			return collectParameters( (SimpleValue) value, dialect, defaultCatalog, defaultSchema, rootClass );
//		}
//
	}
}
