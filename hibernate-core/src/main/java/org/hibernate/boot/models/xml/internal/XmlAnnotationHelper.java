/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.CollectionType;
import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.annotations.DiscriminatorOptions;
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.RowId;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.boot.internal.Target;
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
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenerContainerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityOrMappedSuperclass;
import org.hibernate.boot.jaxb.mapping.spi.JaxbGeneratedValueImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbHbmFilterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIdClassImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIndexImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJoinColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbLifecycleCallback;
import org.hibernate.boot.jaxb.mapping.spi.JaxbLifecycleCallbackContainer;
import org.hibernate.boot.jaxb.mapping.spi.JaxbLobImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToManyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNationalizedImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNaturalId;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNotFoundCapable;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPluralAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSchemaAware;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSequenceGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTableGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUniqueConstraintImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUserTypeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUuidGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.db.JaxbCheckable;
import org.hibernate.boot.jaxb.mapping.spi.db.JaxbTableMapping;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.categorize.spi.JpaEventListener;
import org.hibernate.boot.models.categorize.spi.JpaEventListenerStyle;
import org.hibernate.boot.models.internal.AnnotationUsageHelper;
import org.hibernate.boot.models.xml.internal.db.ColumnProcessing;
import org.hibernate.boot.models.xml.internal.db.ForeignKeyProcessing;
import org.hibernate.boot.models.xml.internal.db.JoinColumnProcessing;
import org.hibernate.boot.models.xml.internal.db.TableProcessing;
import org.hibernate.boot.models.xml.spi.XmlDocument;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.internal.util.KeyedConsumer;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.MutableAnnotationTarget;
import org.hibernate.models.spi.MutableAnnotationUsage;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converts;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.lang.Boolean.FALSE;
import static java.util.Collections.emptyList;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * Helper for creating annotation from equivalent JAXB
 *
 * @author Steve Ebersole
 */
public class XmlAnnotationHelper {

	public static void applyOptionalAttribute(MutableAnnotationUsage<? extends Annotation> annotationUsage, String attributeName, Object value) {
		if ( value != null ) {
			annotationUsage.setAttributeValue( attributeName, value );
		}
	}

	public static void applyOptionalAttribute(MutableAnnotationUsage<? extends Annotation> annotationUsage, String attributeName, String value) {
		if ( StringHelper.isNotEmpty( value ) ) {
			annotationUsage.setAttributeValue( attributeName, value );
		}
	}

	/**
	 * Handle creating {@linkplain Entity @Entity} from an {@code <entity/>} element.
	 * Used in both complete and override modes.
	 */
	public static void applyEntity(
			JaxbEntity jaxbEntity,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<Entity> entityAnn = classDetails.applyAnnotationUsage(
				JpaAnnotations.ENTITY,
				xmlDocumentContext.getModelBuildingContext()
		);
		XmlProcessingHelper.applyAttributeIfSpecified( "name", jaxbEntity.getName(), entityAnn );
	}

	public static void applyColumn(
			JaxbColumnImpl jaxbColumn,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbColumn == null ) {
			return;
		}

