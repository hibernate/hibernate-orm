/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import jakarta.persistence.AccessType;
import org.hibernate.AnnotationException;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SecondaryRow;
import org.hibernate.boot.internal.LimitedCollectionClassification;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAssociationOverrideImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributeOverrideImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbBasicMapping;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCachingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCascadeTypeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCheckConstraintImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionUserTypeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConfigurationParameterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConvertImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCustomSqlImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbDiscriminatorColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbDiscriminatorFormulaImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbElementCollectionImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntity;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenerContainerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityOrMappedSuperclass;
import org.hibernate.boot.jaxb.mapping.spi.JaxbFilterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbGeneratedValueImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbGenericIdGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIdClassImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIndexImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJoinColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbLifecycleCallback;
import org.hibernate.boot.jaxb.mapping.spi.JaxbLifecycleCallbackContainer;
import org.hibernate.boot.jaxb.mapping.spi.JaxbLobImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNationalizedImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNaturalId;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNotFoundCapable;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPluralAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPrimaryKeyJoinColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSchemaAware;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSecondaryTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSequenceGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTableGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUniqueConstraintImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUserTypeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUuidGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbVersionImpl;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.XmlAnnotations;
import org.hibernate.boot.models.annotations.internal.*;
import org.hibernate.boot.models.annotations.spi.CustomSqlDetails;
import org.hibernate.boot.models.annotations.spi.DatabaseObjectDetails;
import org.hibernate.boot.models.JpaEventListenerStyle;
import org.hibernate.boot.models.spi.JpaEventListener;
import org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing;
import org.hibernate.boot.models.xml.internal.db.ForeignKeyProcessing;
import org.hibernate.boot.models.xml.internal.db.JoinColumnProcessing;
import org.hibernate.boot.models.xml.internal.db.TableProcessing;
import org.hibernate.boot.models.xml.spi.XmlDocument;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.MutableAnnotationTarget;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Index;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static org.hibernate.boot.models.HibernateAnnotations.FILTER;
import static org.hibernate.boot.models.HibernateAnnotations.FILTER_JOIN_TABLE;
import static org.hibernate.boot.models.HibernateAnnotations.PARAMETER;
import static org.hibernate.boot.models.HibernateAnnotations.SECONDARY_ROW;
import static org.hibernate.boot.models.HibernateAnnotations.SECONDARY_ROWS;
import static org.hibernate.boot.models.HibernateAnnotations.SQL_RESTRICTION;
import static org.hibernate.boot.models.JpaAnnotations.ASSOCIATION_OVERRIDE;
import static org.hibernate.boot.models.JpaAnnotations.ASSOCIATION_OVERRIDES;
import static org.hibernate.boot.models.JpaAnnotations.ATTRIBUTE_OVERRIDE;
import static org.hibernate.boot.models.JpaAnnotations.ATTRIBUTE_OVERRIDES;
import static org.hibernate.boot.models.JpaAnnotations.CHECK_CONSTRAINT;
import static org.hibernate.boot.models.JpaAnnotations.COLUMN;
import static org.hibernate.boot.models.JpaAnnotations.CONVERT;
import static org.hibernate.boot.models.JpaAnnotations.EXCLUDE_DEFAULT_LISTENERS;
import static org.hibernate.boot.models.JpaAnnotations.EXCLUDE_SUPERCLASS_LISTENERS;
import static org.hibernate.boot.models.JpaAnnotations.INDEX;
import static org.hibernate.boot.models.JpaAnnotations.SECONDARY_TABLE;
import static org.hibernate.boot.models.JpaAnnotations.UNIQUE_CONSTRAINT;
import static org.hibernate.boot.models.xml.internal.UserTypeCasesMapKey.MAP_KEY_USER_TYPE_CASES;
import static org.hibernate.boot.models.xml.internal.UserTypeCasesStandard.STANDARD_USER_TYPE_CASES;
import static org.hibernate.internal.util.NullnessHelper.coalesce;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.unqualify;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;

/**
 * Helper for creating annotation from equivalent JAXB
 *
 * @author Steve Ebersole
 */
public class XmlAnnotationHelper {
	/**
	 * Handle creating {@linkplain Entity @Entity} from an {@code <entity/>} element.
	 * Used in both complete and override modes.
	 */
	public static void applyEntity(
			JaxbEntity jaxbEntity,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		final EntityJpaAnnotation entityAnn = (EntityJpaAnnotation) classDetails.applyAnnotationUsage(
				JpaAnnotations.ENTITY,
				xmlDocumentContext.getModelBuildingContext()
		);
		if ( isNotEmpty( jaxbEntity.getName() ) ) {
			entityAnn.name( jaxbEntity.getName() );
		}
	}

	public static void applyColumn(
			JaxbColumnImpl jaxbColumn,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbColumn == null ) {
			return;
		}

