/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.hibernate.AssertionFailure;
import org.hibernate.FetchMode;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.Remove;
import org.hibernate.TimeZoneStorageStrategy;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.annotations.common.reflection.java.JavaXMember;
import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
import org.hibernate.boot.model.internal.AnnotatedJoinColumns;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.id.factory.spi.CustomIdGeneratorCreationContext;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.generator.Generator;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.JdbcTypeNameMapper;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.LobTypeMappings;
import org.hibernate.type.descriptor.jdbc.NationalizedTypeMappings;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.DynamicParameterizedType;

import jakarta.persistence.AttributeConverter;

import static java.lang.Boolean.parseBoolean;
import static org.hibernate.boot.model.convert.spi.ConverterDescriptor.TYPE_NAME_PREFIX;
import static org.hibernate.id.factory.internal.IdentifierGeneratorUtil.createLegacyIdentifierGenerator;
import static org.hibernate.internal.util.collections.ArrayHelper.toBooleanArray;

/**
 * A mapping model object that represents any value that maps to columns.
 *
 * @author Gavin King
 * @author Yanming Zhou
 */
public abstract class SimpleValue implements KeyValue {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( SimpleValue.class );

	public static final String DEFAULT_ID_GEN_STRATEGY = "assigned";

	private final MetadataBuildingContext buildingContext;
	private final MetadataImplementor metadata;

	private final List<Selectable> columns = new ArrayList<>();
	private final List<Boolean> insertability = new ArrayList<>();
	private final List<Boolean> updatability = new ArrayList<>();
	private boolean partitionKey;

	private String typeName;
	private Properties typeParameters;
	private boolean isVersion;
	private boolean isNationalized;
	private boolean isLob;

	private Map<String,Object> identifierGeneratorParameters;
	private String identifierGeneratorStrategy = DEFAULT_ID_GEN_STRATEGY;
	private String nullValue;

	private Table table;
	private String foreignKeyName;
	private String foreignKeyDefinition;
	private boolean alternateUniqueKey;
	private OnDeleteAction onDeleteAction;
	private boolean foreignKeyEnabled = true;

	private ConverterDescriptor attributeConverterDescriptor;
	private Type type;

