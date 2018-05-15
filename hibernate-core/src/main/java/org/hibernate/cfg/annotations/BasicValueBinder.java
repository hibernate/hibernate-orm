/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.MapKeyTemporal;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import org.hibernate.AssertionFailure;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.annotations.MapKeyType;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.Ejb3Column;
import org.hibernate.cfg.Ejb3JoinColumn;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.cfg.PkDrivenByDefaultMapsIdSecondPass;
import org.hibernate.cfg.SetSimpleValueTypeSecondPass;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Table;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

/**
 * @author Emmanuel Bernard
 */
public class BasicValueBinder<T> implements JdbcRecommendedSqlTypeMappingContext {

	// todo (6.0) : In light of how we want to build Types (specifically BasicTypes) moving forward this Class should undergo major changes
	//		see the comments in #setType
	//		but as always the "design" of these classes make it unclear exactly how to change it properly.

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, BasicValueBinder.class.getName() );

	public enum Kind {
		ATTRIBUTE,
		COLLECTION_ID,
		COLLECTION_ELEMENT,
		COLLECTION_INDEX
	}

	private final Kind kind;
	private final MetadataBuildingContext buildingContext;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// BasicType info

	private String explicitBasicTypeName;
	private Map explicitLocalTypeParams;

	private BasicJavaDescriptor<T> javaDescriptor;
	private SqlTypeDescriptor sqlTypeDescriptor;
	private ConverterDescriptor converterDescriptor;
	private boolean isNationalized;
	private boolean isLob;
	private javax.persistence.EnumType enumType;
	private TemporalType temporalPrecision;

	private BasicValue basicValue;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// mapping (database) data

	private Table table;
	private Ejb3Column[] columns;

	// todo (6.0) : investigate what this is used for.  it may not be needed
	private String referencedEntityName;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// pretty sure none of these are needed any longer
	// todo (6.0) : verify, and remove if so
	private String propertyName;
	private String returnedClassName;
	private String persistentClassName;
	private String defaultType = "";
	private Properties typeParameters = new Properties();


	public BasicValueBinder(Kind kind, MetadataBuildingContext buildingContext) {
		assert kind != null;
		assert  buildingContext != null;

		this.kind = kind;
		this.buildingContext = buildingContext;
	}

	@Override
	public EnumType getEnumeratedType() {
		return enumType;
	}

	@Override
	public SqlTypeDescriptor getExplicitSqlTypeDescriptor() {
		return sqlTypeDescriptor;
	}

	@Override
	public boolean isLob() {
		return isLob;
	}

	@Override
	public TemporalType getTemporalType() {
		return temporalPrecision;
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return buildingContext.getPreferredSqlTypeCodeForBoolean();
	}

	@Override
	public boolean isNationalized() {
		return isNationalized;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return buildingContext.getBootstrapContext().getTypeConfiguration();
	}


	public void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName;
	}

	public void setTimestampVersionType(String versionType) {
		// todo (6.0) : change this to instead pass any indicated org.hibernate.tuple.ValueGenerator
		// this.timeStampVersionType = versionType;
		throw new NotYetImplementedFor6Exception(  );
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public void setReturnedClassName(String returnedClassName) {
		this.returnedClassName = returnedClassName;

		if ( defaultType.length() == 0 ) {
			defaultType = returnedClassName;
		}
	}

	public void setTable(Table table) {
		this.table = table;
	}

	public void setColumns(Ejb3Column[] columns) {
		this.columns = columns;
	}


	public void setPersistentClassName(String persistentClassName) {
		this.persistentClassName = persistentClassName;
	}

	//TODO execute it lazily to be order safe

	public void setType(
			XProperty navigableXProperty,
			XClass navigableXClass,
			String declaringClassName,
			ConverterDescriptor converterDescriptor) {
		if ( navigableXClass == null ) {
			// we cannot guess anything
			return;
		}

		if ( columns == null ) {
			throw new AssertionFailure( "SimpleValueBinder.setColumns should be set before SimpleValueBinder.setType" );
		}

		XClass returnedClassOrElement = navigableXClass;
		boolean isArray = false;
		if ( navigableXProperty.isArray() ) {
			returnedClassOrElement = navigableXProperty.getElementClass();
			isArray = true;
		}

		// If we get into this method we know that there is a Java type for the value
		//		and that it is safe to load on the app classloader.
		final Class navigableJavaType = resolveJavaType( returnedClassOrElement, buildingContext );
		if ( navigableJavaType == null ) {
			throw new IllegalStateException( "BasicType requires Java type" );
		}
		Properties typeParameters = this.typeParameters;
		typeParameters.clear();

		final boolean isMap = Map.class.isAssignableFrom(
				buildingContext.getBootstrapContext()
						.getReflectionManager()
						.toClass( navigableXProperty.getType() )
		);

		final boolean key = kind == Kind.COLLECTION_INDEX;

		if ( !key ) {
			isLob = navigableXProperty.isAnnotationPresent( Lob.class );
		}

		if ( getDialect().supportsNationalizedTypes() ) {
			isNationalized = navigableXProperty.isAnnotationPresent( Nationalized.class )
					|| buildingContext.getBuildingOptions().useNationalizedCharacterData();
		}

		applyAttributeConverter( navigableXProperty, converterDescriptor, key );

		Type explicitTypeAnn = null;
		if ( key ) {
			final MapKeyType mapKeyTypeAnn = navigableXProperty.getAnnotation( MapKeyType.class );
			if ( mapKeyTypeAnn != null ) {
				explicitTypeAnn = mapKeyTypeAnn.value();
			}
		}
		else {
			explicitTypeAnn = navigableXProperty.getAnnotation( Type.class );
		}

		if ( explicitTypeAnn != null ) {
			setExplicitType( explicitTypeAnn );
		}
		else {
			switch ( kind ) {
				case COLLECTION_ID: {
					final XClass valueClass = navigableXProperty.getClassOrElementClass();
					basicValue.setJavaClass(
							buildingContext.getBootstrapContext().getReflectionManager().toClass( valueClass )
					);
					break;
				}
				case COLLECTION_INDEX: {
					if ( isMap ) {
						prepareMapKey( navigableXProperty );
					}
					else {
						prepareListIndex( navigableXProperty );
					}
					break;
				}
				case COLLECTION_ELEMENT: {
					prepareCollectionElement( navigableXProperty, navigableXClass );
					break;
				}
				default: {
					assert kind == Kind.ATTRIBUTE;
					prepareBasicAttribute( navigableXProperty, navigableXClass );
				}
			}
		}

		this.typeParameters = typeParameters;
	}

	@SuppressWarnings("unchecked")
	private void prepareMapKey(XProperty mapAttribute) {
		final Class javaType = buildingContext.getBootstrapContext()
				.getReflectionManager()
				.toClass( mapAttribute.getMapKey() );
		basicValue.setJavaClass( javaType );

		javaDescriptor = (BasicJavaDescriptor) buildingContext.getBootstrapContext()
				.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getOrMakeJavaDescriptor( javaType );

		final MapKeyType mapKeyTypeAnn = mapAttribute.getAnnotation( MapKeyType.class );
		if ( mapKeyTypeAnn != null ) {
			// See BasicTypeProducerRegistry registrations in StandardBasicTypes
			//
			// todo (6.0) : need to move those to MetadataBuildingContext
			//		I like the idea too of having TypeDefs fold into that
			throw new NotYetImplementedException( "see comment at throw site" );
		}

		final MapKeyTemporal mapKeyTemporalAnn = mapAttribute.getAnnotation( MapKeyTemporal.class );
		if ( mapKeyTemporalAnn != null ) {
			temporalPrecision = mapKeyTemporalAnn.value();
		}
		else {
			temporalPrecision = null;
		}

		if ( javaType.isEnum() ) {
			final MapKeyEnumerated enumeratedAnn = mapAttribute.getAnnotation( MapKeyEnumerated.class );
			if ( enumeratedAnn == null ) {
				enumType = javax.persistence.EnumType.ORDINAL;
			}
			else {
				enumType = enumeratedAnn.value();
				if ( enumType == null ) {
					throw new IllegalStateException(
							"javax.persistence.EnumType was null on @javax.persistence.MapKeyEnumerated " +
									" associated with attribute " + mapAttribute.getDeclaringClass().getName() +
									'.' + mapAttribute.getName()
					);
				}
			}
		}
		else {
			enumType = null;
		}

		mapKeySupplementalDetails( mapAttribute, buildingContext );
	}

	@SuppressWarnings("unchecked")
	private void prepareListIndex(XProperty listAttribute) {
		javaDescriptor = (BasicJavaDescriptor) buildingContext.getBootstrapContext()
				.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getOrMakeJavaDescriptor( Integer.class );

		basicValue.setSqlType( javaDescriptor.getJdbcRecommendedSqlType( this ) );
	}

	@SuppressWarnings("unchecked")
	private void prepareCollectionElement(XProperty attributeDescriptor, XClass elementJavaType) {
		// todo (6.0) : @SqlType / @SqlTypeDescriptor

		final Class javaClass = buildingContext.getBootstrapContext()
				.getReflectionManager()
				.toClass( elementJavaType );

		basicValue.setJavaClass( javaClass );

		javaDescriptor = (BasicJavaDescriptor) buildingContext.getBootstrapContext()
				.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getOrMakeJavaDescriptor( javaClass );

		final Temporal temporalAnn = attributeDescriptor.getAnnotation( Temporal.class );
		if ( temporalAnn != null ) {
			temporalPrecision = temporalAnn.value();
			if ( temporalPrecision == null ) {
				throw new IllegalStateException(
						"No javax.persistence.TemporalType defined for @javax.persistence.Temporal " +
								"associated with attribute " + attributeDescriptor.getDeclaringClass().getName() +
								'.' + attributeDescriptor.getName()
				);
			}
		}
		else {
			temporalPrecision = null;
		}

		if ( javaClass.isEnum() ) {
			final Enumerated enumeratedAnn = attributeDescriptor.getAnnotation( Enumerated.class );
			if ( enumeratedAnn == null ) {
				enumType = javax.persistence.EnumType.ORDINAL;
			}
			else {
				enumType = enumeratedAnn.value();
				if ( enumType == null ) {
					throw new IllegalStateException(
							"javax.persistence.EnumType was null on @javax.persistence.Enumerated " +
									" associated with attribute " + attributeDescriptor.getDeclaringClass().getName() +
									'.' + attributeDescriptor.getName()
					);
				}
			}
		}
		else {
			enumType = null;
		}

		normalSupplementalDetails( attributeDescriptor, buildingContext );
	}

	@SuppressWarnings("unchecked")
	private void prepareBasicAttribute(XProperty attributeDescriptor, XClass attributeType) {
		final Class javaType = buildingContext.getBootstrapContext()
				.getReflectionManager()
				.toClass( attributeType );

		javaDescriptor = (BasicJavaDescriptor) buildingContext.getBootstrapContext()
				.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getOrMakeJavaDescriptor( javaType );

		final Temporal temporalAnn = attributeDescriptor.getAnnotation( Temporal.class );
		if ( temporalAnn != null ) {
			this.temporalPrecision = temporalAnn.value();
			if ( this.temporalPrecision == null ) {
				throw new IllegalStateException(
						"No javax.persistence.TemporalType defined for @javax.persistence.Temporal " +
								"associated with attribute " + attributeDescriptor.getDeclaringClass().getName() +
								'.' + attributeDescriptor.getName()
				);
			}
		}
		else {
			this.temporalPrecision = null;
		}

		if ( javaType.isEnum() ) {
			final Enumerated enumeratedAnn = attributeDescriptor.getAnnotation( Enumerated.class );
			if ( enumeratedAnn == null ) {
				this.enumType = javax.persistence.EnumType.ORDINAL;
			}
			else {
				this.enumType = enumeratedAnn.value();
				if ( this.enumType == null ) {
					throw new IllegalStateException(
							"javax.persistence.EnumType was null on @javax.persistence.Enumerated " +
									" associated with attribute " + attributeDescriptor.getDeclaringClass().getName() +
									'.' + attributeDescriptor.getName()
					);
				}
			}
		}
		else {
			this.enumType = null;
		}

		normalSupplementalDetails( attributeDescriptor, buildingContext );
	}

	private void mapKeySupplementalDetails(
			XProperty navigableXProperty,
			MetadataBuildingContext buildingContext) {
		final MapKeyEnumerated mapKeyEnumeratedAnn = navigableXProperty.getAnnotation( MapKeyEnumerated.class );
		if ( mapKeyEnumeratedAnn != null ) {
			enumType = mapKeyEnumeratedAnn.value();
		}

		final MapKeyTemporal mapKeyTemporalAnn = navigableXProperty.getAnnotation( MapKeyTemporal.class );
		if ( mapKeyTemporalAnn != null ) {
			temporalPrecision = mapKeyTemporalAnn.value();
		}
	}
	private void normalSupplementalDetails(
			XProperty navigableXProperty,
			MetadataBuildingContext buildingContext) {
		final Enumerated mapKeyEnumeratedAnn = navigableXProperty.getAnnotation( Enumerated.class );
		if ( mapKeyEnumeratedAnn != null ) {
			enumType = mapKeyEnumeratedAnn.value();
		}

		final Temporal mapKeyTemporalAnn = navigableXProperty.getAnnotation( Temporal.class );
		if ( mapKeyTemporalAnn != null ) {
			temporalPrecision = mapKeyTemporalAnn.value();
		}
	}

	private static Class resolveJavaType(XClass returnedClassOrElement, MetadataBuildingContext buildingContext) {
		return buildingContext.getBootstrapContext()
					.getReflectionManager()
					.toClass( returnedClassOrElement );
	}

	private Dialect getDialect() {
		return buildingContext.getBuildingOptions()
				.getServiceRegistry()
				.getService( JdbcServices.class )
				.getJdbcEnvironment()
				.getDialect();
	}

	private void applyAttributeConverter(XProperty property, ConverterDescriptor attributeConverterDescriptor, boolean key) {
		if ( attributeConverterDescriptor == null ) {
			return;
		}

		LOG.debugf( "Starting applyAttributeConverter [%s:%s]", persistentClassName, property.getName() );

		if ( property.isAnnotationPresent( Id.class ) ) {
			LOG.debugf( "Skipping AttributeConverter checks for Id attribute [%s]", property.getName() );
			return;
		}

		if ( property.isAnnotationPresent( Version.class ) ) {
			LOG.debugf( "Skipping AttributeConverter checks for version attribute [%s]", property.getName() );
			return;
		}

		if ( !key && property.isAnnotationPresent( Temporal.class ) ) {
			LOG.debugf( "Skipping AttributeConverter checks for Temporal attribute [%s]", property.getName() );
			return;
		}
		if ( key && property.isAnnotationPresent( MapKeyTemporal.class ) ) {
			LOG.debugf( "Skipping AttributeConverter checks for map-key annotated as MapKeyTemporal [%s]", property.getName() );
			return;
		}

		if ( !key && property.isAnnotationPresent( Enumerated.class ) ) {
			LOG.debugf( "Skipping AttributeConverter checks for Enumerated attribute [%s]", property.getName() );
			return;
		}
		if ( key && property.isAnnotationPresent( MapKeyEnumerated.class ) ) {
			LOG.debugf( "Skipping AttributeConverter checks for map-key annotated as MapKeyEnumerated [%s]", property.getName() );
			return;
		}

		if ( isAssociation() ) {
			LOG.debugf( "Skipping AttributeConverter checks for association attribute [%s]", property.getName() );
			return;
		}

		this.converterDescriptor = attributeConverterDescriptor;
	}

	private boolean isAssociation() {
		// todo : this information is only known to caller(s), need to pass that information in somehow.
		// or, is this enough?
		return referencedEntityName != null;
	}

	public void setExplicitType(String explicitType) {
		this.explicitBasicTypeName = explicitType;
	}

	//FIXME raise an assertion failure  if setResolvedTypeMapping(String) and setResolvedTypeMapping(Type) are use at the same time

	public void setExplicitType(Type typeAnn) {
		setExplicitType( typeAnn.type() );
		this.explicitLocalTypeParams = extractTypeParams( typeAnn.parameters() );
	}

	@SuppressWarnings("unchecked")
	private Map extractTypeParams(Parameter[] parameters) {
		if ( parameters == null || parameters.length == 0 ) {
			return Collections.emptyMap();
		}

		if ( parameters.length == 1 ) {
			return Collections.singletonMap( parameters[0].name(), parameters[0].value() );
		}

		final Map map = new HashMap();
		for ( Parameter parameter: parameters ) {
			map.put( parameter.name(), parameter.value() );
		}

		return map;
	}

	private void validate() {
		//TODO check necessary params
		Ejb3Column.checkPropertyConsistency( columns, propertyName );
	}

	public BasicValue make() {

		validate();
		LOG.debugf( "building SimpleValue for %s", propertyName );
		if ( table == null ) {
			table = columns[0].getTable();
		}
		basicValue = new BasicValue( buildingContext, table );
		if ( isNationalized ) {
			basicValue.makeNationalized();
		}
		if ( isLob ) {
			basicValue.makeLob();
		}
		if ( enumType != null ) {
			basicValue.setEnumType( enumType );
		}
		if ( temporalPrecision != null ) {
			basicValue.setTemporalPrecision( temporalPrecision );
		}
		// todo (6.0) : explicit SqlTypeDescriptor / JDBC type-code
		// todo (6.0) : explicit mutability / immutable
		// todo (6.0) : explicit Comparator

		linkWithValue();

		boolean isInSecondPass = buildingContext.getMetadataCollector().isInSecondPass();
		if ( !isInSecondPass ) {
			//Defer this to the second pass
			buildingContext.getMetadataCollector().addSecondPass( new SetSimpleValueTypeSecondPass( this ) );
		}
		else {
			//We are already in second pass
			fillSimpleValue();
		}
		return basicValue;
	}

	public void linkWithValue() {
		if ( columns[0].isNameDeferred() && !buildingContext.getMetadataCollector().isInSecondPass() && referencedEntityName != null ) {
			buildingContext.getMetadataCollector().addSecondPass(
					new PkDrivenByDefaultMapsIdSecondPass(
							referencedEntityName, (Ejb3JoinColumn[]) columns, basicValue
					)
			);
		}
		else {
			for ( Ejb3Column column : columns ) {
				column.linkWithValue( basicValue );
			}
		}
	}

	public void fillSimpleValue() {
		LOG.debugf( "Starting fillSimpleValue for %s", propertyName );
		basicValue.setExplicitTypeName( explicitBasicTypeName );
		basicValue.setExplicitTypeParams( explicitLocalTypeParams );
		basicValue.setEnumType( enumType );
		basicValue.setTemporalPrecision( temporalPrecision );
		if ( isLob ) {
			basicValue.makeLob();
		}
		if ( isNationalized ) {
			basicValue.makeNationalized();
		}
		basicValue.setSqlType( sqlTypeDescriptor );
		basicValue.setJavaTypeDescriptor( javaDescriptor );
		basicValue.setTypeUsingReflection( persistentClassName, propertyName );
		basicValue.setJpaAttributeConverterDescriptor( converterDescriptor );
	}
}