		final ColumnJpaAnnotation columnAnnotationUsage = (ColumnJpaAnnotation) memberDetails.applyAnnotationUsage(
				COLUMN,
				xmlDocumentContext.getModelBuildingContext()
		);
		columnAnnotationUsage.apply( jaxbColumn, xmlDocumentContext );
	}

	private static ColumnJpaAnnotation createColumnAnnotation(
			JaxbColumnImpl jaxbColumn,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		final ColumnJpaAnnotation usage = COLUMN.createUsage( xmlDocumentContext.getModelBuildingContext() );
		usage.apply( jaxbColumn, xmlDocumentContext );
		return usage;
	}

	public static void applyColumnTransformation(
			JaxbColumnImpl jaxbColumn,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( isEmpty( jaxbColumn.getRead() )
				&& isEmpty( jaxbColumn.getWrite() ) ) {
			return;
		}

		final ColumnTransformerAnnotation annotationUsage = (ColumnTransformerAnnotation) memberDetails.applyAnnotationUsage(
				HibernateAnnotations.COLUMN_TRANSFORMER,
				xmlDocumentContext.getModelBuildingContext()
		);

		annotationUsage.forColumn( jaxbColumn.getName() );

		if ( isNotEmpty( jaxbColumn.getRead() ) ) {
			annotationUsage.read( jaxbColumn.getRead() );
		}
		if ( isNotEmpty( jaxbColumn.getWrite() ) ) {
			annotationUsage.write( jaxbColumn.getWrite() );
		}
	}

	public static void applyUserType(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyUserType( jaxbType, memberDetails, STANDARD_USER_TYPE_CASES, xmlDocumentContext );
	}

	public static void applyMapKeyUserType(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyUserType( jaxbType, memberDetails, MAP_KEY_USER_TYPE_CASES, xmlDocumentContext );
	}

	public static void applyUserType(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			UserTypeCases cases,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbType == null || isEmpty( jaxbType.getValue() ) ) {
			cases.handleNone( jaxbType, memberDetails, xmlDocumentContext );
			return;
		}

		final SimpleTypeInterpretation interpretation = SimpleTypeInterpretation.interpret( jaxbType.getValue() );
		if ( interpretation == null ) {
			cases.handleGeneral( jaxbType, memberDetails, xmlDocumentContext );
			return;
		}

		switch ( interpretation ) {
			case BOOLEAN -> cases.handleBoolean( jaxbType, memberDetails, xmlDocumentContext );
			case BYTE -> cases.handleByte( jaxbType, memberDetails, xmlDocumentContext );
			case SHORT -> cases.handleShort( jaxbType, memberDetails, xmlDocumentContext );
			case INTEGER -> cases.handleInteger( jaxbType, memberDetails, xmlDocumentContext );
			case LONG -> cases.handleLong( jaxbType, memberDetails, xmlDocumentContext );
			case DOUBLE -> cases.handleDouble( jaxbType, memberDetails, xmlDocumentContext );
			case FLOAT -> cases.handleFloat( jaxbType, memberDetails, xmlDocumentContext );
			case BIG_INTEGER -> cases.handleBigInteger( jaxbType, memberDetails, xmlDocumentContext );
			case BIG_DECIMAL -> cases.handleBigDecimal( jaxbType, memberDetails, xmlDocumentContext );
			case CHARACTER -> cases.handleCharacter( jaxbType, memberDetails, xmlDocumentContext );
			case STRING -> cases.handleString( jaxbType, memberDetails, xmlDocumentContext );
			case INSTANT -> cases.handleInstant( jaxbType, memberDetails, xmlDocumentContext );
			case DURATION -> cases.handleDuration( jaxbType, memberDetails, xmlDocumentContext );
			case YEAR -> cases.handleYear( jaxbType, memberDetails, xmlDocumentContext );
			case LOCAL_DATE_TIME -> cases.handleLocalDateTime( jaxbType, memberDetails, xmlDocumentContext );
			case LOCAL_DATE -> cases.handleLocalDate( jaxbType, memberDetails, xmlDocumentContext );
			case LOCAL_TIME -> cases.handleLocalTime( jaxbType, memberDetails, xmlDocumentContext );
			case OFFSET_DATE_TIME -> cases.handleOffsetDateTime( jaxbType, memberDetails, xmlDocumentContext );
			case OFFSET_TIME -> cases.handleOffsetTime( jaxbType, memberDetails, xmlDocumentContext );
			case ZONED_DATE_TIME -> cases.handleZonedDateTime( jaxbType, memberDetails, xmlDocumentContext );
			case ZONE_ID -> cases.handleZoneId( jaxbType, memberDetails, xmlDocumentContext );
			case ZONE_OFFSET -> cases.handleZoneOffset( jaxbType, memberDetails, xmlDocumentContext );
			case UUID -> cases.handleUuid( jaxbType, memberDetails, xmlDocumentContext );
			case URL -> cases.handleUrl( jaxbType, memberDetails, xmlDocumentContext );
			case INET_ADDRESS -> cases.handleInetAddress( jaxbType, memberDetails, xmlDocumentContext );
			case CURRENCY -> cases.handleCurrency( jaxbType, memberDetails, xmlDocumentContext );
			case LOCALE -> cases.handleLocale( jaxbType, memberDetails, xmlDocumentContext );
			case CLASS -> cases.handleClass( jaxbType, memberDetails, xmlDocumentContext );
			case BLOB -> cases.handleBlob( jaxbType, memberDetails, xmlDocumentContext );
			case CLOB -> cases.handleClob( jaxbType, memberDetails, xmlDocumentContext );
			case NCLOB -> cases.handleNClob( jaxbType, memberDetails, xmlDocumentContext );
			case JDBC_TIMESTAMP -> cases.handleTimestamp( jaxbType, memberDetails, xmlDocumentContext );
			case JDBC_DATE -> cases.handleDate( jaxbType, memberDetails, xmlDocumentContext );
			case JDBC_TIME -> cases.handleTime( jaxbType, memberDetails, xmlDocumentContext );
			case CALENDAR -> cases.handleCalendar( jaxbType, memberDetails, xmlDocumentContext );
			case TIME_ZONE -> cases.handleTimeZone( jaxbType, memberDetails, xmlDocumentContext );
		}
	}

	private static final Parameter[] NO_PARAMETERS = new Parameter[0];

	public static Parameter[] collectParameters(
			List<JaxbConfigurationParameterImpl> jaxbParameters,
			XmlDocumentContext xmlDocumentContext) {
		return collectParameters( jaxbParameters, xmlDocumentContext.getModelBuildingContext() );
	}

	public static Parameter[] collectParameters(
			List<JaxbConfigurationParameterImpl> jaxbParameters,
			ModelsContext sourceModelContext) {
		if ( isEmpty( jaxbParameters ) ) {
			return NO_PARAMETERS;
		}

		final Parameter[] parameters = new Parameter[jaxbParameters.size()];
		for ( int i = 0; i < jaxbParameters.size(); i++ ) {
			final JaxbConfigurationParameterImpl jaxbParameter = jaxbParameters.get( i );
			final ParameterAnnotation usage = PARAMETER.createUsage( sourceModelContext );
			parameters[i] = usage;
			usage.name( jaxbParameter.getName() );
			usage.value( jaxbParameter.getValue() );
		}
		return parameters;
	}

	public static void applyCollectionUserType(
			JaxbCollectionUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbType == null ) {
			return;
		}

		final CollectionTypeAnnotation typeAnn = (CollectionTypeAnnotation) memberDetails.applyAnnotationUsage(
				HibernateAnnotations.COLLECTION_TYPE,
				xmlDocumentContext.getModelBuildingContext()
		);
		final ClassDetails userTypeImpl = resolveJavaType( jaxbType.getType(), xmlDocumentContext );
		typeAnn.type( userTypeImpl.toJavaClass() );
		typeAnn.parameters( collectParameters( jaxbType.getParameters(), xmlDocumentContext ) );
	}

	public static void applyCollectionId(
			JaxbCollectionIdImpl jaxbCollectionId,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbCollectionId == null ) {
			return;
		}

		final CollectionIdAnnotation collectionIdAnn = (CollectionIdAnnotation) memberDetails.applyAnnotationUsage(
				HibernateAnnotations.COLLECTION_ID,
				xmlDocumentContext.getModelBuildingContext()
		);

		final JaxbColumnImpl jaxbColumn = jaxbCollectionId.getColumn();
		if ( jaxbColumn != null ) {
			collectionIdAnn.column( createColumnAnnotation(
					jaxbColumn,
					memberDetails,
					xmlDocumentContext
			) );
		}

		final JaxbGeneratedValueImpl generator = jaxbCollectionId.getGenerator();
		if ( generator != null && isNotEmpty( generator.getGenerator() ) ) {
			collectionIdAnn.generator( generator.getGenerator() );
		}

		if ( StringHelper.isNotEmpty( jaxbCollectionId.getTarget() ) ) {
			final SimpleTypeInterpretation simpleTypeInterpretation = SimpleTypeInterpretation.interpret(
					jaxbCollectionId.getTarget()
			);
			assert simpleTypeInterpretation != null;

			final CollectionIdJavaClassAnnotation annotationUsage = (CollectionIdJavaClassAnnotation) memberDetails.applyAnnotationUsage(
					HibernateAnnotations.COLLECTION_ID_JAVA_CLASS,
					xmlDocumentContext.getModelBuildingContext()
			);
			annotationUsage.idType( simpleTypeInterpretation.getJavaType() );
		}
		else {
			// this will likely lead to an error later.
			// should we throw an exception here?
		}
	}

	public static void applyCascading(
			JaxbCascadeTypeImpl jaxbCascadeType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbCascadeType == null ) {
			return;
		}

		// We always use Hibernate specific `org.hibernate.annotations.CascadeType`
		// since it is a superset of `jakarta.persistence.CascadeType`
		final List<CascadeType> cascadeTypes = new ArrayList<>( xmlDocumentContext.getEffectiveDefaults().getDefaultCascadeTypes() );
		if ( jaxbCascadeType.getCascadeAll() != null ) {
			cascadeTypes.add( CascadeType.ALL );
		}
		if ( jaxbCascadeType.getCascadePersist() != null ) {
			cascadeTypes.add( CascadeType.PERSIST );
		}
		if ( jaxbCascadeType.getCascadeMerge() != null ) {
			cascadeTypes.add( CascadeType.MERGE );
		}
		if ( jaxbCascadeType.getCascadeRemove() != null ) {
			cascadeTypes.add( CascadeType.REMOVE );
		}
		if ( jaxbCascadeType.getCascadeRefresh() != null ) {
			cascadeTypes.add( CascadeType.REFRESH );
		}
		if ( jaxbCascadeType.getCascadeDetach() != null ) {
			cascadeTypes.add( CascadeType.DETACH );
		}
		if ( jaxbCascadeType.getCascadeReplicate() != null ) {
			//noinspection deprecation
			cascadeTypes.add( CascadeType.REPLICATE );
		}
		if ( jaxbCascadeType.getCascadeLock() != null ) {
			cascadeTypes.add( CascadeType.LOCK );
		}

		if ( !cascadeTypes.isEmpty() ) {
			final CascadeAnnotation cascadeAnn = (CascadeAnnotation) memberDetails.applyAnnotationUsage(
					HibernateAnnotations.CASCADE,
					xmlDocumentContext.getModelBuildingContext()
			);
			cascadeAnn.value( cascadeTypes.toArray(CascadeType[]::new) );
		}
	}

	public static void applyTargetClass(
			String name,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final TargetXmlAnnotation targetAnn = (TargetXmlAnnotation) memberDetails.applyAnnotationUsage(
				XmlAnnotations.TARGET,
				xmlDocumentContext.getModelBuildingContext()
		);
		targetAnn.value( name );
	}

	@SuppressWarnings("deprecation")
	public static void applyTemporal(
			TemporalType temporalType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( temporalType == null ) {
			return;
		}

		final TemporalJpaAnnotation temporalAnn = (TemporalJpaAnnotation) memberDetails.applyAnnotationUsage(
				JpaAnnotations.TEMPORAL,
				xmlDocumentContext.getModelBuildingContext()
		);
		temporalAnn.value( temporalType );
	}

	public static void applyLob(JaxbLobImpl jaxbLob, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext) {
		if ( jaxbLob == null ) {
			return;
		}

		memberDetails.applyAnnotationUsage( JpaAnnotations.LOB, xmlDocumentContext.getModelBuildingContext() );
	}

	public static void applyEnumerated(EnumType enumType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext) {
		if ( enumType == null ) {
			return;
		}

		final EnumeratedJpaAnnotation annotationUsage = (EnumeratedJpaAnnotation) memberDetails.applyAnnotationUsage(
				JpaAnnotations.ENUMERATED,
				xmlDocumentContext.getModelBuildingContext()
		);

		annotationUsage.value( enumType );
	}

	public static void applyNationalized(
			JaxbNationalizedImpl jaxbNationalized,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbNationalized == null ) {
			return;
		}

		memberDetails.applyAnnotationUsage( HibernateAnnotations.NATIONALIZED, xmlDocumentContext.getModelBuildingContext() );
	}

	public static void applyGeneratedValue(
			JaxbGeneratedValueImpl jaxbGeneratedValue,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbGeneratedValue == null ) {
			return;
		}

		final GeneratedValueJpaAnnotation generatedValueAnn = (GeneratedValueJpaAnnotation) memberDetails.applyAnnotationUsage(
				JpaAnnotations.GENERATED_VALUE,
				xmlDocumentContext.getModelBuildingContext()
		);

		if ( jaxbGeneratedValue.getStrategy() != null ) {
			generatedValueAnn.strategy( jaxbGeneratedValue.getStrategy() );
		}

		if ( isNotEmpty( jaxbGeneratedValue.getGenerator() ) ) {
			generatedValueAnn.generator( jaxbGeneratedValue.getGenerator() );
		}
	}

	public static void applySequenceGenerator(
			JaxbSequenceGeneratorImpl jaxbGenerator,
			MutableAnnotationTarget generatorTarget,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbGenerator == null ) {
			return;
		}

		final SequenceGeneratorJpaAnnotation sequenceAnn = (SequenceGeneratorJpaAnnotation) generatorTarget.applyAnnotationUsage(
				JpaAnnotations.SEQUENCE_GENERATOR,
				xmlDocumentContext.getModelBuildingContext()
		);

		if ( isNotEmpty( jaxbGenerator.getName() ) ) {
			sequenceAnn.name( jaxbGenerator.getName() );
		}

		if ( jaxbGenerator.getSequenceName() != null ) {
			sequenceAnn.sequenceName( jaxbGenerator.getSequenceName() );
		}

		if ( isNotEmpty( jaxbGenerator.getCatalog() ) ) {
			sequenceAnn.catalog( jaxbGenerator.getCatalog() );
		}

		if ( isNotEmpty( jaxbGenerator.getSchema() ) ) {
			sequenceAnn.schema( jaxbGenerator.getSchema() );
		}

		if ( jaxbGenerator.getInitialValue() != null ) {
			sequenceAnn.initialValue( jaxbGenerator.getInitialValue() );
		}

		if ( jaxbGenerator.getAllocationSize() != null ) {
			sequenceAnn.allocationSize( jaxbGenerator.getAllocationSize() );
		}

		if ( isNotEmpty( jaxbGenerator.getOptions() ) ) {
			sequenceAnn.options( jaxbGenerator.getOptions() );
		}
	}

	public static void applyTableGenerator(
			JaxbTableGeneratorImpl jaxbGenerator,
			MutableAnnotationTarget generatorTarget,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbGenerator == null ) {
			return;
		}

		final TableGeneratorJpaAnnotation tableAnn = (TableGeneratorJpaAnnotation) generatorTarget.applyAnnotationUsage( JpaAnnotations.TABLE_GENERATOR, xmlDocumentContext.getModelBuildingContext() );
		tableAnn.apply( jaxbGenerator, xmlDocumentContext );
	}

	public static void applyUuidGenerator(
			JaxbUuidGeneratorImpl jaxbGenerator,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbGenerator == null ) {
			return;
		}

		final UuidGeneratorAnnotation uuidGenAnn = (UuidGeneratorAnnotation) memberDetails.applyAnnotationUsage(
				HibernateAnnotations.UUID_GENERATOR,
				xmlDocumentContext.getModelBuildingContext()
		);

		uuidGenAnn.style( jaxbGenerator.getStyle() );
	}

	public static void applyGenericGenerator(
			JaxbGenericIdGeneratorImpl jaxbGenerator,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbGenerator == null ) {
			return;
		}

		final GenericGeneratorAnnotation generatorAnn = (GenericGeneratorAnnotation) memberDetails.applyAnnotationUsage(
				HibernateAnnotations.GENERIC_GENERATOR,
				xmlDocumentContext.getModelBuildingContext()
		);
		generatorAnn.name( "" );
		generatorAnn.strategy( jaxbGenerator.getClazz() );

		final List<JaxbConfigurationParameterImpl> jaxbParameters = jaxbGenerator.getParameters();
		if ( isEmpty( jaxbParameters ) ) {
			generatorAnn.parameters( NO_PARAMETERS );
		}
		else {
			final Parameter[] parameters = new Parameter[jaxbParameters.size()];
			for ( int i = 0; i < jaxbParameters.size(); i++ ) {
				final ParameterAnnotation parameterUsage = PARAMETER.createUsage( xmlDocumentContext.getModelBuildingContext() );
				parameterUsage.name( jaxbParameters.get(i).getName() );
				parameterUsage.value( jaxbParameters.get(i).getValue() );
				parameters[i] = parameterUsage;
			}
			generatorAnn.parameters( parameters );
		}
	}

	public static void applyAttributeOverrides(
			JaxbPluralAttribute pluralAttribute,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final List<JaxbAttributeOverrideImpl> jaxbMapKeyOverrides = pluralAttribute.getMapKeyAttributeOverrides();
		final List<JaxbAttributeOverrideImpl> jaxbElementOverrides =
				pluralAttribute instanceof JaxbElementCollectionImpl elementCollection
						? elementCollection.getAttributeOverrides()
						: emptyList();

		if ( isEmpty( jaxbMapKeyOverrides ) && isEmpty( jaxbElementOverrides ) ) {
			return;
		}

		final AttributeOverridesJpaAnnotation overridesUsage = (AttributeOverridesJpaAnnotation) memberDetails.applyAnnotationUsage(
				ATTRIBUTE_OVERRIDES,
				xmlDocumentContext.getModelBuildingContext()
		);

		final int numberOfOverrides = jaxbMapKeyOverrides.size() + jaxbElementOverrides.size();
		final AttributeOverride[] overrideUsages = new AttributeOverride[numberOfOverrides];
		overridesUsage.value( overrideUsages );

		// We need to handle overrides for maps specially...
		if ( memberDetails.getMapKeyType() != null ) {
			int position = 0;
			for ( JaxbAttributeOverrideImpl jaxbOverride : jaxbMapKeyOverrides ) {
				overrideUsages[position++] = createAttributeOverrideUsage(
						jaxbOverride,
						"key",
						memberDetails,
						xmlDocumentContext
				);
			}
			for ( JaxbAttributeOverrideImpl jaxbOverride : jaxbElementOverrides ) {
				overrideUsages[position++] = createAttributeOverrideUsage(
						jaxbOverride,
						"value",
						memberDetails,
						xmlDocumentContext
				);
			}
		}
		else {
			assert isEmpty( jaxbMapKeyOverrides );
			for ( int i = 0; i < jaxbElementOverrides.size(); i++ ) {
				overrideUsages[i] = createAttributeOverrideUsage(
						jaxbElementOverrides.get(i),
						"value",
						memberDetails,
						xmlDocumentContext
				);
			}
		}
	}

	private static AttributeOverrideJpaAnnotation createAttributeOverrideUsage(
			JaxbAttributeOverrideImpl jaxbOverride,
			String namePrefix,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		final ModelsContext modelBuildingContext = xmlDocumentContext.getModelBuildingContext();

		final AttributeOverrideJpaAnnotation overrideUsage = ATTRIBUTE_OVERRIDE.createUsage( modelBuildingContext );

		final String name = StringHelper.qualifyConditionally( namePrefix, jaxbOverride.getName() );
		overrideUsage.name( name );

		final ColumnJpaAnnotation columnAnn = COLUMN.createUsage( modelBuildingContext );
		overrideUsage.column( columnAnn );
		columnAnn.apply( jaxbOverride.getColumn(), xmlDocumentContext );
		return overrideUsage;
	}

	public static void applyAttributeOverrides(
			List<JaxbAttributeOverrideImpl> jaxbOverrides,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyAttributeOverrides( jaxbOverrides, memberDetails, null, xmlDocumentContext );
	}

	public static void applyAttributeOverrides(
			List<JaxbAttributeOverrideImpl> jaxbOverrides,
			MutableAnnotationTarget target,
			String namePrefix,
			XmlDocumentContext xmlDocumentContext) {
		if ( isEmpty( jaxbOverrides ) ) {
			return;
		}

		final AttributeOverridesJpaAnnotation overridesUsage = (AttributeOverridesJpaAnnotation) target.replaceAnnotationUsage(
				ATTRIBUTE_OVERRIDE,
				ATTRIBUTE_OVERRIDES,
				xmlDocumentContext.getModelBuildingContext()
		);

		final AttributeOverride[] overrideUsages = new AttributeOverride[jaxbOverrides.size()];
		overridesUsage.value( overrideUsages );

		for ( int i = 0; i < jaxbOverrides.size(); i++ ) {
			overrideUsages[i] = createAttributeOverrideUsage(
					jaxbOverrides.get( i ),
					namePrefix,
					target,
					xmlDocumentContext
			);
		}
	}

	public static void applyAssociationOverrides(
			List<JaxbAssociationOverrideImpl> jaxbOverrides,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		if ( isEmpty( jaxbOverrides ) ) {
			return;
		}

		final AssociationOverridesJpaAnnotation overridesUsage = (AssociationOverridesJpaAnnotation) target.replaceAnnotationUsage(
				ASSOCIATION_OVERRIDE,
				ASSOCIATION_OVERRIDES,
				xmlDocumentContext.getModelBuildingContext()
		);

		final AssociationOverride[] overrideUsages = new AssociationOverride[jaxbOverrides.size()];
		overridesUsage.value( overrideUsages );

		for ( int i = 0; i < jaxbOverrides.size(); i++ ) {
			final AssociationOverrideJpaAnnotation override = ASSOCIATION_OVERRIDE.createUsage( xmlDocumentContext.getModelBuildingContext() );
			overrideUsages[i] = override;
			transferAssociationOverride( jaxbOverrides.get(i), override, target, xmlDocumentContext );
		}
	}

	private static void transferAssociationOverride(
			JaxbAssociationOverrideImpl jaxbOverride,
			AssociationOverrideJpaAnnotation overrideUsage,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		overrideUsage.name( jaxbOverride.getName() );

		final List<JaxbJoinColumnImpl> joinColumns = jaxbOverride.getJoinColumns();
		if ( CollectionHelper.isNotEmpty( joinColumns ) ) {
			overrideUsage.joinColumns( JoinColumnProcessing.transformJoinColumnList(
					joinColumns,
					xmlDocumentContext
			) );
		}
		if ( jaxbOverride.getJoinTable() != null ) {
			overrideUsage.joinTable( TableProcessing.transformJoinTable(
					jaxbOverride.getJoinTable(),
					target,
					xmlDocumentContext
			) );
		}
		if ( jaxbOverride.getForeignKeys() != null ) {
			overrideUsage.foreignKey( ForeignKeyProcessing.createNestedForeignKeyAnnotation(
					jaxbOverride.getForeignKeys(),
					xmlDocumentContext
			) );
		}

	}

	public static void applyConvert(
			JaxbConvertImpl jaxbConvert,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbConvert == null ) {
			return;
		}

		final ConvertJpaAnnotation annotation = (ConvertJpaAnnotation) memberDetails.replaceAnnotationUsage(
				CONVERT,
				xmlDocumentContext.getModelBuildingContext()
		);
		transferConvertDetails( jaxbConvert, annotation, null, xmlDocumentContext );
	}

	public static void applyConverts(
			List<JaxbConvertImpl> jaxbConverts,
			String namePrefix,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( isEmpty( jaxbConverts ) ) {
			return;
		}

		final ConvertsJpaAnnotation convertsUsage = (ConvertsJpaAnnotation) memberDetails.replaceAnnotationUsage(
				CONVERT,
				JpaAnnotations.CONVERTS,
				xmlDocumentContext.getModelBuildingContext()
		);
		final Convert[] convertUsages = new Convert[jaxbConverts.size()];
		convertsUsage.value( convertUsages );

		for ( int i = 0; i < jaxbConverts.size(); i++ ) {
			convertUsages[i] = transformConvert(
					jaxbConverts.get( i ),
					namePrefix,
					xmlDocumentContext
			);
		}
	}

	public static Convert transformConvert(
			JaxbConvertImpl jaxbConvert,
			String namePrefix,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbConvert == null ) {
			return null;
		}

		final ConvertJpaAnnotation convert = CONVERT.createUsage( xmlDocumentContext.getModelBuildingContext() );

		transferConvertDetails( jaxbConvert, convert, namePrefix, xmlDocumentContext );

		return convert;
	}

	private static void transferConvertDetails(
			JaxbConvertImpl jaxbConvert,
			ConvertJpaAnnotation convert,
			String namePrefix,
			XmlDocumentContext xmlDocumentContext) {
		if ( isNotEmpty( jaxbConvert.getConverter() ) ) {
			convert.converter( xmlDocumentContext.resolveJavaType( jaxbConvert.getConverter() ).toJavaClass() );
		}
		if ( isNotEmpty( jaxbConvert.getAttributeName() ) ) {
			convert.attributeName( prefixIfNotAlready( jaxbConvert.getAttributeName(), namePrefix ) );
		}
		if ( jaxbConvert.isDisableConversion() != null ) {
			convert.disableConversion( jaxbConvert.isDisableConversion() );
		}
	}

	public static void applyTable(
			JaxbTableImpl jaxbTable,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbTable == null ) {
			final XmlDocument.Defaults defaults = xmlDocumentContext.getXmlDocument().getDefaults();
			final String catalog = defaults.getCatalog();
			final String schema = defaults.getSchema();
			if ( isNotEmpty( catalog ) || isNotEmpty( schema ) ) {
				final TableJpaAnnotation tableAnn = (TableJpaAnnotation) target.applyAnnotationUsage(
						JpaAnnotations.TABLE,
						xmlDocumentContext.getModelBuildingContext()
				);
				if ( isNotEmpty( catalog ) ) {
					tableAnn.catalog( catalog );

				}
				if ( isNotEmpty( schema ) ) {
					tableAnn.schema( schema );
				}
			}
		}
		else {
			final TableJpaAnnotation tableAnn = (TableJpaAnnotation) target.applyAnnotationUsage(
					JpaAnnotations.TABLE,
					xmlDocumentContext.getModelBuildingContext()
			);
			tableAnn.apply( jaxbTable, xmlDocumentContext );
		}
	}

	public static void applyOptionalString(String value, Consumer<String> target) {
		if ( isNotEmpty( value ) ) {
			target.accept( value );
		}
	}

	public static void applyNaturalIdCache(
			JaxbNaturalId jaxbNaturalId,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbNaturalId == null || jaxbNaturalId.getCaching() == null ) {
			return;
		}

		final NaturalIdCacheAnnotation naturalIdCacheUsage = (NaturalIdCacheAnnotation) classDetails.applyAnnotationUsage(
				HibernateAnnotations.NATURAL_ID_CACHE,
				xmlDocumentContext.getModelBuildingContext()
		);

		final JaxbCachingImpl jaxbCaching = jaxbNaturalId.getCaching();
		if ( isNotEmpty( jaxbCaching.getRegion() ) ) {
			naturalIdCacheUsage.region( jaxbCaching.getRegion() );
		}
	}

	static void applyInheritance(
			JaxbEntity jaxbEntity,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbEntity.getInheritance() == null ) {
			return;
		}

		final InheritanceJpaAnnotation inheritanceUsage = (InheritanceJpaAnnotation) classDetails.applyAnnotationUsage(
				JpaAnnotations.INHERITANCE,
				xmlDocumentContext.getModelBuildingContext()
		);
		if ( jaxbEntity.getInheritance().getStrategy() != null ) {
			inheritanceUsage.strategy( jaxbEntity.getInheritance().getStrategy() );
		}
	}

	public static ClassDetails resolveJavaType(String value, XmlDocumentContext xmlDocumentContext) {
		return resolveJavaType(
				xmlDocumentContext.getXmlDocument().getDefaults().getPackage(),
				value,
				xmlDocumentContext.getModelBuildingContext().getClassDetailsRegistry()
		);
	}

	/**
	 * Used in cases where we might need to account for legacy "simple type naming" or "named basic types" such as
	 * {@code type="string"}.
	 */
	public static ClassDetails resolveSimpleJavaType(String value, ClassDetailsRegistry classDetailsRegistry) {
		final SimpleTypeInterpretation simpleInterpretation = SimpleTypeInterpretation.interpret( value );
		if ( simpleInterpretation != null ) {
			return classDetailsRegistry.resolveClassDetails( simpleInterpretation.getJavaType().getName() );
		}
		return resolveJavaType( null, value, classDetailsRegistry );
	}

	public static ClassDetails resolveJavaType(String value, ClassDetailsRegistry classDetailsRegistry) {
		return resolveJavaType( null, value, classDetailsRegistry );
	}

	public static ClassDetails resolveJavaType(String packageName, String name, ClassDetailsRegistry classDetailsRegistry) {
		if ( isEmpty( name ) ) {
			name = Object.class.getName();
		}
		else if ( byte.class.getName().equals( name )
				|| boolean.class.getName().equals( name )
				|| short.class.getName().equals( name )
				|| int.class.getName().equals( name )
				|| long.class.getName().equals( name )
				|| double.class.getName().equals( name )
				|| float.class.getName().equals( name ) ) {
			// nothing to do for primitives
		}
		else if ( Byte.class.getSimpleName().equalsIgnoreCase( name ) ) {
			name = Byte.class.getName();
		}
		else if ( Boolean.class.getSimpleName().equalsIgnoreCase( name ) ) {
			name = Boolean.class.getName();
		}
		else if ( Short.class.getSimpleName().equalsIgnoreCase( name ) ) {
			name = Short.class.getName();
		}
		else if ( Integer.class.getSimpleName().equalsIgnoreCase( name ) ) {
			name = Integer.class.getName();
		}
		else if ( Long.class.getSimpleName().equalsIgnoreCase( name ) ) {
			name = Long.class.getName();
		}
		else if ( Double.class.getSimpleName().equalsIgnoreCase( name ) ) {
			name = Double.class.getName();
		}
		else if ( Float.class.getSimpleName().equalsIgnoreCase( name ) ) {
			name = Float.class.getName();
		}
		else if ( BigInteger.class.getSimpleName().equalsIgnoreCase( name ) ) {
			name = BigInteger.class.getName();
		}
		else if ( BigDecimal.class.getSimpleName().equalsIgnoreCase( name ) ) {
			name = BigDecimal.class.getName();
		}
		else if ( String.class.getSimpleName().equalsIgnoreCase( name ) ) {
			name = String.class.getName();
		}
		else if ( Character.class.getSimpleName().equalsIgnoreCase( name ) ) {
			name = Character.class.getName();
		}
		else if ( UUID.class.getSimpleName().equalsIgnoreCase( name ) ) {
			name = Character.class.getName();
		}
		else if ( URL.class.getSimpleName().equalsIgnoreCase( name ) ) {
			name = URL.class.getName();
		}
		else if ( Blob.class.getSimpleName().equalsIgnoreCase( name ) ) {
			name = Blob.class.getName();
		}
		else if ( Clob.class.getSimpleName().equalsIgnoreCase( name ) ) {
			name = Clob.class.getName();
		}
		else if ( NClob.class.getSimpleName().equalsIgnoreCase( name ) ) {
			name = NClob.class.getName();
		}
		else if ( Instant.class.getSimpleName().equalsIgnoreCase( name ) ) {
			name = Instant.class.getName();
		}
		else if ( LocalDate.class.getSimpleName().equalsIgnoreCase( name ) ) {
			name = LocalDate.class.getName();
		}
		else if ( LocalTime.class.getSimpleName().equalsIgnoreCase( name ) ) {
			name = LocalTime.class.getName();
		}
		else if ( LocalDateTime.class.getSimpleName().equalsIgnoreCase( name ) ) {
			name = LocalDateTime.class.getName();
		}
		else if ( ZonedDateTime.class.getSimpleName().equalsIgnoreCase( name ) ) {
			name = ZonedDateTime.class.getName();
		}
		else if ( OffsetTime.class.getSimpleName().equalsIgnoreCase( name ) ) {
			name = OffsetTime.class.getName();
		}
		else if ( OffsetDateTime.class.getSimpleName().equalsIgnoreCase( name ) ) {
			name = OffsetDateTime.class.getName();
		}
		else {
			name = StringHelper.qualifyConditionallyIfNot( packageName, name );
		}

		return classDetailsRegistry.resolveClassDetails( name );
	}

	public static void applyBasicTypeComposition(
			JaxbBasicMapping jaxbBasicMapping,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbBasicMapping.getType() != null ) {
			applyUserType( jaxbBasicMapping.getType(), memberDetails, xmlDocumentContext );
		}
		else if ( jaxbBasicMapping.getJavaType() != null ) {
			applyJavaTypeDescriptor( jaxbBasicMapping.getJavaType(), memberDetails, xmlDocumentContext );
		}

		if ( isNotEmpty( jaxbBasicMapping.getJdbcType() ) ) {
			applyJdbcTypeDescriptor( jaxbBasicMapping.getJdbcType(), memberDetails, xmlDocumentContext );
		}
		else if ( jaxbBasicMapping.getJdbcTypeCode() != null ) {
			applyJdbcTypeCode( jaxbBasicMapping.getJdbcTypeCode(), memberDetails, xmlDocumentContext );
		}
		else if ( isNotEmpty( jaxbBasicMapping.getJdbcTypeName() ) ) {
			applyJdbcTypeCode(
					resolveJdbcTypeName( jaxbBasicMapping.getJdbcTypeName() ),
					memberDetails,
					xmlDocumentContext
			);
		}
	}

	private static int resolveJdbcTypeName(String name) {
		try {
			final Field matchingField = SqlTypes.class.getDeclaredField( name );
			return matchingField.getInt( null );
		}
		catch (NoSuchFieldException | IllegalAccessException e) {
			throw new ModelsException( "Could not resolve <jdbc-type-name>" + name + "</jdbc-type-name>", e );
		}
	}

	public static void applyJavaTypeDescriptor(
			String descriptorClassName,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final JavaTypeAnnotation typeAnn = (JavaTypeAnnotation) memberDetails.applyAnnotationUsage(
				HibernateAnnotations.JAVA_TYPE,
				xmlDocumentContext.getModelBuildingContext()
		);

		final ClassDetails descriptorClass = xmlDocumentContext
				.getModelBuildingContext()
				.getClassDetailsRegistry()
				.resolveClassDetails( descriptorClassName );
		typeAnn.value( descriptorClass.toJavaClass() );
	}


	private static void applyJdbcTypeDescriptor(
			String descriptorClassName,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final ClassDetails descriptorClassDetails = xmlDocumentContext
				.getModelBuildingContext()
				.getClassDetailsRegistry()
				.resolveClassDetails( descriptorClassName );
		final JdbcTypeAnnotation jdbcTypeAnn = (JdbcTypeAnnotation) memberDetails.applyAnnotationUsage(
				HibernateAnnotations.JDBC_TYPE,
				xmlDocumentContext.getModelBuildingContext()
		);
		jdbcTypeAnn.value( descriptorClassDetails.toJavaClass() );

	}

	public static void applyJdbcTypeCode(
			Integer jdbcTypeCode,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jdbcTypeCode == null ) {
			return;
		}

		final JdbcTypeCodeAnnotation typeCodeAnn = (JdbcTypeCodeAnnotation) memberDetails.applyAnnotationUsage(
				HibernateAnnotations.JDBC_TYPE_CODE,
				xmlDocumentContext.getModelBuildingContext()
		);
		typeCodeAnn.value( jdbcTypeCode );
	}

	public static void applyFilters(
			List<JaxbFilterImpl> jaxbFilters,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		if ( isEmpty( jaxbFilters ) ) {
			return;
		}

		final FiltersAnnotation filters = (FiltersAnnotation) target.replaceAnnotationUsage(
				FILTER,
				HibernateAnnotations.FILTERS,
				xmlDocumentContext.getModelBuildingContext()
		);

		final FilterAnnotation[] filterUsages = new FilterAnnotation[jaxbFilters.size()];
		filters.value( filterUsages );

		for ( int i = 0; i < jaxbFilters.size(); i++ ) {
			final FilterAnnotation filterUsage = FILTER.createUsage( xmlDocumentContext.getModelBuildingContext() );
			filterUsages[i] = filterUsage;
			filterUsage.apply( jaxbFilters.get(i), xmlDocumentContext );
		}
	}

	public static void applyJoinTableFilters(
			List<JaxbFilterImpl> jaxbFilters,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		if ( isEmpty( jaxbFilters ) ) {
			return;
		}

		final FilterJoinTablesAnnotation filters = (FilterJoinTablesAnnotation) target.replaceAnnotationUsage(
				FILTER_JOIN_TABLE,
				HibernateAnnotations.FILTER_JOIN_TABLES,
				xmlDocumentContext.getModelBuildingContext()
		);

		final FilterJoinTableAnnotation[] filterUsages = new FilterJoinTableAnnotation[jaxbFilters.size()];
		filters.value( filterUsages );

		for ( int i = 0; i < jaxbFilters.size(); i++ ) {
			final FilterJoinTableAnnotation filterUsage = FILTER_JOIN_TABLE.createUsage( xmlDocumentContext.getModelBuildingContext() );
			filterUsages[i] = filterUsage;

			filterUsage.apply( jaxbFilters.get(i), xmlDocumentContext );
		}
	}

	public static void applySqlRestriction(
			String sqlRestriction,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		if ( isEmpty( sqlRestriction ) ) {
			return;
		}

		final SQLRestrictionAnnotation sqlRestrictionAnn = (SQLRestrictionAnnotation) target.applyAnnotationUsage(
				SQL_RESTRICTION,
				xmlDocumentContext.getModelBuildingContext()
		);
		sqlRestrictionAnn.value( sqlRestriction );
	}

	public static void applySqlJoinTableRestriction(
			String sqlRestriction,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		if ( isEmpty( sqlRestriction ) ) {
			return;
		}
		final SQLJoinTableRestrictionAnnotation sqlRestrictionAnn = (SQLJoinTableRestrictionAnnotation) target.applyAnnotationUsage(
				HibernateAnnotations.SQL_JOIN_TABLE_RESTRICTION,
				xmlDocumentContext.getModelBuildingContext()
		);
		sqlRestrictionAnn.value( sqlRestriction );
	}

	public static void applyCustomSql(
			JaxbCustomSqlImpl jaxbCustomSql,
			MutableAnnotationTarget target,
			AnnotationDescriptor descriptor,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbCustomSql == null ) {
			return;
		}

		final CustomSqlDetails annotation = (CustomSqlDetails) target.applyAnnotationUsage(
				descriptor,
				xmlDocumentContext.getModelBuildingContext()
		);

		applyCustomSql( jaxbCustomSql, annotation );
	}

	public static void applyCustomSql(
			JaxbCustomSqlImpl jaxbCustomSql,
			CustomSqlDetails annotation) {
		if ( jaxbCustomSql == null ) {
			return;
		}

		annotation.sql( jaxbCustomSql.getValue() );
		annotation.callable( jaxbCustomSql.isCallable() );

		if ( isNotEmpty( jaxbCustomSql.getTable() ) ) {
			annotation.table( jaxbCustomSql.getTable() );
		}

		if ( jaxbCustomSql.getResultCheck() != null ) {
			annotation.check( interpretResultCheckStyle( jaxbCustomSql.getResultCheck() ) );
		}
	}

	@SuppressWarnings({ "deprecation", "removal" })
	private static ResultCheckStyle interpretResultCheckStyle(ExecuteUpdateResultCheckStyle style) {
		return switch ( style ) {
			case NONE -> ResultCheckStyle.NONE;
			case COUNT -> ResultCheckStyle.COUNT;
			case PARAM -> ResultCheckStyle.PARAM;
		};
	}

	static void applyIdClass(
			JaxbIdClassImpl jaxbIdClass,
			MutableClassDetails target,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbIdClass == null || isEmpty( jaxbIdClass.getClazz() ) ) {
			return;
		}

		final IdClassJpaAnnotation idClassAnn = (IdClassJpaAnnotation) target.applyAnnotationUsage(
				JpaAnnotations.ID_CLASS,
				xmlDocumentContext.getModelBuildingContext()
		);

		final ClassDetails idClassImpl = xmlDocumentContext.getModelBuildingContext()
				.getClassDetailsRegistry()
				.resolveClassDetails( jaxbIdClass.getClazz() );
		idClassAnn.value( idClassImpl.toJavaClass() );
	}

	public static void applyLifecycleCallbacks(
			JaxbEntityOrMappedSuperclass jaxbClass,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		final ModelsContext modelBuildingContext = xmlDocumentContext.getModelBuildingContext();

		if ( jaxbClass.getExcludeDefaultListeners() != null ) {
			classDetails.applyAnnotationUsage( EXCLUDE_DEFAULT_LISTENERS, modelBuildingContext );
		}

		if ( jaxbClass.getExcludeSuperclassListeners() != null ) {
			classDetails.applyAnnotationUsage( EXCLUDE_SUPERCLASS_LISTENERS, modelBuildingContext );
		}

		applyLifecycleCallbacks( jaxbClass, JpaEventListenerStyle.CALLBACK, classDetails, xmlDocumentContext );

		applyEntityListeners( jaxbClass.getEntityListenerContainer(), classDetails, xmlDocumentContext );
	}

	public static void applyEntityListeners(
			JaxbEntityListenerContainerImpl entityListenerContainer,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( entityListenerContainer == null || entityListenerContainer.getEntityListeners().isEmpty() ) {
			return;
		}

		final EntityListenersJpaAnnotation listenersUsage = (EntityListenersJpaAnnotation) classDetails.replaceAnnotationUsage(
				JpaAnnotations.ENTITY_LISTENERS,
				xmlDocumentContext.getModelBuildingContext()
		);

		final Class<?>[] listeners = new Class[entityListenerContainer.getEntityListeners().size()];
		listenersUsage.value( listeners );

		for ( int i = 0; i < entityListenerContainer.getEntityListeners().size(); i++ ) {
			final JaxbEntityListenerImpl jaxbEntityListener = entityListenerContainer.getEntityListeners().get( i );
			final MutableClassDetails entityListenerClass = xmlDocumentContext.resolveJavaType( jaxbEntityListener.getClazz() );
			applyLifecycleCallbacks(
					jaxbEntityListener,
					JpaEventListenerStyle.LISTENER,
					entityListenerClass,
					xmlDocumentContext
			);

			// todo (7.0) : need to make sure we look up the ClassDetails later when applying listeners; the *Class* won't be correct
			listeners[i] = entityListenerClass.toJavaClass();
		}
	}

	static void applyLifecycleCallbacks(
			JaxbLifecycleCallbackContainer lifecycleCallbackContainer,
			JpaEventListenerStyle callbackType,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyLifecycleCallback( lifecycleCallbackContainer.getPrePersist(), callbackType, JpaAnnotations.PRE_PERSIST, classDetails, xmlDocumentContext );
		applyLifecycleCallback( lifecycleCallbackContainer.getPostPersist(), callbackType, JpaAnnotations.POST_PERSIST, classDetails, xmlDocumentContext );
		applyLifecycleCallback( lifecycleCallbackContainer.getPreRemove(), callbackType, JpaAnnotations.PRE_REMOVE, classDetails, xmlDocumentContext );
		applyLifecycleCallback( lifecycleCallbackContainer.getPostRemove(), callbackType, JpaAnnotations.POST_REMOVE, classDetails, xmlDocumentContext );
		applyLifecycleCallback( lifecycleCallbackContainer.getPreUpdate(), callbackType, JpaAnnotations.PRE_UPDATE, classDetails, xmlDocumentContext );
		applyLifecycleCallback( lifecycleCallbackContainer.getPostUpdate(), callbackType, JpaAnnotations.POST_UPDATE, classDetails, xmlDocumentContext );
		applyLifecycleCallback( lifecycleCallbackContainer.getPostLoad(), callbackType, JpaAnnotations.POST_LOAD, classDetails, xmlDocumentContext );
	}

	private static <A extends Annotation> void applyLifecycleCallback(
			JaxbLifecycleCallback lifecycleCallback,
			JpaEventListenerStyle callbackType,
			AnnotationDescriptor<A> annotationDescriptor,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( lifecycleCallback != null ) {
			final MutableMemberDetails methodDetails = getCallbackMethodDetails(
					lifecycleCallback.getMethodName(),
					callbackType,
					classDetails
			);
			if ( methodDetails == null ) {
				throw new AnnotationException( String.format(
						"Lifecycle callback method not found - %s (%s)",
						lifecycleCallback.getMethodName(),
						classDetails.getName()
				) );
			}

			methodDetails.applyAnnotationUsage( annotationDescriptor, xmlDocumentContext.getModelBuildingContext() );
		}
	}

	private static MutableMemberDetails getCallbackMethodDetails(
			String name,
			JpaEventListenerStyle callbackType,
			ClassDetails classDetails) {
		for ( MethodDetails method : classDetails.getMethods() ) {
			if ( method.getName().equals( name )
					&& JpaEventListener.matchesSignature( callbackType, method ) ) {
				return (MutableMemberDetails) method;
			}
		}
		return null;
	}

	static void applyRowId(
			String rowId,
			MutableClassDetails target,
			XmlDocumentContext xmlDocumentContext) {
		if ( rowId == null ) {
			return;
		}

		final RowIdAnnotation rowIdAnn = (RowIdAnnotation) target.applyAnnotationUsage(
				HibernateAnnotations.ROW_ID,
				xmlDocumentContext.getModelBuildingContext()
		);
		if ( isNotEmpty( rowId ) ) {
			rowIdAnn.value( rowId );
		}
	}

	private static String prefixIfNotAlready(String value, String prefix) {
		if ( isNotEmpty( prefix ) ) {
			final String previous = unqualify( value );
			if ( !previous.equalsIgnoreCase( prefix ) ) {
				return StringHelper.qualify( prefix, value );
			}
		}
		return value;
	}

	static void applyDiscriminatorValue(
			String discriminatorValue,
			MutableClassDetails target,
			XmlDocumentContext xmlDocumentContext) {
		if ( isEmpty( discriminatorValue ) ) {
			return;
		}

		final DiscriminatorValueJpaAnnotation valueAnn = (DiscriminatorValueJpaAnnotation) target.applyAnnotationUsage(
				JpaAnnotations.DISCRIMINATOR_VALUE,
				xmlDocumentContext.getModelBuildingContext()
		);
		valueAnn.value( discriminatorValue );
	}

	static void applyDiscriminatorColumn(
			JaxbDiscriminatorColumnImpl jaxbDiscriminatorColumn,
			MutableClassDetails target,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbDiscriminatorColumn == null ) {
			return;
		}

		final DiscriminatorColumnJpaAnnotation discriminatorColumnAnn = (DiscriminatorColumnJpaAnnotation) target.applyAnnotationUsage(
				JpaAnnotations.DISCRIMINATOR_COLUMN,
				xmlDocumentContext.getModelBuildingContext()
		);
		discriminatorColumnAnn.apply( jaxbDiscriminatorColumn, xmlDocumentContext );

		if ( jaxbDiscriminatorColumn.isForceSelection() || jaxbDiscriminatorColumn.isInsertable() == FALSE ) {
			final DiscriminatorOptionsAnnotation optionsAnn = (DiscriminatorOptionsAnnotation) target.applyAnnotationUsage(
					HibernateAnnotations.DISCRIMINATOR_OPTIONS,
					xmlDocumentContext.getModelBuildingContext()
			);
			optionsAnn.force( true );

			if ( jaxbDiscriminatorColumn.isInsertable() != null ) {
				optionsAnn.insert( jaxbDiscriminatorColumn.isInsertable() );
			}
		}
	}

	public static void applyDiscriminatorFormula(
			@Nullable JaxbDiscriminatorFormulaImpl jaxbDiscriminatorFormula,
			MutableClassDetails target,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbDiscriminatorFormula == null ) {
			return;
		}
		if ( isEmpty( jaxbDiscriminatorFormula.getFragment() ) ) {
			return;
		}

		final DiscriminatorFormulaAnnotation discriminatorFormulaAnn = (DiscriminatorFormulaAnnotation) target.applyAnnotationUsage(
				HibernateAnnotations.DISCRIMINATOR_FORMULA,
				xmlDocumentContext.getModelBuildingContext()
		);

		discriminatorFormulaAnn.value( jaxbDiscriminatorFormula.getFragment() );
		if ( jaxbDiscriminatorFormula.getDiscriminatorType() != null ) {
			discriminatorFormulaAnn.discriminatorType( jaxbDiscriminatorFormula.getDiscriminatorType() );
		}

		if ( jaxbDiscriminatorFormula.isForceSelection() ) {
			final DiscriminatorOptionsAnnotation optionsAnn = (DiscriminatorOptionsAnnotation) target.applyAnnotationUsage(
					HibernateAnnotations.DISCRIMINATOR_OPTIONS,
					xmlDocumentContext.getModelBuildingContext()
			);
			optionsAnn.force( true );
		}
	}

	public static String determineTargetName(String explicitName, XmlDocumentContext xmlDocumentContext) {
		final String qualifiedName = StringHelper.qualifyConditionallyIfNot(
				xmlDocumentContext.getXmlDocument().getDefaults().getPackage(),
				explicitName
		);
		final ClassDetails classDetails = xmlDocumentContext.getModelBuildingContext()
				.getClassDetailsRegistry()
				.resolveClassDetails( qualifiedName );
		if ( classDetails != null ) {
			return classDetails.getName();
		}

		return explicitName;
	}


	/**
	 * Applies the schema defined either <ol>
	 *     <li>The JAXB node directly</li>
	 *     <li>The XML document's {@code <schema/>} element</li>
	 * </ol>
	 *
	 * @apiNote The schema defined in {@code <persistence-unit-defaults/>}, if any,
	 * is NOT handled here
	 */
	public static void applySchema(
			JaxbSchemaAware jaxbNode,
			DatabaseObjectDetails annotationUsage,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbNode == null ) {
			return;
		}

		if ( isNotEmpty( jaxbNode.getSchema() ) ) {
			annotationUsage.schema( jaxbNode.getSchema() );
		}
		else if ( isNotEmpty( documentSchema( xmlDocumentContext ) ) ) {
			annotationUsage.schema( documentSchema( xmlDocumentContext ) );
		}
	}

	/**
	 * Returns the XML document's {@code <schema/>} element value, if one
	 *
	 * @apiNote This is NOT the same as a {@code <schema/>} defined in {@code <persistence-unit-defaults/>}
	 */
	private static String documentSchema(XmlDocumentContext xmlDocumentContext) {
		return xmlDocumentContext.getXmlDocument()
				.getDefaults()
				.getSchema();
	}

	/**
	 * Applies the catalog defined either <ol>
	 *     <li>The JAXB node directly</li>
	 *     <li>The XML document's {@code <catalog/>} element</li>
	 * </ol>
	 *
	 * @apiNote The catalog defined in {@code <persistence-unit-defaults/>}, if any,
	 * is NOT handled here
	 */
	public static void applyCatalog(
			JaxbSchemaAware jaxbNode,
			DatabaseObjectDetails annotationUsage,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbNode == null ) {
			return;
		}

		if ( isNotEmpty( jaxbNode.getCatalog() ) ) {
			annotationUsage.catalog( jaxbNode.getCatalog() );
		}
		else if ( isNotEmpty( documentCatalog( xmlDocumentContext ) ) ) {
			annotationUsage.catalog( documentCatalog( xmlDocumentContext ) );
		}
	}

	/**
	 * Returns the XML document's {@code <catalog/>} element value, if one
	 *
	 * @apiNote This is NOT the same as a {@code <catalog/>} defined in {@code <persistence-unit-defaults/>}
	 */
	private static String documentCatalog(XmlDocumentContext xmlDocumentContext) {
		return xmlDocumentContext.getXmlDocument()
				.getDefaults()
				.getCatalog();
	}

	public static void applyNotFound(
			JaxbNotFoundCapable jaxbNode,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		assert jaxbNode != null;

		final NotFoundAction notFoundAction = jaxbNode.getNotFound();
		if ( notFoundAction == null || notFoundAction == NotFoundAction.EXCEPTION ) {
			return;
		}

		final NotFoundAnnotation notFoundAnn = (NotFoundAnnotation) memberDetails.applyAnnotationUsage(
				HibernateAnnotations.NOT_FOUND,
				xmlDocumentContext.getModelBuildingContext()
		);
		notFoundAnn.action( notFoundAction );
	}

	public static void applySecondaryTables(List<JaxbSecondaryTableImpl> jaxbSecondaryTables, MutableAnnotationTarget target, XmlDocumentContext xmlDocumentContext) {
		if ( jaxbSecondaryTables == null || jaxbSecondaryTables.isEmpty() ) {
			return;
		}

		final SecondaryTablesJpaAnnotation tablesUsage = (SecondaryTablesJpaAnnotation) target.replaceAnnotationUsage(
				SECONDARY_TABLE,
				JpaAnnotations.SECONDARY_TABLES,
				xmlDocumentContext.getModelBuildingContext()
		);
		final SecondaryTable[] tableUsages = new SecondaryTable[jaxbSecondaryTables.size()];
		tablesUsage.value( tableUsages );

		final SecondaryRowsAnnotation rowsUsage = (SecondaryRowsAnnotation) target.replaceAnnotationUsage(
				SECONDARY_ROW,
				SECONDARY_ROWS,
				xmlDocumentContext.getModelBuildingContext()
		);
		final SecondaryRow[] rowUsages = new SecondaryRow[jaxbSecondaryTables.size()];
		rowsUsage.value( rowUsages );

		for ( int i = 0; i < jaxbSecondaryTables.size(); i++ ) {
			final SecondaryTableJpaAnnotation tableUsage = SECONDARY_TABLE.createUsage( xmlDocumentContext.getModelBuildingContext() );
			tableUsages[i] = tableUsage;

			final JaxbSecondaryTableImpl jaxbSecondaryTable = jaxbSecondaryTables.get( i );
			tableUsage.apply( jaxbSecondaryTable, xmlDocumentContext );

			final SecondaryRowAnnotation rowUsage = SECONDARY_ROW.createUsage( xmlDocumentContext.getModelBuildingContext() );
			rowUsages[i] = rowUsage;
			rowUsage.table( tableUsage.name() );
			rowUsage.optional( jaxbSecondaryTable.isOptional() == TRUE );
			rowUsage.owned( jaxbSecondaryTable.isOwned() == TRUE );
		}
	}

	private static final CheckConstraint[] NO_CHECK_CONSTRAINTS = new CheckConstraint[0];
	public static CheckConstraint[] collectCheckConstraints(
			List<JaxbCheckConstraintImpl> jaxbChecks,
			XmlDocumentContext xmlDocumentContext) {
		if ( isEmpty( jaxbChecks ) ) {
			return NO_CHECK_CONSTRAINTS;
		}

		final CheckConstraint[] checks = new CheckConstraint[jaxbChecks.size()];
		for ( int i = 0; i < jaxbChecks.size(); i++ ) {
			final JaxbCheckConstraintImpl jaxbCheck = jaxbChecks.get( i );
			final CheckConstraintJpaAnnotation annotation = CHECK_CONSTRAINT.createUsage( xmlDocumentContext.getModelBuildingContext() );
			checks[i] = annotation;
			annotation.constraint( jaxbCheck.getConstraint() );
			applyOptionalString( jaxbCheck.getName(), annotation::name );
			applyOptionalString( jaxbCheck.getOptions(), annotation::options );
		}
		return checks;
	}

	private static final UniqueConstraint[] NO_UNIQUE_CONSTRAINTS = new UniqueConstraint[0];
	public static UniqueConstraint[] collectUniqueConstraints(
			List<JaxbUniqueConstraintImpl> jaxbUniqueConstraints,
			XmlDocumentContext xmlDocumentContext) {
		return collectUniqueConstraints( jaxbUniqueConstraints, xmlDocumentContext.getModelBuildingContext() );
	}

	public static UniqueConstraint[] collectUniqueConstraints(
			List<JaxbUniqueConstraintImpl> jaxbUniqueConstraints,
			ModelsContext modelContext) {
		if ( isEmpty( jaxbUniqueConstraints ) ) {
			return NO_UNIQUE_CONSTRAINTS;
		}

		final UniqueConstraint[] constraints = new UniqueConstraint[jaxbUniqueConstraints.size()];
		for ( int i = 0; i < jaxbUniqueConstraints.size(); i++ ) {
			final UniqueConstraintJpaAnnotation uniqueConstraint = UNIQUE_CONSTRAINT.createUsage( modelContext );
			constraints[i] = uniqueConstraint;

			final JaxbUniqueConstraintImpl jaxbUniqueConstraint = jaxbUniqueConstraints.get( i );
			uniqueConstraint.columnNames( jaxbUniqueConstraint.getColumnName().toArray(String[]::new) );
			applyOptionalString( jaxbUniqueConstraint.getName(), uniqueConstraint::name );
			applyOptionalString( jaxbUniqueConstraint.getOptions(), uniqueConstraint::options );
		}
		return constraints;
	}

	private static final Index[] NO_INDICES = new Index[0];
	public static Index[] collectIndexes(
			List<JaxbIndexImpl> jaxbIndexes,
			XmlDocumentContext xmlDocumentContext) {
		return collectIndexes( jaxbIndexes, xmlDocumentContext.getModelBuildingContext() );
	}

	public static Index[] collectIndexes(
			List<JaxbIndexImpl> jaxbIndexes,
			ModelsContext sourceModelContext) {
		if ( isEmpty( jaxbIndexes ) ) {
			return NO_INDICES;
		}

		final Index[] indexes = new Index[jaxbIndexes.size()];
		for ( int i = 0; i < jaxbIndexes.size(); i++ ) {
			final IndexJpaAnnotation index = INDEX.createUsage( sourceModelContext );
			indexes[i] = index;

			final JaxbIndexImpl jaxbIndex = jaxbIndexes.get( i );
			index.columnList( jaxbIndex.getColumnList() );
			applyOptionalString( jaxbIndex.getName(), index::name );
			if ( jaxbIndex.isUnique() != null ) {
				index.unique( jaxbIndex.isUnique() );
			}
		}
		return indexes;
	}



	public static void applyPrimaryKeyJoinColumns(
			JaxbEntityImpl jaxbEntity,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		List<JaxbPrimaryKeyJoinColumnImpl> jaxbColumns = jaxbEntity.getPrimaryKeyJoinColumns();
		if ( isEmpty( jaxbColumns ) ) {
			return;
		}

		final ModelsContext modelBuildingContext = xmlDocumentContext.getModelBuildingContext();
		final PrimaryKeyJoinColumnsJpaAnnotation columnsAnn = (PrimaryKeyJoinColumnsJpaAnnotation) classDetails.replaceAnnotationUsage(
				JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN,
				JpaAnnotations.PRIMARY_KEY_JOIN_COLUMNS,
				modelBuildingContext
		);

		final PrimaryKeyJoinColumn[] columns = new PrimaryKeyJoinColumn[jaxbColumns.size()];
		for ( int i = 0; i < jaxbColumns.size(); i++ ) {
			final PrimaryKeyJoinColumnJpaAnnotation column = JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN.createUsage( modelBuildingContext );
			final JaxbPrimaryKeyJoinColumnImpl jaxbColumn = jaxbColumns.get( i );
			column.apply( jaxbColumn, xmlDocumentContext );
			columns[i] = column;
		}
		columnsAnn.value( columns );
	}

	public static void applyCollectionClassification(
			LimitedCollectionClassification classification,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		CollectionClassificationXmlAnnotation collectionClassification = (CollectionClassificationXmlAnnotation) memberDetails.applyAnnotationUsage(
				HibernateAnnotations.COLLECTION_CLASSIFICATION,
				xmlDocumentContext.getModelBuildingContext()
		);
		collectionClassification.value( classification );
	}

	public static void applyVersion(JaxbVersionImpl version, MutableClassDetails mutableClassDetails, AccessType classAccessType, XmlDocumentContext xmlDocumentContext) {
		if ( version != null ) {
			final AccessType accessType = coalesce( version.getAccess(), classAccessType );
			final String versionAttributeName = version.getName();

			final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
					versionAttributeName,
					accessType,
					mutableClassDetails
			);
			memberDetails.applyAnnotationUsage(
					JpaAnnotations.VERSION,
					xmlDocumentContext.getModelBuildingContext()
			);
			CommonAttributeProcessing.applyAccess( accessType, memberDetails, xmlDocumentContext );
			CommonAttributeProcessing.applyAttributeAccessor( version, memberDetails, xmlDocumentContext );
			XmlAnnotationHelper.applyTemporal( version.getTemporal(), memberDetails, xmlDocumentContext );
			if ( version.getColumn() != null ) {
				final ColumnJpaAnnotation columnAnn = (ColumnJpaAnnotation) memberDetails.applyAnnotationUsage(
						JpaAnnotations.COLUMN,
						xmlDocumentContext.getModelBuildingContext()
				);
				columnAnn.apply( version.getColumn(), xmlDocumentContext );
				XmlAnnotationHelper.applyColumnTransformation( version.getColumn(), memberDetails, xmlDocumentContext );
			}
		}
	}
}