	private IdentifierGeneratorCreator customIdGeneratorCreator;
	private Generator generator;

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
		this.isVersion = original.isVersion;
		this.isNationalized = original.isNationalized;
		this.isLob = original.isLob;
		this.identifierGeneratorParameters = original.identifierGeneratorParameters;
		this.identifierGeneratorStrategy = original.identifierGeneratorStrategy;
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
		this.generator = original.generator;
	}

	@Override
	public MetadataBuildingContext getBuildingContext() {
		return buildingContext;
	}

	public MetadataImplementor getMetadata() {
		return metadata;
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return getMetadata().getMetadataBuildingOptions().getServiceRegistry();
	}

	public TypeConfiguration getTypeConfiguration() {
		return getBuildingContext().getBootstrapContext().getTypeConfiguration();
	}

	public void setOnDeleteAction(OnDeleteAction onDeleteAction) {
		this.onDeleteAction = onDeleteAction;
	}

	public OnDeleteAction getOnDeleteAction() {
		return onDeleteAction;
	}

	/**
	 * @deprecated use {@link #getOnDeleteAction()}
	 */
	@Deprecated(since = "6.2")
	@Override
	public boolean isCascadeDeleteEnabled() {
		return onDeleteAction == OnDeleteAction.CASCADE;
	}

	/**
	 * @deprecated use {@link #setOnDeleteAction(OnDeleteAction)}
	 */
	@Deprecated(since = "6.2")
	public void setCascadeDeleteEnabled(boolean cascadeDeleteEnabled) {
		this.onDeleteAction = cascadeDeleteEnabled ? OnDeleteAction.CASCADE : OnDeleteAction.NO_ACTION;
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
		int index = columns.indexOf( column );
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
			final Selectable[] originalColumns = columns.toArray( new Selectable[0] );
			final boolean[] originalInsertability = toBooleanArray( insertability );
			final boolean[] originalUpdatability = toBooleanArray( updatability );
			for ( int i = 0; i < originalOrder.length; i++ ) {
				final int originalIndex = originalOrder[i];
				final Selectable selectable = originalColumns[i];
				if ( selectable instanceof Column ) {
					( (Column) selectable ).setTypeIndex( originalIndex );
				}
				columns.set( originalIndex, selectable );
				insertability.set( originalIndex, originalInsertability[i] );
				updatability.set( originalIndex, originalUpdatability[i] );
			}
		}
	}

	@Override
	public boolean hasFormula() {
		for ( Selectable selectable : getSelectables() ) {
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

	/**
	 * @deprecated Use {@link #getSelectables()} instead
	 */
	@Deprecated(forRemoval = true, since = "6.3")
	public Iterator<Selectable> getConstraintColumnIterator() {
		return getSelectables().iterator();
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		if ( typeName != null && typeName.startsWith( TYPE_NAME_PREFIX ) ) {
			final String converterClassName = typeName.substring( TYPE_NAME_PREFIX.length() );
			final ClassLoaderService cls = getMetadata()
					.getMetadataBuildingOptions()
					.getServiceRegistry()
					.requireService( ClassLoaderService.class );
			try {
				final Class<? extends AttributeConverter<?,?>> converterClass = cls.classForName( converterClassName );
				this.attributeConverterDescriptor = new ClassBasedConverterDescriptor(
						converterClass,
						false,
						( (InFlightMetadataCollector) getMetadata() ).getBootstrapContext().getClassmateContext()
				);
				return;
			}
			catch (Exception e) {
				log.logBadHbmAttributeConverterType( typeName, e.getMessage() );
			}
		}

		this.typeName = typeName;
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

	public void createForeignKey(PersistentClass referencedEntity, AnnotatedJoinColumns joinColumns) throws MappingException {}

	@Override
	public ForeignKey createForeignKeyOfEntity(String entityName) {
		if ( isConstrained() ) {
			final ForeignKey foreignKey = table.createForeignKey(
					getForeignKeyName(),
					getConstraintColumns(),
					entityName,
					getForeignKeyDefinition()
			);
			foreignKey.setOnDeleteAction( onDeleteAction );
			return foreignKey;
		}

		return null;
	}

	@Override
	public void createUniqueKey(MetadataBuildingContext context) {
		if ( hasFormula() ) {
			throw new MappingException( "unique key constraint involves formulas" );
		}
		getTable().createUniqueKey( getConstraintColumns(), context );
	}

	@Internal
	public void setCustomIdGeneratorCreator(IdentifierGeneratorCreator customIdGeneratorCreator) {
		this.customIdGeneratorCreator = customIdGeneratorCreator;
	}

	@Internal
	public IdentifierGeneratorCreator getCustomIdGeneratorCreator() {
		return customIdGeneratorCreator;
	}

	@Override
	public Generator createGenerator(
			IdentifierGeneratorFactory identifierGeneratorFactory,
			Dialect dialect,
			RootClass rootClass) throws MappingException {
		return createGenerator( identifierGeneratorFactory, dialect, rootClass, rootClass == null ? null : rootClass.getIdentifierProperty() );
	}

	@Override
	public Generator createGenerator(
			IdentifierGeneratorFactory identifierGeneratorFactory,
			Dialect dialect,
			RootClass rootClass,
			Property property) throws MappingException {
		if ( generator == null ) {
			if ( customIdGeneratorCreator != null ) {
				generator = customIdGeneratorCreator.createGenerator(
						new IdGeneratorCreationContext( identifierGeneratorFactory, null, null, rootClass, property)
				);
			}
			else {
				generator = createLegacyIdentifierGenerator(this, identifierGeneratorFactory, dialect, null, null, rootClass );
				if ( generator instanceof IdentityGenerator ) {
					setColumnToIdentity();
				}
			}
		}
		return generator;
	}

	private void setColumnToIdentity() {
		if ( getColumnSpan() != 1 ) {
			throw new MappingException( "Identity generation requires exactly one column" );
		}
		final Selectable column = getColumn(0);
		if ( column instanceof Column ) {
			( (Column) column).setIdentity( true );
		}
		else {
			throw new MappingException( "Identity generation requires a column" );
		}
	}

	public boolean isUpdateable() {
		//needed to satisfy KeyValue
		return true;
	}
	
	public FetchMode getFetchMode() {
		return FetchMode.SELECT;
	}

	public Table getTable() {
		return table;
	}

	/**
	 * Returns the identifierGeneratorStrategy.
	 * @return String
	 */
	public String getIdentifierGeneratorStrategy() {
		return identifierGeneratorStrategy;
	}

	/**
	 * Sets the identifierGeneratorStrategy.
	 * @param identifierGeneratorStrategy The identifierGeneratorStrategy to set
	 */
	public void setIdentifierGeneratorStrategy(String identifierGeneratorStrategy) {
		this.identifierGeneratorStrategy = identifierGeneratorStrategy;
	}

	public Map<String, Object> getIdentifierGeneratorParameters() {
		return identifierGeneratorParameters;
	}

	public void setIdentifierGeneratorParameters(Map<String, Object> identifierGeneratorParameters) {
		this.identifierGeneratorParameters = identifierGeneratorParameters;
	}

	/**
	 * @deprecated use {@link #getIdentifierGeneratorParameters()}
	 */
	@Deprecated @Remove
	public Properties getIdentifierGeneratorProperties() {
		Properties properties = new Properties();
		properties.putAll( identifierGeneratorParameters );
		return properties;
	}

	/**
	 * @deprecated use {@link #setIdentifierGeneratorParameters(Map)}
	 */
	@Deprecated @Remove
	public void setIdentifierGeneratorProperties(Properties identifierGeneratorProperties) {
		this.identifierGeneratorParameters = new HashMap<>();
		identifierGeneratorProperties.forEach((key, value) -> {
			if (key instanceof String) {
				identifierGeneratorParameters.put((String) key, value);
			}
		});
	}

	/**
	 * @deprecated use {@link #setIdentifierGeneratorParameters(Map)}
	 */
	@Deprecated @Remove
	public void setIdentifierGeneratorProperties(Map<String,Object> identifierGeneratorProperties) {
		this.identifierGeneratorParameters = identifierGeneratorProperties;
	}

	public String getNullValue() {
		return nullValue;
	}

	/**
	 * Sets the nullValue.
	 * @param nullValue The nullValue to set
	 */
	public void setNullValue(String nullValue) {
		this.nullValue = nullValue;
	}

	public String getForeignKeyName() {
		return foreignKeyName;
	}

	public void setForeignKeyName(String foreignKeyName) {
		// the FK name "none" was a magic value in the hbm.xml
		// mapping language that indicated to not create a FK
		if ( "none".equals( foreignKeyName ) ) {
			foreignKeyEnabled = false;
		}
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

	public String getForeignKeyDefinition() {
		return foreignKeyDefinition;
	}

	public void setForeignKeyDefinition(String foreignKeyDefinition) {
		this.foreignKeyDefinition = foreignKeyDefinition;
	}

	public boolean isAlternateUniqueKey() {
		return alternateUniqueKey;
	}

	public void setAlternateUniqueKey(boolean unique) {
		this.alternateUniqueKey = unique;
	}

	public boolean isNullable() {
		for ( Selectable selectable : getSelectables() ) {
			if ( selectable instanceof Formula ) {
				// if there are *any* formulas, then the Value overall is
				// considered nullable
				return true;
			}
			else if ( !( (Column) selectable ).isNullable() ) {
				// if there is a single non-nullable column, the Value
				// overall is considered non-nullable.
				return false;
			}
		}
		// nullable by default
		return true;
	}

	public boolean isSimpleValue() {
		return true;
	}

	public boolean isValid(Mapping mapping) throws MappingException {
		return getColumnSpan() == getType().getColumnSpan( mapping );
	}

	protected void setAttributeConverterDescriptor(ConverterDescriptor descriptor) {
		this.attributeConverterDescriptor = descriptor;
	}

	protected ConverterDescriptor getAttributeConverterDescriptor() {
		return attributeConverterDescriptor;
	}

	//	public Type getType() throws MappingException {
//		if ( type != null ) {
//			return type;
//		}
//
//		if ( typeName == null ) {
//			throw new MappingException( "No type name" );
//		}
//
//		if ( typeParameters != null
//				&& Boolean.valueOf( typeParameters.getProperty( DynamicParameterizedType.IS_DYNAMIC ) )
//				&& typeParameters.get( DynamicParameterizedType.PARAMETER_TYPE ) == null ) {
//			createParameterImpl();
//		}
//
//		Type result = getMetadata().getTypeConfiguration().getTypeResolver().heuristicType( typeName, typeParameters );
//
//		if ( isVersion && result instanceof BinaryType ) {
//			// if this is a byte[] version/timestamp, then we need to use RowVersionType
//			// instead of BinaryType (HHH-10413)
//			// todo (6.0) - although for T/SQL databases we should use its
//			log.debug( "version is BinaryType; changing to RowVersionType" );
//			result = RowVersionType.INSTANCE;
//		}
//
//		if ( result == null ) {
//			String msg = "Could not determine type for: " + typeName;
//			if ( table != null ) {
//				msg += ", at table: " + table.getName();
//			}
//			if ( columns != null && columns.size() > 0 ) {
//				msg += ", for columns: " + columns;
//			}
//			throw new MappingException( msg );
//		}
//
//		return result;
//	}

	@Override
	public void setTypeUsingReflection(String className, String propertyName) throws MappingException {
		// NOTE : this is called as the last piece in setting SimpleValue type information, and implementations
		// rely on that fact, using it as a signal that all information it is going to get is defined at this point...

		if ( typeName != null ) {
			// assume either (a) explicit type was specified or (b) determine was already performed
			return;
		}

		if ( type != null ) {
			return;
		}

		if ( attributeConverterDescriptor == null ) {
			// this is here to work like legacy.  This should change when we integrate with metamodel to
			// look for JdbcType and JavaType individually and create the BasicType (well, really
			// keep a registry of [JdbcType,JavaType] -> BasicType...)
			if ( className == null ) {
				throw new MappingException( "Attribute types for a dynamic entity must be explicitly specified: " + propertyName );
			}
			typeName = getClass( className, propertyName ).getName();
			// todo : to fully support isNationalized here we need to do the process hinted at above
			// 		essentially, much of the logic from #buildAttributeConverterTypeAdapter wrt resolving
			//		a (1) JdbcType, a (2) JavaType and dynamically building a BasicType
			// 		combining them.
			return;
		}

		// we had an AttributeConverter...
		type = buildAttributeConverterTypeAdapter();
	}

	private Class<?> getClass(String className, String propertyName) {
		return ReflectHelper.reflectedPropertyClass(
				className,
				propertyName,
				getMetadata()
						.getMetadataBuildingOptions()
						.getServiceRegistry()
						.requireService( ClassLoaderService.class )
		);
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
						return getMetadata()
								.getMetadataBuildingOptions()
								.getServiceRegistry()
								.requireService( ManagedBeanRegistry.class );
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
		JavaType<T> domainJavaType = jpaAttributeConverter.getDomainJavaType();
		JavaType<?> relationalJavaType = jpaAttributeConverter.getRelationalJavaType();

		// build the SqlTypeDescriptor adapter ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Going back to the illustration, this should be a SqlTypeDescriptor that handles the Integer <-> String
		//		conversions.  This is the more complicated piece.  First we need to determine the JDBC type code
		//		corresponding to the AttributeConverter's declared "databaseColumnJavaType" (how we read that value out
		// 		of ResultSets).  See JdbcTypeJavaClassMappings for details.  Again, given example, this should return
		// 		VARCHAR/CHAR
		final JdbcType recommendedJdbcType = relationalJavaType.getRecommendedJdbcType(
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
		int jdbcTypeCode = recommendedJdbcType.getDdlTypeCode();
		if ( isLob() ) {
			if ( LobTypeMappings.isMappedToKnownLobCode( jdbcTypeCode ) ) {
				jdbcTypeCode = LobTypeMappings.getLobCodeTypeMapping( jdbcTypeCode );
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
									jdbcTypeCode,
									JdbcTypeNameMapper.getTypeName( jdbcTypeCode )
							)
					);
				}
			}
		}
		if ( isNationalized() ) {
			jdbcTypeCode = NationalizedTypeMappings.toNationalizedTypeCode( jdbcTypeCode );
		}


		// todo : cache the AttributeConverterTypeAdapter in case that AttributeConverter is applied multiple times.
		return new ConvertedBasicTypeImpl<>(
				TYPE_NAME_PREFIX
						+ jpaAttributeConverter.getConverterJavaType().getTypeName(),
				String.format(
						"BasicType adapter for AttributeConverter<%s,%s>",
						domainJavaType.getTypeName(),
						relationalJavaType.getTypeName()
				),
				metadata.getTypeConfiguration().getJdbcTypeRegistry().getDescriptor( jdbcTypeCode ),
				jpaAttributeConverter
		);
	}

	public boolean isTypeSpecified() {
		return typeName != null;
	}

	public void setTypeParameters(Properties parameterMap) {
		this.typeParameters = parameterMap;
	}

	public void setTypeParameters(Map<String, ?> parameters) {
		if ( parameters != null ) {
			Properties properties = new Properties();
			properties.putAll( parameters );
			setTypeParameters( properties );
		}
	}

	public Properties getTypeParameters() {
		return typeParameters;
	}

	public void copyTypeFrom( SimpleValue sourceValue ) {
		setTypeName( sourceValue.getTypeName() );
		setTypeParameters( sourceValue.getTypeParameters() );

		type = sourceValue.type;
		attributeConverterDescriptor = sourceValue.attributeConverterDescriptor;
	}

	@Override
	public boolean isSame(Value other) {
		return this == other || other instanceof SimpleValue && isSame( (SimpleValue) other );
	}

	protected static boolean isSame(Value v1, Value v2) {
		return v1 == v2 || v1 != null && v2 != null && v1.isSame( v2 );
	}

	public boolean isSame(SimpleValue other) {
		return Objects.equals( columns, other.columns )
			&& Objects.equals( typeName, other.typeName )
			&& Objects.equals( typeParameters, other.typeParameters )
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
		if ( insertability.size() > 0 ) {
			return insertability.get( index );
		}
		return false;
	}

	@Override
	public boolean isColumnUpdateable(int index) {
		if ( updatability.size() > 0 ) {
			return updatability.get( index );
		}
		return false;
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

	public ConverterDescriptor getJpaAttributeConverterDescriptor() {
		return attributeConverterDescriptor;
	}

	public void setJpaAttributeConverterDescriptor(ConverterDescriptor descriptor) {
		this.attributeConverterDescriptor = descriptor;
	}

	protected void createParameterImpl() {
		try {
			final String[] columnNames = new String[ columns.size() ];
			final Long[] columnLengths = new Long[ columns.size() ];

			for ( int i = 0; i < columns.size(); i++ ) {
				final Selectable selectable = columns.get(i);
				if ( selectable instanceof Column ) {
					final Column column = (Column) selectable;
					columnNames[i] = column.getName();
					columnLengths[i] = column.getLength();
				}
			}

			final XProperty xProperty = (XProperty) typeParameters.get( DynamicParameterizedType.XPROPERTY );
			// todo : not sure this works for handling @MapKeyEnumerated
			final Annotation[] annotations = xProperty == null
					? new Annotation[0]
					: xProperty.getAnnotations();

			final ClassLoaderService classLoaderService = getMetadata()
					.getMetadataBuildingOptions()
					.getServiceRegistry()
					.requireService( ClassLoaderService.class );
			typeParameters.put(
					DynamicParameterizedType.PARAMETER_TYPE,
					new ParameterTypeImpl(
							classLoaderService.classForTypeName(
									typeParameters.getProperty(DynamicParameterizedType.RETURNED_CLASS)
							),
							xProperty instanceof JavaXMember ? ((JavaXMember) xProperty ).getJavaType() : null,
							annotations,
							table.getCatalog(),
							table.getSchema(),
							table.getName(),
							parseBoolean(typeParameters.getProperty(DynamicParameterizedType.IS_PRIMARY_KEY)),
							columnNames,
							columnLengths
					)
			);
		}
		catch ( ClassLoadingException e ) {
			throw new MappingException( "Could not create DynamicParameterizedType for type: " + typeName, e );
		}
	}

	public DynamicParameterizedType.ParameterType makeParameterImpl() {
		try {
			final String[] columnNames = new String[ columns.size() ];
			final Long[] columnLengths = new Long[ columns.size() ];

			for ( int i = 0; i < columns.size(); i++ ) {
				final Selectable selectable = columns.get(i);
				if ( selectable instanceof Column ) {
					final Column column = (Column) selectable;
					columnNames[i] = column.getName();
					columnLengths[i] = column.getLength();
				}
			}

			final XProperty xProperty = (XProperty) typeParameters.get( DynamicParameterizedType.XPROPERTY );
			// todo : not sure this works for handling @MapKeyEnumerated
			final Annotation[] annotations = xProperty == null
					? new Annotation[0]
					: xProperty.getAnnotations();

			final ClassLoaderService classLoaderService = getMetadata()
					.getMetadataBuildingOptions()
					.getServiceRegistry()
					.requireService( ClassLoaderService.class );

			return new ParameterTypeImpl(
					classLoaderService.classForTypeName(typeParameters.getProperty(DynamicParameterizedType.RETURNED_CLASS)),
					xProperty instanceof JavaXMember ? ((JavaXMember) xProperty ).getJavaType() : null,
					annotations,
					table.getCatalog(),
					table.getSchema(),
					table.getName(),
					parseBoolean(typeParameters.getProperty(DynamicParameterizedType.IS_PRIMARY_KEY)),
					columnNames,
					columnLengths
			);
		}
		catch ( ClassLoadingException e ) {
			throw new MappingException( "Could not create DynamicParameterizedType for type: " + typeName, e );
		}
	}

	private static final class ParameterTypeImpl implements DynamicParameterizedType.ParameterType {

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
				java.lang.reflect.Type returnedJavaType,
				Annotation[] annotationsMethod,
				String catalog,
				String schema,
				String table,
				boolean primaryKey,
				String[] columns,
				Long[] columnLengths) {
			this.returnedClass = returnedClass;
			this.returnedJavaType = returnedJavaType != null ? returnedJavaType : returnedClass;
			this.annotationsMethod = annotationsMethod;
			this.catalog = catalog;
			this.schema = schema;
			this.table = table;
			this.primaryKey = primaryKey;
			this.columns = columns;
			this.columnLengths = columnLengths;
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

	private class IdGeneratorCreationContext implements CustomIdGeneratorCreationContext {
		private final IdentifierGeneratorFactory identifierGeneratorFactory;
		private final String defaultCatalog;
		private final String defaultSchema;
		private final RootClass rootClass;
		private final Property property;

		public IdGeneratorCreationContext(
				IdentifierGeneratorFactory identifierGeneratorFactory,
				String defaultCatalog,
				String defaultSchema,
				RootClass rootClass,
				Property property) {
			this.identifierGeneratorFactory = identifierGeneratorFactory;
			this.defaultCatalog = defaultCatalog;
			this.defaultSchema = defaultSchema;
			this.rootClass = rootClass;
			this.property = property;
		}

		@Override
		public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
			return identifierGeneratorFactory;
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
		public String getDefaultCatalog() {
			return defaultCatalog;
		}

		@Override
		public String getDefaultSchema() {
			return defaultSchema;
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

		// we could add these if it helps integrate old infrastructure
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
//		@Override
//		public SqlStringGenerationContext getSqlStringGenerationContext() {
//			final Database database = getDatabase();
//			return fromExplicit( database.getJdbcEnvironment(), database, defaultCatalog, defaultSchema );
//		}
	}
}