		createColumnAnnotation( jaxbColumn, memberDetails, xmlDocumentContext );
	}

	private static MutableAnnotationUsage<Column> createColumnAnnotation(
			JaxbColumnImpl jaxbColumn,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<Column> columnAnnotationUsage = target.applyAnnotationUsage(
				JpaAnnotations.COLUMN,
				xmlDocumentContext.getModelBuildingContext()
		);

		ColumnProcessing.applyColumnDetails( jaxbColumn, target, columnAnnotationUsage, xmlDocumentContext );

		return columnAnnotationUsage;
	}


	public static <T,N> void applyOr(
			N jaxbNode,
			Function<N,T> jaxbValueAccess,
			String name,
			KeyedConsumer<String, T> valueConsumer,
			Supplier<T> defaultValueProvider) {
		if ( jaxbNode != null ) {
			final T value = jaxbValueAccess.apply( jaxbNode );
			if ( value != null ) {
				valueConsumer.accept( name, value );
				return;
			}
		}

		valueConsumer.accept( name, defaultValueProvider.get() );
	}

	public static <T,N,A extends Annotation> void applyOr(
			N jaxbNode,
			Function<N,T> jaxbValueAccess,
			String name,
			MutableAnnotationUsage<A> annotationUsage,
			AnnotationDescriptor<A> annotationDescriptor) {
		//noinspection unchecked
		applyOr(
				jaxbNode,
				jaxbValueAccess,
				name,
				(key, value) -> annotationUsage.setAttributeValue( name, value ),
				() -> (T) annotationDescriptor.getAttribute( name ).getAttributeMethod().getDefaultValue()
		);
	}







	public static void applyUserType(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbType == null ) {
			return;
		}

		final MutableAnnotationUsage<Type> typeAnn = memberDetails.applyAnnotationUsage(
				HibernateAnnotations.TYPE,
				xmlDocumentContext.getModelBuildingContext()
		);

		final ClassDetails userTypeImpl = resolveJavaType( jaxbType.getValue(), xmlDocumentContext );
		typeAnn.setAttributeValue( "value", userTypeImpl );
		typeAnn.setAttributeValue( "parameters", collectParameters( jaxbType.getParameters(), memberDetails, xmlDocumentContext ) );
	}

	public static List<AnnotationUsage<Parameter>> collectParameters(
			List<JaxbConfigurationParameterImpl> jaxbParameters,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbParameters ) ) {
			return emptyList();
		}

		List<AnnotationUsage<Parameter>> parameterAnnList = new ArrayList<>( jaxbParameters.size() );
		jaxbParameters.forEach( (jaxbParam) -> {
			final MutableAnnotationUsage<Parameter> parameterUsage =
					HibernateAnnotations.PARAMETER.createUsage( xmlDocumentContext.getModelBuildingContext() );
			parameterAnnList.add( parameterUsage );
			parameterUsage.setAttributeValue( "name", jaxbParam.getName() );
			parameterUsage.setAttributeValue( "value", jaxbParam.getValue() );
		} );
		return parameterAnnList;
	}

	public static void applyCollectionUserType(
			JaxbCollectionUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbType == null ) {
			return;
		}

		final MutableAnnotationUsage<CollectionType> typeAnn = memberDetails.applyAnnotationUsage(
				HibernateAnnotations.COLLECTION_TYPE,
				xmlDocumentContext.getModelBuildingContext()
		);
		final ClassDetails userTypeImpl = resolveJavaType( jaxbType.getType(), xmlDocumentContext );
		typeAnn.setAttributeValue( "type", userTypeImpl );
		typeAnn.setAttributeValue( "parameters", collectParameters( jaxbType.getParameters(), memberDetails, xmlDocumentContext ) );
	}

	public static void applyCollectionId(
			JaxbCollectionIdImpl jaxbCollectionId,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbCollectionId == null ) {
			return;
		}

		final MutableAnnotationUsage<CollectionId> collectionIdAnn = memberDetails.applyAnnotationUsage(
				HibernateAnnotations.COLLECTION_ID,
				xmlDocumentContext.getModelBuildingContext()
		);

		final JaxbColumnImpl jaxbColumn = jaxbCollectionId.getColumn();
		if ( jaxbColumn != null ) {
			collectionIdAnn.setAttributeValue( "column", createColumnAnnotation(
					jaxbColumn,
					memberDetails,
					xmlDocumentContext
			) );
		}

		final JaxbGeneratedValueImpl generator = jaxbCollectionId.getGenerator();
		if ( generator != null && StringHelper.isNotEmpty( generator.getGenerator() ) ) {
			collectionIdAnn.setAttributeValue( "generator", generator.getGenerator() );
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
			final MutableAnnotationUsage<Cascade> cascadeAnn = memberDetails.applyAnnotationUsage(
					HibernateAnnotations.CASCADE,
					xmlDocumentContext.getModelBuildingContext()
			);
			cascadeAnn.setAttributeValue( "value", cascadeTypes );
		}
	}

	public static <A extends Annotation> void applyUniqueConstraints(
			List<JaxbUniqueConstraintImpl> jaxbUniqueConstraints,
			AnnotationTarget target,
			MutableAnnotationUsage<A> annotationUsage,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbUniqueConstraints ) ) {
			return;
		}

		final List<AnnotationUsage<UniqueConstraint>> uniqueConstraintUsages = new ArrayList<>( jaxbUniqueConstraints.size() );
		annotationUsage.setAttributeValue( "uniqueConstraints", uniqueConstraintUsages );

		jaxbUniqueConstraints.forEach( (jaxbUniqueConstraint) -> {
			final MutableAnnotationUsage<UniqueConstraint> ucUsage =
					JpaAnnotations.UNIQUE_CONSTRAINT.createUsage( xmlDocumentContext.getModelBuildingContext() );
			XmlAnnotationHelper.applyOptionalAttribute( ucUsage, "name", jaxbUniqueConstraint.getName() );
			XmlAnnotationHelper.applyOptionalAttribute( ucUsage, "options", jaxbUniqueConstraint.getOptions() );
			ucUsage.setAttributeValue( "columnNames", jaxbUniqueConstraint.getColumnName() );
			uniqueConstraintUsages.add( ucUsage );
		} );
	}

	public static <A extends Annotation> void applyIndexes(
			List<JaxbIndexImpl> jaxbIndexes,
			AnnotationTarget target,
			MutableAnnotationUsage<A> annotationUsage,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbIndexes ) ) {
			return;
		}

		final List<AnnotationUsage<Index>> indexes = new ArrayList<>( jaxbIndexes.size() );
		jaxbIndexes.forEach( jaxbIndex -> {
			final MutableAnnotationUsage<Index> indexAnn =
					JpaAnnotations.INDEX.createUsage( xmlDocumentContext.getModelBuildingContext() );
			applyOr( jaxbIndex, JaxbIndexImpl::getName, "name", indexAnn, JpaAnnotations.INDEX );
			applyOr( jaxbIndex, JaxbIndexImpl::getColumnList, "columnList", indexAnn, JpaAnnotations.INDEX );
			applyOr( jaxbIndex, JaxbIndexImpl::isUnique, "unique", indexAnn, JpaAnnotations.INDEX );
			indexes.add( indexAnn );
		} );

		annotationUsage.setAttributeValue( "indexes", indexes );
	}

	public static <A extends Annotation> void applyCheckConstraints(
			JaxbCheckable jaxbCheckable,
			AnnotationTarget target,
			MutableAnnotationUsage<A> annotationUsage,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbCheckable!= null && CollectionHelper.isNotEmpty( jaxbCheckable.getCheckConstraints() ) ) {
			final List<AnnotationUsage<CheckConstraint>> checks = new ArrayList<>( jaxbCheckable.getCheckConstraints().size() );
			for ( JaxbCheckConstraintImpl jaxbCheck : jaxbCheckable.getCheckConstraints() ) {
				final MutableAnnotationUsage<CheckConstraint> checkAnn = JpaAnnotations.CHECK_CONSTRAINT.createUsage( xmlDocumentContext.getModelBuildingContext() );
				checkAnn.setAttributeValue( "constraint", jaxbCheck.getConstraint() );
				applyOptionalAttribute( checkAnn, "name", jaxbCheck.getName() );
				applyOptionalAttribute( checkAnn, "options", jaxbCheck.getOptions() );
				checks.add( checkAnn );
			}
			annotationUsage.setAttributeValue( "check", checks );
		}
	}

	public static void applyTargetClass(
			String name,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final ClassDetails classDetails = resolveJavaType( name, xmlDocumentContext );
		final MutableAnnotationUsage<Target> targetAnn = memberDetails.applyAnnotationUsage(
				HibernateAnnotations.TARGET,
				xmlDocumentContext.getModelBuildingContext()
		);
		targetAnn.setAttributeValue( "value", classDetails );
	}

	@SuppressWarnings("deprecation")
	public static void applyTemporal(
			TemporalType temporalType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( temporalType == null ) {
			return;
		}

		final MutableAnnotationUsage<Temporal> temporalAnn = memberDetails.applyAnnotationUsage(
				JpaAnnotations.TEMPORAL,
				xmlDocumentContext.getModelBuildingContext()
		);
		temporalAnn.setAttributeValue( "value", temporalType );
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

		final MutableAnnotationUsage<Enumerated> annotationUsage = memberDetails.applyAnnotationUsage(
				JpaAnnotations.ENUMERATED,
				xmlDocumentContext.getModelBuildingContext()
		);

		annotationUsage.setAttributeValue( "value", enumType );
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

		final MutableAnnotationUsage<GeneratedValue> generatedValueAnn = memberDetails.applyAnnotationUsage(
				JpaAnnotations.GENERATED_VALUE,
				xmlDocumentContext.getModelBuildingContext()
		);
		memberDetails.addAnnotationUsage( generatedValueAnn );
		XmlProcessingHelper.applyAttributeIfSpecified( "strategy", jaxbGeneratedValue.getStrategy(), generatedValueAnn );
		XmlProcessingHelper.applyAttributeIfSpecified( "generator", jaxbGeneratedValue.getGenerator(), generatedValueAnn );
	}

	public static void applySequenceGenerator(
			JaxbSequenceGeneratorImpl jaxbGenerator,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbGenerator == null ) {
			return;
		}

		final MutableAnnotationUsage<SequenceGenerator> sequenceAnn = memberDetails.applyAnnotationUsage(
				JpaAnnotations.SEQUENCE_GENERATOR,
				xmlDocumentContext.getModelBuildingContext()
		);

		XmlProcessingHelper.applyAttributeIfSpecified( "sequenceName", jaxbGenerator.getSequenceName(), sequenceAnn );

		XmlProcessingHelper.applyAttributeIfSpecified( "catalog", jaxbGenerator.getCatalog(), sequenceAnn );
		XmlProcessingHelper.applyAttributeIfSpecified( "schema", jaxbGenerator.getSchema(), sequenceAnn );
		XmlProcessingHelper.applyAttributeIfSpecified( "initialValue", jaxbGenerator.getInitialValue(), sequenceAnn );
		XmlProcessingHelper.applyAttributeIfSpecified( "allocationSize", jaxbGenerator.getInitialValue(), sequenceAnn );
		XmlProcessingHelper.applyAttributeIfSpecified( "options", jaxbGenerator.getOptions(), sequenceAnn );
	}

	public static void applyTableGenerator(
			JaxbTableGeneratorImpl jaxbGenerator,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbGenerator == null ) {
			return;
		}

		final MutableAnnotationUsage<TableGenerator> tableAnn = memberDetails.applyAnnotationUsage( JpaAnnotations.TABLE_GENERATOR, xmlDocumentContext.getModelBuildingContext() );
		XmlProcessingHelper.applyAttributeIfSpecified( "name", jaxbGenerator.getName(), tableAnn );
		XmlProcessingHelper.applyAttributeIfSpecified( "table", jaxbGenerator.getTable(), tableAnn );
		XmlProcessingHelper.applyAttributeIfSpecified( "catalog", jaxbGenerator.getCatalog(), tableAnn );
		XmlProcessingHelper.applyAttributeIfSpecified( "schema", jaxbGenerator.getSchema(), tableAnn );
		XmlProcessingHelper.applyAttributeIfSpecified( "pkColumnName", jaxbGenerator.getPkColumnName(), tableAnn );
		XmlProcessingHelper.applyAttributeIfSpecified( "valueColumnName", jaxbGenerator.getValueColumnName(), tableAnn );
		XmlProcessingHelper.applyAttributeIfSpecified( "pkColumnValue", jaxbGenerator.getPkColumnValue(), tableAnn );
		XmlProcessingHelper.applyAttributeIfSpecified( "initialValue", jaxbGenerator.getInitialValue(), tableAnn );
		XmlProcessingHelper.applyAttributeIfSpecified( "allocationSize", jaxbGenerator.getInitialValue(), tableAnn );
		applyUniqueConstraints( jaxbGenerator.getUniqueConstraints(), memberDetails, tableAnn, xmlDocumentContext );
		applyIndexes( jaxbGenerator.getIndexes(), memberDetails, tableAnn, xmlDocumentContext );
	}

	public static void applyUuidGenerator(
			JaxbUuidGeneratorImpl jaxbGenerator,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbGenerator == null ) {
			return;
		}

		final MutableAnnotationUsage<UuidGenerator> uuidGenAnn = memberDetails.applyAnnotationUsage(
				HibernateAnnotations.UUID_GENERATOR,
				xmlDocumentContext.getModelBuildingContext()
		);

		uuidGenAnn.setAttributeValue( "style", jaxbGenerator.getStyle() );
	}

	public static void applyAttributeOverrides(
			JaxbPluralAttribute pluralAttribute,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final List<JaxbAttributeOverrideImpl> jaxbMapKeyOverrides = pluralAttribute.getMapKeyAttributeOverrides();
		final List<JaxbAttributeOverrideImpl> jaxbElementOverrides = pluralAttribute instanceof JaxbElementCollectionImpl
				? ( (JaxbElementCollectionImpl) pluralAttribute ).getAttributeOverrides()
				: emptyList();

		if ( CollectionHelper.isEmpty( jaxbMapKeyOverrides ) && CollectionHelper.isEmpty( jaxbElementOverrides ) ) {
			return;
		}

		final MutableAnnotationUsage<AttributeOverrides> overridesUsage = memberDetails.applyAnnotationUsage(
				JpaAnnotations.ATTRIBUTE_OVERRIDES,
				xmlDocumentContext.getModelBuildingContext()
		);

		final int numberOfOverrides = jaxbMapKeyOverrides.size() + jaxbElementOverrides.size();
		final List<MutableAnnotationUsage<AttributeOverride>> overrideUsages = arrayList( numberOfOverrides );
		overridesUsage.setAttributeValue( "value", overrideUsages );

		// We need to handle overrides for maps specially...
		if ( memberDetails.getMapKeyType() != null ) {
			jaxbMapKeyOverrides.forEach( (jaxbOverride) -> {
				overrideUsages.add( createAttributeOverrideUsage(
						jaxbOverride,
						"key",
						memberDetails,
						xmlDocumentContext
				) );
			} );
			jaxbElementOverrides.forEach( (jaxbOverride) -> {
				overrideUsages.add( createAttributeOverrideUsage(
						jaxbOverride,
						"value",
						memberDetails,
						xmlDocumentContext
				) );
			} );
		}
		else {
			assert CollectionHelper.isEmpty( jaxbMapKeyOverrides );
			jaxbElementOverrides.forEach( (jaxbOverride) -> {
				overrideUsages.add( createAttributeOverrideUsage(
						jaxbOverride,
						null,
						memberDetails,
						xmlDocumentContext
				) );
			} );
		}
	}

	private static MutableAnnotationUsage<AttributeOverride> createAttributeOverrideUsage(
			JaxbAttributeOverrideImpl jaxbOverride,
			String namePrefix,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		final SourceModelBuildingContext modelBuildingContext = xmlDocumentContext.getModelBuildingContext();

		final MutableAnnotationUsage<AttributeOverride> overrideUsage = JpaAnnotations.ATTRIBUTE_OVERRIDE.createUsage( modelBuildingContext );

		final String name = StringHelper.qualifyConditionally( namePrefix, jaxbOverride.getName() );
		overrideUsage.setAttributeValue( "name", name );

		final MutableAnnotationUsage<Column> columnAnn = JpaAnnotations.COLUMN.createUsage( modelBuildingContext );
		overrideUsage.setAttributeValue( "column", columnAnn );
		ColumnProcessing.applyColumnDetails( jaxbOverride.getColumn(), target, columnAnn, xmlDocumentContext );

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
		if ( CollectionHelper.isEmpty( jaxbOverrides ) ) {
			return;
		}

		final MutableAnnotationUsage<AttributeOverrides> overridesUsage = target.replaceAnnotationUsage(
				JpaAnnotations.ATTRIBUTE_OVERRIDE,
				JpaAnnotations.ATTRIBUTE_OVERRIDES,
				xmlDocumentContext.getModelBuildingContext()
		);
		final ArrayList<MutableAnnotationUsage<AttributeOverride>> overrideUsages = arrayList( jaxbOverrides.size() );
		overridesUsage.setAttributeValue( "value", overrideUsages );

		jaxbOverrides.forEach( (jaxbOverride) -> {
			overrideUsages.add( createAttributeOverrideUsage(
					jaxbOverride,
					namePrefix,
					target,
					xmlDocumentContext
			) );
		} );
	}

	public static void applyAssociationOverrides(
			List<JaxbAssociationOverrideImpl> jaxbOverrides,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbOverrides ) ) {
			return;
		}

		final MutableAnnotationUsage<AssociationOverrides> overridesUsage = target.replaceAnnotationUsage(
				JpaAnnotations.ASSOCIATION_OVERRIDE,
				JpaAnnotations.ASSOCIATION_OVERRIDES,
				xmlDocumentContext.getModelBuildingContext()
		);
		final ArrayList<MutableAnnotationUsage<AssociationOverride>> overrideUsages = arrayList( jaxbOverrides.size() );
		overridesUsage.setAttributeValue( "value", overrideUsages );

		jaxbOverrides.forEach( (jaxbOverride) -> {
			final MutableAnnotationUsage<AssociationOverride> overrideUsage =
					JpaAnnotations.ASSOCIATION_OVERRIDE.createUsage( xmlDocumentContext.getModelBuildingContext() );
			transferAssociationOverride( jaxbOverride, overrideUsage, target, xmlDocumentContext );
			overrideUsages.add( overrideUsage );
		} );
	}
	
	private static void transferAssociationOverride(
			JaxbAssociationOverrideImpl jaxbOverride,
			MutableAnnotationUsage<AssociationOverride> overrideUsage,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		overrideUsage.setAttributeValue( "name", jaxbOverride.getName() );

		final List<JaxbJoinColumnImpl> joinColumns = jaxbOverride.getJoinColumns();
		if ( CollectionHelper.isNotEmpty( joinColumns ) ) {
			overrideUsage.setAttributeValue( 
					"joinColumns",
					JoinColumnProcessing.transformJoinColumnList( joinColumns, target, xmlDocumentContext )
			);
		}
		if ( jaxbOverride.getJoinTable() != null ) {
			overrideUsage.setAttributeValue(
					"joinTable",
					TableProcessing.transformJoinTable( jaxbOverride.getJoinTable(), target, xmlDocumentContext )
			);
		}
		if ( jaxbOverride.getForeignKeys() != null ) {
			overrideUsage.setAttributeValue(
					"foreignKey",
					ForeignKeyProcessing.createNestedForeignKeyAnnotation( jaxbOverride.getForeignKeys(), target, xmlDocumentContext )
			);
		}
		
	}

	public static void applyConverts(
			List<JaxbConvertImpl> jaxbConverts,
			String namePrefix,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbConverts ) ) {
			return;
		}

		final MutableAnnotationUsage<Converts> convertsUsage = memberDetails.replaceAnnotationUsage(
				JpaAnnotations.CONVERT,
				JpaAnnotations.CONVERTS,
				xmlDocumentContext.getModelBuildingContext()
		);
		final ArrayList<MutableAnnotationUsage<Convert>> convertUsages = arrayList( jaxbConverts.size() );
		convertsUsage.setAttributeValue( "value", convertUsages );

		for ( JaxbConvertImpl jaxbConvert : jaxbConverts ) {
			final MutableAnnotationUsage<Convert> convertUsage = JpaAnnotations.CONVERT.createUsage( xmlDocumentContext.getModelBuildingContext() );
			convertUsages.add( convertUsage );

			final ClassDetailsRegistry classDetailsRegistry = xmlDocumentContext.getModelBuildingContext().getClassDetailsRegistry();
			final ClassDetails converter;
			if ( StringHelper.isNotEmpty( jaxbConvert.getConverter() ) ) {
				converter = classDetailsRegistry.resolveClassDetails( jaxbConvert.getConverter() );
				convertUsage.setAttributeValue( "converter", converter );
			}

			XmlProcessingHelper.applyAttributeIfSpecified(
					"attributeName",
					prefixIfNotAlready( jaxbConvert.getAttributeName(), namePrefix ),
					convertUsage
			);
			XmlProcessingHelper.applyAttributeIfSpecified( "disableConversion", jaxbConvert.isDisableConversion(), convertUsage );
		}
	}

	public static void applyConvert(
			JaxbConvertImpl jaxbConvert,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyConvert( jaxbConvert, memberDetails, null, xmlDocumentContext );
	}

	public static void applyConvert(
			JaxbConvertImpl jaxbConvert,
			MutableMemberDetails memberDetails,
			String namePrefix,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbConvert == null ) {
			return;
		}

		final MutableAnnotationUsage<Convert> convertUsage = memberDetails.applyAnnotationUsage(
				JpaAnnotations.CONVERT,
				xmlDocumentContext.getModelBuildingContext()
		);

		final ClassDetailsRegistry classDetailsRegistry = xmlDocumentContext.getModelBuildingContext().getClassDetailsRegistry();
		final ClassDetails converter;
		if ( StringHelper.isNotEmpty( jaxbConvert.getConverter() ) ) {
			converter = classDetailsRegistry.resolveClassDetails( jaxbConvert.getConverter() );
			convertUsage.setAttributeValue( "converter", converter );
		}

		XmlProcessingHelper.applyAttributeIfSpecified(
				"attributeName",
				prefixIfNotAlready( jaxbConvert.getAttributeName(), namePrefix ),
				convertUsage
		);
		XmlProcessingHelper.applyAttributeIfSpecified( "disableConversion", jaxbConvert.isDisableConversion(), convertUsage );
	}

	public static void applyTable(
			JaxbTableImpl jaxbTable,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbTable == null ) {
			final XmlDocument.Defaults defaults = xmlDocumentContext.getXmlDocument().getDefaults();
			final String catalog = defaults.getCatalog();
			final String schema = defaults.getSchema();
			if ( StringHelper.isNotEmpty( catalog ) || StringHelper.isNotEmpty( schema ) ) {
				final MutableAnnotationUsage<Table> tableAnn = target.applyAnnotationUsage(
						JpaAnnotations.TABLE,
						xmlDocumentContext.getModelBuildingContext()
				);
				if ( StringHelper.isNotEmpty( catalog ) ) {
					tableAnn.setAttributeValue( "catalog", catalog );

				}
				if ( StringHelper.isNotEmpty( schema ) ) {
					tableAnn.setAttributeValue( "schema", schema );
				}
			}
		}
		else {
			final MutableAnnotationUsage<Table> tableAnn = target.applyAnnotationUsage(
					JpaAnnotations.TABLE,
					xmlDocumentContext.getModelBuildingContext()
			);
			applyOr( jaxbTable, JaxbTableImpl::getName, "name", tableAnn, JpaAnnotations.TABLE );
			applyTableAttributes( jaxbTable, target, tableAnn, JpaAnnotations.TABLE, xmlDocumentContext );
		}
	}

	public static <A extends Annotation> void applyTableAttributes(
			JaxbTableMapping jaxbTable,
			AnnotationTarget target,
			MutableAnnotationUsage<A> tableAnn,
			AnnotationDescriptor<A> annotationDescriptor,
			XmlDocumentContext xmlDocumentContext) {
		applyOrCatalog( jaxbTable, tableAnn, annotationDescriptor, xmlDocumentContext );
		applyOrSchema( jaxbTable, tableAnn, annotationDescriptor, xmlDocumentContext);
		applyOr( jaxbTable, JaxbTableMapping::getOptions, "options", tableAnn, annotationDescriptor );
		applyOr( jaxbTable, JaxbTableMapping::getComment, "comment", tableAnn, annotationDescriptor );
		applyCheckConstraints( jaxbTable, target, tableAnn, xmlDocumentContext );
		applyUniqueConstraints( jaxbTable.getUniqueConstraints(), target, tableAnn, xmlDocumentContext );
		applyIndexes( jaxbTable.getIndexes(), target, tableAnn, xmlDocumentContext );
	}

	public static void applyNaturalIdCache(
			JaxbNaturalId jaxbNaturalId,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbNaturalId == null || jaxbNaturalId.getCaching() == null ) {
			return;
		}

		final MutableAnnotationUsage<NaturalIdCache> naturalIdCacheUsage = classDetails.applyAnnotationUsage(
				HibernateAnnotations.NATURAL_ID_CACHE,
				xmlDocumentContext.getModelBuildingContext()
		);

		final JaxbCachingImpl jaxbCaching = jaxbNaturalId.getCaching();
		XmlProcessingHelper.applyAttributeIfSpecified( "region", jaxbCaching.getRegion(), naturalIdCacheUsage );
	}

	static void applyInheritance(
			JaxbEntity jaxbEntity,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbEntity.getInheritance() == null ) {
			return;
		}

		final MutableAnnotationUsage<Inheritance> inheritanceUsage = classDetails.applyAnnotationUsage(
				JpaAnnotations.INHERITANCE,
				xmlDocumentContext.getModelBuildingContext()
		);
		XmlProcessingHelper.applyAttributeIfSpecified( "strategy", jaxbEntity.getInheritance().getStrategy(), inheritanceUsage );
	}

	public static ClassDetails resolveJavaType(String value, XmlDocumentContext xmlDocumentContext) {
		return resolveJavaType(
				xmlDocumentContext.getXmlDocument().getDefaults().getPackage(),
				value,
				xmlDocumentContext.getModelBuildingContext().getClassDetailsRegistry()
		);
	}

	public static ClassDetails resolveJavaType(String value, ClassDetailsRegistry classDetailsRegistry) {
		return resolveJavaType( null, value, classDetailsRegistry );
	}

	public static ClassDetails resolveJavaType(String packageName, String name, ClassDetailsRegistry classDetailsRegistry) {
		if ( StringHelper.isEmpty( name ) ) {
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
		else if ( StringHelper.isNotEmpty( jaxbBasicMapping.getTarget() ) ) {
			applyTargetClass( jaxbBasicMapping.getTarget(), memberDetails, xmlDocumentContext );
		}

		if ( StringHelper.isNotEmpty( jaxbBasicMapping.getJdbcType() ) ) {
			applyJdbcTypeDescriptor( jaxbBasicMapping.getJdbcType(), memberDetails, xmlDocumentContext );
		}
		else if ( jaxbBasicMapping.getJdbcTypeCode() != null ) {
			applyJdbcTypeCode( jaxbBasicMapping.getJdbcTypeCode(), memberDetails, xmlDocumentContext );
		}
		else if ( StringHelper.isNotEmpty( jaxbBasicMapping.getJdbcTypeName() ) ) {
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
		final MutableAnnotationUsage<JavaType> typeAnn = memberDetails.applyAnnotationUsage(
				HibernateAnnotations.JAVA_TYPE,
				xmlDocumentContext.getModelBuildingContext()
		);

		final ClassDetails descriptorClass = xmlDocumentContext
				.getModelBuildingContext()
				.getClassDetailsRegistry()
				.resolveClassDetails( descriptorClassName );
		typeAnn.setAttributeValue( "value", descriptorClass );
	}


	private static void applyJdbcTypeDescriptor(
			String descriptorClassName,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final ClassDetails descriptorClassDetails = xmlDocumentContext
				.getModelBuildingContext()
				.getClassDetailsRegistry()
				.resolveClassDetails( descriptorClassName );
		final MutableAnnotationUsage<JdbcType> jdbcTypeAnn = memberDetails.applyAnnotationUsage(
				HibernateAnnotations.JDBC_TYPE,
				xmlDocumentContext.getModelBuildingContext()
		);
		jdbcTypeAnn.setAttributeValue( "value", descriptorClassDetails );

	}

	public static void applyJdbcTypeCode(
			Integer jdbcTypeCode,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jdbcTypeCode == null ) {
			return;
		}

		final MutableAnnotationUsage<JdbcTypeCode> typeCodeAnn = memberDetails.applyAnnotationUsage(
				HibernateAnnotations.JDBC_TYPE_CODE,
				xmlDocumentContext.getModelBuildingContext()
		);
		typeCodeAnn.setAttributeValue( "value", jdbcTypeCode );
	}

	public static void applyFilters(
			List<JaxbHbmFilterImpl> filters,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		applyFilters( filters, target, HibernateAnnotations.FILTER, xmlDocumentContext );
	}

	public static void applyJoinTableFilters(
			List<JaxbHbmFilterImpl> filters,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		applyFilters( filters, target, HibernateAnnotations.FILTER_JOIN_TABLE, xmlDocumentContext );
	}

	public static <F extends Annotation> void applyFilters(
			List<JaxbHbmFilterImpl> filters,
			MutableAnnotationTarget target,
			AnnotationDescriptor<F> filterAnnotationDescriptor,
			XmlDocumentContext xmlDocumentContext) {
		assert filterAnnotationDescriptor.getRepeatableContainer() != null;

		if ( filters.isEmpty() ) {
			return;
		}

		// replaces existing
		final MutableAnnotationUsage<?> containerUsage = target.replaceAnnotationUsage(
				filterAnnotationDescriptor,
				filterAnnotationDescriptor.getRepeatableContainer(),
				xmlDocumentContext.getModelBuildingContext()
		);

		final ArrayList<Object> filterUsages = arrayList( filters.size() );
		containerUsage.setAttributeValue( "value", filterUsages );

		filters.forEach( (jaxbFilter) -> {
			final MutableAnnotationUsage<F> filterUsage = filterAnnotationDescriptor.createUsage( xmlDocumentContext.getModelBuildingContext() );
			filterUsages.add( filterUsage );

			filterUsage.setAttributeValue( "name", jaxbFilter.getName() );
			XmlProcessingHelper.applyAttributeIfSpecified( "condition", jaxbFilter.getCondition(), filterUsage );
			XmlProcessingHelper.applyAttributeIfSpecified( "deduceAliasInjectionPoints", jaxbFilter.isAutoAliasInjection(), filterUsage );

			final List<JaxbHbmFilterImpl.JaxbAliasesImpl> aliases = jaxbFilter.getAliases();
			if ( !CollectionHelper.isEmpty( aliases ) ) {
				filterUsage.setAttributeValue( "aliases", getSqlFragmentAliases( aliases, target, xmlDocumentContext ) );
			}
		} );
	}

	private static List<AnnotationUsage<SqlFragmentAlias>> getSqlFragmentAliases(
			List<JaxbHbmFilterImpl.JaxbAliasesImpl> aliases,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		final List<AnnotationUsage<SqlFragmentAlias>> sqlFragmentAliases = new ArrayList<>( aliases.size() );
		for ( JaxbHbmFilterImpl.JaxbAliasesImpl alias : aliases ) {
			final MutableAnnotationUsage<SqlFragmentAlias> aliasAnn =
					HibernateAnnotations.SQL_FRAGMENT_ALIAS.createUsage( xmlDocumentContext.getModelBuildingContext() );

			aliasAnn.setAttributeValue( "alias", alias.getAlias() );
			XmlProcessingHelper.applyAttributeIfSpecified( "table", alias.getTable(), aliasAnn );
			if ( StringHelper.isNotEmpty( alias.getEntity() ) ) {
				aliasAnn.setAttributeValue(
						"entity",
						xmlDocumentContext.getModelBuildingContext().getClassDetailsRegistry().resolveClassDetails( alias.getEntity() )
				);
			}
			sqlFragmentAliases.add( aliasAnn );
		}
		return sqlFragmentAliases;
	}

	public static void applySqlRestriction(
			String sqlRestriction,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		applySqlRestriction( sqlRestriction, target, HibernateAnnotations.SQL_RESTRICTION, xmlDocumentContext );
	}

	public static void applySqlJoinTableRestriction(
			String sqlJoinTableRestriction,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		applySqlRestriction( sqlJoinTableRestriction, target, HibernateAnnotations.SQL_RESTRICTION_JOIN_TABLE, xmlDocumentContext );
	}

	private static <A extends Annotation> void applySqlRestriction(
			String sqlRestriction,
			MutableAnnotationTarget target,
			AnnotationDescriptor<A> annotationDescriptor,
			XmlDocumentContext xmlDocumentContext) {
		if ( StringHelper.isNotEmpty( sqlRestriction ) ) {
			final MutableAnnotationUsage<A> restrictionUsage = target.applyAnnotationUsage(
					annotationDescriptor,
					xmlDocumentContext.getModelBuildingContext()
			);
			restrictionUsage.setAttributeValue( "value", sqlRestriction );
		}
	}

	public static <A extends Annotation> void applyCustomSql(
			JaxbCustomSqlImpl jaxbCustomSql,
			MutableAnnotationTarget target,
			AnnotationDescriptor<A> annotationDescriptor,
			XmlDocumentContext xmlDocumentContext) {
		// todo (7.0) : account for secondary-table custom SQL
		if ( jaxbCustomSql == null ) {
			return;
		}

		final MutableAnnotationUsage<A> annotationUsage = target.applyAnnotationUsage(
				annotationDescriptor,
				xmlDocumentContext.getModelBuildingContext()
		);
		annotationUsage.setAttributeValue( "sql", jaxbCustomSql.getValue() );
		annotationUsage.setAttributeValue( "callable", jaxbCustomSql.isCallable() );
		XmlProcessingHelper.applyAttributeIfSpecified( "table", jaxbCustomSql.getTable(), annotationUsage );
		if ( jaxbCustomSql.getResultCheck() != null ) {
			annotationUsage.setAttributeValue( "check", getResultCheckStyle( jaxbCustomSql.getResultCheck() ) );
		}
	}

	@SuppressWarnings({ "deprecation", "removal" })
	private static ResultCheckStyle getResultCheckStyle(ExecuteUpdateResultCheckStyle style) {
		switch ( style ) {
			case NONE:
				return ResultCheckStyle.NONE;
			case COUNT:
				return ResultCheckStyle.COUNT;
			case PARAM:
				return ResultCheckStyle.PARAM;
			default:
				return null;
		}
	}

	static void applyIdClass(
			JaxbIdClassImpl jaxbIdClass,
			MutableClassDetails target,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbIdClass == null ) {
			return;
		}

		final MutableAnnotationUsage<IdClass> idClassAnn = target.applyAnnotationUsage(
				JpaAnnotations.ID_CLASS,
				xmlDocumentContext.getModelBuildingContext()
		);

		final ClassDetails idClassImpl = xmlDocumentContext.getModelBuildingContext()
				.getClassDetailsRegistry()
				.resolveClassDetails( jaxbIdClass.getClazz() );
		idClassAnn.setAttributeValue( "value", idClassImpl );
	}

	public static void applyLifecycleCallbacks(
			JaxbEntityOrMappedSuperclass jaxbClass,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		final SourceModelBuildingContext modelBuildingContext = xmlDocumentContext.getModelBuildingContext();

		if ( jaxbClass.getExcludeDefaultListeners() != null ) {
			classDetails.applyAnnotationUsage( JpaAnnotations.EXCLUDE_DEFAULT_LISTENERS, modelBuildingContext );
		}

		if ( jaxbClass.getExcludeSuperclassListeners() != null ) {
			classDetails.applyAnnotationUsage( JpaAnnotations.EXCLUDE_SUPERCLASS_LISTENERS, modelBuildingContext );
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

		final MutableAnnotationUsage<EntityListeners> listenersUsage = classDetails.replaceAnnotationUsage(
				JpaAnnotations.ENTITY_LISTENERS,
				xmlDocumentContext.getModelBuildingContext()
		);
		final List<ClassDetails> values = arrayList( entityListenerContainer.getEntityListeners().size() );
		listenersUsage.setAttributeValue( "value", values );

		entityListenerContainer.getEntityListeners().forEach( (jaxbEntityListener) -> {
			final MutableClassDetails entityListenerClass = xmlDocumentContext.resolveJavaType( jaxbEntityListener.getClazz() );
			applyLifecycleCallbacks(
					jaxbEntityListener,
					JpaEventListenerStyle.LISTENER,
					entityListenerClass,
					xmlDocumentContext
			);
			values.add( entityListenerClass );
		} );

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
			if ( method.getName().equals( name ) && JpaEventListener.matchesSignature( callbackType, method ) ) {
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

		final MutableAnnotationUsage<RowId> rowIdAnn = target.applyAnnotationUsage(
				HibernateAnnotations.ROW_ID,
				xmlDocumentContext.getModelBuildingContext()
		);
		XmlProcessingHelper.applyAttributeIfSpecified( "value", rowId, rowIdAnn );
	}

	private static String prefixIfNotAlready(String value, String prefix) {
		if ( StringHelper.isNotEmpty( prefix ) ) {
			final String previous = StringHelper.unqualify( value );
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
		if ( StringHelper.isEmpty( discriminatorValue ) ) {
			return;
		}

		final MutableAnnotationUsage<DiscriminatorValue> valueAnn = target.applyAnnotationUsage(
				JpaAnnotations.DISCRIMINATOR_VALUE,
				xmlDocumentContext.getModelBuildingContext()
		);
		valueAnn.setAttributeValue( "value", discriminatorValue );
	}

	static void applyDiscriminatorColumn(
			JaxbDiscriminatorColumnImpl jaxbDiscriminatorColumn,
			MutableClassDetails target,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbDiscriminatorColumn == null ) {
			return;
		}

		final MutableAnnotationUsage<DiscriminatorColumn> discriminatorColumnAnn = target.applyAnnotationUsage(
				JpaAnnotations.DISCRIMINATOR_COLUMN,
				xmlDocumentContext.getModelBuildingContext()
		);

		AnnotationUsageHelper.applyStringAttributeIfSpecified(
				"name",
				jaxbDiscriminatorColumn.getName(),
				discriminatorColumnAnn
		);
		AnnotationUsageHelper.applyAttributeIfSpecified(
				"discriminatorType",
				jaxbDiscriminatorColumn.getDiscriminatorType(),
				discriminatorColumnAnn
		);

		applyOr(
				jaxbDiscriminatorColumn,
				JaxbDiscriminatorColumnImpl::getColumnDefinition,
				"columnDefinition",
				discriminatorColumnAnn,
				JpaAnnotations.DISCRIMINATOR_COLUMN
		);
		applyOr(
				jaxbDiscriminatorColumn,
				JaxbDiscriminatorColumnImpl::getOptions,
				"options",
				discriminatorColumnAnn,
				JpaAnnotations.DISCRIMINATOR_COLUMN
		);
		applyOr(
				jaxbDiscriminatorColumn,
				JaxbDiscriminatorColumnImpl::getLength,
				"length",
				discriminatorColumnAnn,
				JpaAnnotations.DISCRIMINATOR_COLUMN
		);

		if ( jaxbDiscriminatorColumn.isForceSelection() || jaxbDiscriminatorColumn.isInsertable() == FALSE ) {
			final MutableAnnotationUsage<DiscriminatorOptions> optionsAnn = target.applyAnnotationUsage(
					HibernateAnnotations.DISCRIMINATOR_OPTIONS,
					xmlDocumentContext.getModelBuildingContext()
			);
			optionsAnn.setAttributeValue( "force", true );

			AnnotationUsageHelper.applyAttributeIfSpecified(
					"insert",
					jaxbDiscriminatorColumn.isInsertable(),
					discriminatorColumnAnn
			);
		}
	}

	public static void applyDiscriminatorFormula(
			@Nullable JaxbDiscriminatorFormulaImpl jaxbDiscriminatorFormula,
			MutableClassDetails target,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbDiscriminatorFormula == null ) {
			return;
		}
		if ( StringHelper.isEmpty( jaxbDiscriminatorFormula.getFragment() ) ) {
			return;
		}

		final MutableAnnotationUsage<DiscriminatorFormula> discriminatorFormulaAnn = target.applyAnnotationUsage(
				HibernateAnnotations.DISCRIMINATOR_FORMULA,
				xmlDocumentContext.getModelBuildingContext()
		);

		discriminatorFormulaAnn.setAttributeValue( "value", jaxbDiscriminatorFormula.getFragment() );
		XmlProcessingHelper.applyAttributeIfSpecified( "discriminatorType", jaxbDiscriminatorFormula.getDiscriminatorType(), discriminatorFormulaAnn );


		if ( jaxbDiscriminatorFormula.isForceSelection() ) {
			final MutableAnnotationUsage<DiscriminatorOptions> optionsAnn = target.applyAnnotationUsage(
					HibernateAnnotations.DISCRIMINATOR_OPTIONS,
					xmlDocumentContext.getModelBuildingContext()
			);
			optionsAnn.setAttributeValue( "force", true );
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

	public static <T, N, A extends Annotation> void applyOrSchema(
			JaxbSchemaAware jaxbNode,
			MutableAnnotationUsage<A> annotationUsage,
			AnnotationDescriptor<A> annotationDescriptor,
			XmlDocumentContext xmlDocumentContext) {
		applyOr(
				jaxbNode,
				(table) -> {
					if ( StringHelper.isNotEmpty( table.getSchema() ) ) {
						return table.getSchema();
					}
					else if ( StringHelper.isNotEmpty( defaultSchema( xmlDocumentContext ) ) ) {
						return defaultSchema( xmlDocumentContext );
					}
					return null;
				},
				"schema",
				annotationUsage,
				annotationDescriptor
		);
	}

	private static String defaultSchema(XmlDocumentContext xmlDocumentContext) {
		return xmlDocumentContext.getXmlDocument()
				.getDefaults()
				.getSchema();
	}

	public static <T, N, A extends Annotation> void applyOrCatalog(
			JaxbSchemaAware jaxbNode,
			MutableAnnotationUsage<A> annotationUsage,
			AnnotationDescriptor<A> annotationDescriptor,
			XmlDocumentContext xmlDocumentContext) {
		applyOr(
				jaxbNode,
				(table) -> {
					if ( StringHelper.isNotEmpty( table.getCatalog() ) ) {
						return table.getCatalog();
					}
					else if ( StringHelper.isNotEmpty( defaultCatalog( xmlDocumentContext ) ) ) {
						return defaultCatalog( xmlDocumentContext );
					}
					return null;
				},
				"catalog",
				annotationUsage,
				annotationDescriptor
		);
	}

	private static String defaultCatalog(XmlDocumentContext xmlDocumentContext) {
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
		if ( notFoundAction == null || jaxbNode.getNotFound() != NotFoundAction.EXCEPTION ) {
			return;
		}

		final MutableAnnotationUsage<NotFound> notFoundAnn = memberDetails.applyAnnotationUsage(
				HibernateAnnotations.NOT_FOUND,
				xmlDocumentContext.getModelBuildingContext()
		);
		notFoundAnn.setAttributeValue( "action", notFoundAction );
	}
}
