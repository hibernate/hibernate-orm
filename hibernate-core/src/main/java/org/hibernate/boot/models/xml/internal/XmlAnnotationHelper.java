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
import org.hibernate.annotations.AttributeAccessor;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.CollectionType;
import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.annotations.DiscriminatorOptions;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterJoinTable;
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.RowId;
import org.hibernate.annotations.SQLJoinTableRestriction;
import org.hibernate.annotations.SQLRestriction;
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
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntity;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbGeneratedValueImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbHbmFilterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIdClassImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIndexImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJoinColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbLifecycleCallback;
import org.hibernate.boot.jaxb.mapping.spi.JaxbLifecycleCallbackContainer;
import org.hibernate.boot.jaxb.mapping.spi.JaxbLobImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNationalizedImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNaturalId;
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
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.internal.util.KeyedConsumer;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.AnnotationDescriptor;
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

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;

import static java.lang.Boolean.FALSE;
import static java.util.Collections.emptyList;
import static org.hibernate.boot.models.xml.internal.XmlProcessingHelper.makeNestedAnnotation;

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

	public static <S> void applyOptionalAttribute(
			MutableAnnotationUsage<? extends Annotation> annotationUsage,
			String attributeName,
			S valueSource,
			Function<S,Object> valueExtractor) {
		if ( valueSource == null ) {
			return;
		}

		final Object value = valueExtractor.apply( valueSource );
		if ( value == null ) {
			return;
		}

		annotationUsage.setAttributeValue( attributeName, value );
	}

	public static <S> void applyOptionalStringAttribute(
			MutableAnnotationUsage<? extends Annotation> annotationUsage,
			String attributeName,
			S valueSource,
			Function<S,String> valueExtractor) {
		if ( valueSource == null ) {
			return;
		}

		final String value = valueExtractor.apply( valueSource );
		if ( StringHelper.isEmpty( value ) ) {
			return;
		}

		annotationUsage.setAttributeValue( attributeName, value );
	}

	/**
	 * Handle creating {@linkplain Entity @Entity} from an {@code <entity/>} element.
	 * Used in both complete and override modes.
	 */
	public static void applyEntity(
			JaxbEntity jaxbEntity,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<Entity> entityUsage = XmlProcessingHelper.getOrMakeAnnotation( Entity.class, classDetails, xmlDocumentContext );
		XmlProcessingHelper.applyAttributeIfSpecified( "name", jaxbEntity.getName(), entityUsage );
	}

	public static MutableAnnotationUsage<Access> createAccessAnnotation(
			AccessType accessType,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<Access> annotationUsage = XmlProcessingHelper.makeAnnotation( Access.class, target, xmlDocumentContext );
		annotationUsage.setAttributeValue( "value", accessType );
		return annotationUsage;
	}

	public static void applyAttributeAccessor(
			String attributeAccessor,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<AttributeAccessor> accessorAnn = XmlProcessingHelper.makeAnnotation( AttributeAccessor.class, memberDetails, xmlDocumentContext );
		memberDetails.addAnnotationUsage( accessorAnn );
		// todo : this is the old, deprecated form
		XmlProcessingHelper.applyAttributeIfSpecified( "value", attributeAccessor, accessorAnn );
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

	public static MutableAnnotationUsage<JoinColumn> applyJoinColumn(
			JaxbJoinColumnImpl jaxbJoinColumn,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbJoinColumn == null ) {
			return null;
		}

		return JoinColumnProcessing.createJoinColumnAnnotation( jaxbJoinColumn, memberDetails, JoinColumn.class, xmlDocumentContext );
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

	private static MutableAnnotationUsage<Column> createColumnAnnotation(
			JaxbColumnImpl jaxbColumn,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<Column> columnAnn = XmlProcessingHelper.getOrMakeAnnotation( Column.class, target, xmlDocumentContext );

		ColumnProcessing.applyColumnDetails( jaxbColumn, target, columnAnn, xmlDocumentContext );

		return columnAnn;
	}

	public static void applyUserType(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbType == null ) {
			return;
		}

		final MutableAnnotationUsage<Type> typeAnn = XmlProcessingHelper.makeAnnotation( Type.class, memberDetails, xmlDocumentContext );
		memberDetails.addAnnotationUsage( typeAnn );

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
			final MutableAnnotationUsage<Parameter> annotationUsage = makeNestedAnnotation( Parameter.class, target, xmlDocumentContext );
			parameterAnnList.add( annotationUsage );
			annotationUsage.setAttributeValue( "name", jaxbParam.getName() );
			annotationUsage.setAttributeValue( "value", jaxbParam.getValue() );
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

		final MutableAnnotationUsage<CollectionType> typeAnn = XmlProcessingHelper.makeAnnotation( CollectionType.class, memberDetails, xmlDocumentContext );
		memberDetails.addAnnotationUsage( typeAnn );

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

		final MutableAnnotationUsage<CollectionId> collectionIdAnn = XmlProcessingHelper.getOrMakeAnnotation( CollectionId.class, memberDetails, xmlDocumentContext );
		final AnnotationDescriptor<CollectionId> collectionIdDescriptor = xmlDocumentContext
				.getModelBuildingContext()
				.getAnnotationDescriptorRegistry()
				.getDescriptor( CollectionId.class );

		final JaxbColumnImpl jaxbColumn = jaxbCollectionId.getColumn();
		if ( jaxbColumn != null ) {
			collectionIdAnn.setAttributeValue( "column", createColumnAnnotation(
					jaxbColumn,
					memberDetails,
					xmlDocumentContext
			) );
		}

		applyOr( jaxbCollectionId, JaxbCollectionIdImpl::getGenerator, "generator", collectionIdAnn, collectionIdDescriptor );
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
			XmlProcessingHelper.getOrMakeAnnotation( Cascade.class, memberDetails, xmlDocumentContext )
					.setAttributeValue( "value", cascadeTypes );
		}
	}

	public static <A extends Annotation> void applyUniqueConstraints(
			List<JaxbUniqueConstraintImpl> jaxbUniqueConstraints,
			MutableAnnotationTarget target,
			MutableAnnotationUsage<A> annotationUsage,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbUniqueConstraints ) ) {
			return;
		}

		final List<AnnotationUsage<UniqueConstraint>> uniqueConstraintUsages = new ArrayList<>( jaxbUniqueConstraints.size() );
		annotationUsage.setAttributeValue( "uniqueConstraints", uniqueConstraintUsages );

		jaxbUniqueConstraints.forEach( (jaxbUniqueConstraint) -> {
			final MutableAnnotationUsage<UniqueConstraint> ucUsage = JpaAnnotations.UNIQUE_CONSTRAINT.createUsage(
					target,
					xmlDocumentContext.getModelBuildingContext()
			);
			XmlAnnotationHelper.applyOptionalAttribute( ucUsage, "name", jaxbUniqueConstraint.getName() );
			XmlAnnotationHelper.applyOptionalAttribute( ucUsage, "options", jaxbUniqueConstraint.getOptions() );
			ucUsage.setAttributeValue( "columnNames", jaxbUniqueConstraint.getColumnName() );
			uniqueConstraintUsages.add( ucUsage );
		} );
	}

	public static <A extends Annotation> void applyIndexes(
			List<JaxbIndexImpl> jaxbIndexes,
			MutableAnnotationTarget target,
			MutableAnnotationUsage<A> annotationUsage,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbIndexes ) ) {
			return;
		}

		final List<AnnotationUsage<Index>> indexes = new ArrayList<>( jaxbIndexes.size() );
		final AnnotationDescriptor<Index> indexDescriptor = xmlDocumentContext.getModelBuildingContext()
				.getAnnotationDescriptorRegistry()
				.getDescriptor( Index.class );
		jaxbIndexes.forEach( jaxbIndex -> {
			final MutableAnnotationUsage<Index> indexAnn = indexDescriptor.createUsage(
					target,
					xmlDocumentContext.getModelBuildingContext()
			);
			applyOr( jaxbIndex, JaxbIndexImpl::getName, "name", indexAnn, indexDescriptor );
			applyOr( jaxbIndex, JaxbIndexImpl::getColumnList, "columnList", indexAnn, indexDescriptor );
			applyOr( jaxbIndex, JaxbIndexImpl::isUnique, "unique", indexAnn, indexDescriptor );
			indexes.add( indexAnn );
		} );

		annotationUsage.setAttributeValue( "indexes", indexes );
	}

	public static <A extends Annotation> void applyCheckConstraints(
			JaxbCheckable jaxbCheckable,
			MutableAnnotationTarget target,
			MutableAnnotationUsage<A> annotationUsage,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbCheckable!= null && CollectionHelper.isNotEmpty( jaxbCheckable.getCheckConstraints() ) ) {
			final List<AnnotationUsage<CheckConstraint>> checks = new ArrayList<>( jaxbCheckable.getCheckConstraints().size() );
			final AnnotationDescriptor<CheckConstraint> checkConstraintDescriptor = xmlDocumentContext.getModelBuildingContext()
					.getAnnotationDescriptorRegistry()
					.getDescriptor( CheckConstraint.class );
			for ( JaxbCheckConstraintImpl jaxbCheck : jaxbCheckable.getCheckConstraints() ) {
				final MutableAnnotationUsage<CheckConstraint> checkAnn = XmlProcessingHelper.getOrMakeAnnotation( CheckConstraint.class, target, xmlDocumentContext );
				applyOr( jaxbCheck, JaxbCheckConstraintImpl::getName, "name", checkAnn, checkConstraintDescriptor );
				applyOr( jaxbCheck, JaxbCheckConstraintImpl::getConstraint, "constraint", checkAnn, checkConstraintDescriptor );
				applyOr( jaxbCheck, JaxbCheckConstraintImpl::getOptions, "options", checkAnn, checkConstraintDescriptor );
				checks.add( checkAnn );
			}
			annotationUsage.setAttributeValue( "check", checks );
		}
	}

	public static <A extends Annotation> void applyCheckConstraints(
			JaxbCheckable jaxbCheckable,
			MutableAnnotationUsage<A> annotationUsage,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isNotEmpty( jaxbCheckable.getCheckConstraints() ) ) {
			final List<AnnotationUsage<CheckConstraint>> checks = new ArrayList<>( jaxbCheckable.getCheckConstraints().size() );
			final AnnotationDescriptor<CheckConstraint> checkConstraintDescriptor = xmlDocumentContext.getModelBuildingContext()
					.getAnnotationDescriptorRegistry()
					.getDescriptor( CheckConstraint.class );
			for ( JaxbCheckConstraintImpl jaxbCheck : jaxbCheckable.getCheckConstraints() ) {
				final MutableAnnotationUsage<CheckConstraint> checkAnn = XmlProcessingHelper.getOrMakeAnnotation( CheckConstraint.class, xmlDocumentContext );
				applyOr( jaxbCheck, JaxbCheckConstraintImpl::getName, "name", checkAnn, checkConstraintDescriptor );
				applyOr( jaxbCheck, JaxbCheckConstraintImpl::getConstraint, "constraint", checkAnn, checkConstraintDescriptor );
				applyOr( jaxbCheck, JaxbCheckConstraintImpl::getOptions, "options", checkAnn, checkConstraintDescriptor );
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
		final MutableAnnotationUsage<Target> targetAnn = XmlProcessingHelper.makeAnnotation( Target.class, memberDetails, xmlDocumentContext );
		targetAnn.setAttributeValue( "value", classDetails );
	}

	public static void applyTemporal(
			TemporalType temporalType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( temporalType == null ) {
			return;
		}

		final MutableAnnotationUsage<Temporal> annotationUsage = XmlProcessingHelper.makeAnnotation( Temporal.class, memberDetails, xmlDocumentContext );
		annotationUsage.setAttributeValue( "value", temporalType );
	}

	public static void applyLob(JaxbLobImpl jaxbLob, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext) {
		if ( jaxbLob == null ) {
			return;
		}

		XmlProcessingHelper.makeAnnotation( Lob.class, memberDetails, xmlDocumentContext );
	}

	public static void applyEnumerated(EnumType enumType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext) {
		if ( enumType == null ) {
			return;
		}

		final MutableAnnotationUsage<Enumerated> annotationUsage = XmlProcessingHelper.makeAnnotation(
				Enumerated.class,
				memberDetails,
				xmlDocumentContext
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

		XmlProcessingHelper.makeAnnotation( Nationalized.class, memberDetails, xmlDocumentContext );
	}

	public static void applyGeneratedValue(
			JaxbGeneratedValueImpl jaxbGeneratedValue,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbGeneratedValue == null ) {
			return;
		}

		final MutableAnnotationUsage<GeneratedValue> generatedValueAnn = XmlProcessingHelper.makeAnnotation( GeneratedValue.class, memberDetails, xmlDocumentContext );
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

		final MutableAnnotationUsage<SequenceGenerator> sequenceAnn = XmlProcessingHelper.getOrMakeNamedAnnotation(
				SequenceGenerator.class,
				jaxbGenerator.getName(),
				memberDetails,
				xmlDocumentContext
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

		final MutableAnnotationUsage<TableGenerator> annotationUsage = XmlProcessingHelper.makeAnnotation( TableGenerator.class, memberDetails, xmlDocumentContext );
		memberDetails.addAnnotationUsage( annotationUsage );
		XmlProcessingHelper.applyAttributeIfSpecified( "name", jaxbGenerator.getName(), annotationUsage );
		XmlProcessingHelper.applyAttributeIfSpecified( "table", jaxbGenerator.getTable(), annotationUsage );
		XmlProcessingHelper.applyAttributeIfSpecified( "catalog", jaxbGenerator.getCatalog(), annotationUsage );
		XmlProcessingHelper.applyAttributeIfSpecified( "schema", jaxbGenerator.getSchema(), annotationUsage );
		XmlProcessingHelper.applyAttributeIfSpecified( "pkColumnName", jaxbGenerator.getPkColumnName(), annotationUsage );
		XmlProcessingHelper.applyAttributeIfSpecified( "valueColumnName", jaxbGenerator.getValueColumnName(), annotationUsage );
		XmlProcessingHelper.applyAttributeIfSpecified( "pkColumnValue", jaxbGenerator.getPkColumnValue(), annotationUsage );
		XmlProcessingHelper.applyAttributeIfSpecified( "initialValue", jaxbGenerator.getInitialValue(), annotationUsage );
		XmlProcessingHelper.applyAttributeIfSpecified( "allocationSize", jaxbGenerator.getInitialValue(), annotationUsage );
		applyUniqueConstraints( jaxbGenerator.getUniqueConstraints(), memberDetails, annotationUsage, xmlDocumentContext );
		applyIndexes( jaxbGenerator.getIndexes(), memberDetails, annotationUsage, xmlDocumentContext );
	}

	public static void applyUuidGenerator(
			JaxbUuidGeneratorImpl jaxbGenerator,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbGenerator == null ) {
			return;
		}

		final MutableAnnotationUsage<UuidGenerator> annotationUsage = XmlProcessingHelper.makeAnnotation( UuidGenerator.class, memberDetails, xmlDocumentContext );
		memberDetails.addAnnotationUsage( annotationUsage );
		annotationUsage.setAttributeValue( "style", jaxbGenerator.getStyle() );
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

		final int numberOfOverrides = jaxbMapKeyOverrides.size() + jaxbElementOverrides.size();
		if ( numberOfOverrides == 1 ) {
			final MutableAnnotationUsage<AttributeOverride> overrideUsage;
			if ( memberDetails.getMapKeyType() != null ) {
				if ( jaxbMapKeyOverrides.size() == 1 ) {
					overrideUsage = createAttributeOverrideUsage(
							jaxbMapKeyOverrides.get( 0 ),
							"key",
							memberDetails,
							xmlDocumentContext
					);
				}
				else {
					assert jaxbElementOverrides.size() == 1;
					overrideUsage = createAttributeOverrideUsage(
							jaxbElementOverrides.get( 0 ),
							"value",
							memberDetails,
							xmlDocumentContext
					);
				}
			}
			else {
				assert jaxbElementOverrides.size() == 1;
				overrideUsage = createAttributeOverrideUsage(
						jaxbElementOverrides.get( 0 ),
						null,
						memberDetails,
						xmlDocumentContext
				);
			}

			memberDetails.addAnnotationUsage( overrideUsage );
			return;
		}

		final SourceModelBuildingContext modelBuildingContext = xmlDocumentContext.getModelBuildingContext();
		final MutableAnnotationUsage<AttributeOverrides> overridesUsage = JpaAnnotations.ATTRIBUTE_OVERRIDES.createUsage(
				memberDetails,
				modelBuildingContext
		);
		memberDetails.addAnnotationUsage( overridesUsage );

		final List<MutableAnnotationUsage<AttributeOverride>> overrideUsages = CollectionHelper.arrayList(
				numberOfOverrides
		);
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
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final SourceModelBuildingContext modelBuildingContext = xmlDocumentContext.getModelBuildingContext();

		final MutableAnnotationUsage<AttributeOverride> overrideUsage = JpaAnnotations.ATTRIBUTE_OVERRIDE.createUsage(
				memberDetails,
				modelBuildingContext
		);

		final String name = StringHelper.qualifyConditionally( namePrefix, jaxbOverride.getName() );
		overrideUsage.setAttributeValue( "name", name );

		final MutableAnnotationUsage<Column> columnAnn = JpaAnnotations.COLUMN.createUsage(
				memberDetails,
				modelBuildingContext
		);
		overrideUsage.setAttributeValue( "column", columnAnn );
		ColumnProcessing.applyColumnDetails( jaxbOverride.getColumn(), memberDetails, columnAnn, xmlDocumentContext );

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
			MutableMemberDetails memberDetails,
			String namePrefix,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbOverrides ) ) {
			return;
		}

		final MutableAnnotationUsage<AttributeOverrides> attributeOverridesAnn = XmlProcessingHelper.makeAnnotation(
				AttributeOverrides.class,
				memberDetails,
				xmlDocumentContext
		);
		memberDetails.addAnnotationUsage( attributeOverridesAnn );

		final ArrayList<MutableAnnotationUsage<AttributeOverride>> overrideUsages = CollectionHelper.arrayList( jaxbOverrides.size() );
		attributeOverridesAnn.setAttributeValue( "value", overrideUsages );

		jaxbOverrides.forEach( (jaxbOverride) -> {
			overrideUsages.add( createAttributeOverrideUsage(
					jaxbOverride,
					namePrefix,
					memberDetails,
					xmlDocumentContext
			) );
		} );
	}

	public static void applyAssociationOverrides(
			List<JaxbAssociationOverrideImpl> jaxbOverrides,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbOverrides ) ) {
			return;
		}

		if ( jaxbOverrides.size() == 1 ) {
			final MutableAnnotationUsage<AssociationOverride> overrideUsage = memberDetails.applyAnnotationUsage(
					JpaAnnotations.ASSOCIATION_OVERRIDE,
					xmlDocumentContext.getModelBuildingContext()
			);
			transferAssociationOverride(
					jaxbOverrides.get( 0 ),
					overrideUsage,
					memberDetails,
					xmlDocumentContext
			);
		}
		else {
			final MutableAnnotationUsage<AssociationOverrides> overridesUsage = memberDetails.applyAnnotationUsage(
					JpaAnnotations.ASSOCIATION_OVERRIDES,
					xmlDocumentContext.getModelBuildingContext()
			);
			final ArrayList<MutableAnnotationUsage<AssociationOverride>> overrideUsages = CollectionHelper.arrayList( jaxbOverrides.size() );
			overridesUsage.setAttributeValue( "value", overrideUsages );

			jaxbOverrides.forEach( (jaxbOverride) -> {
				final MutableAnnotationUsage<AssociationOverride> overrideUsage = JpaAnnotations.ASSOCIATION_OVERRIDE.createUsage(
						memberDetails,
						xmlDocumentContext.getModelBuildingContext()
				);
				transferAssociationOverride( jaxbOverride, overrideUsage, memberDetails, xmlDocumentContext );
				overrideUsages.add( overrideUsage );
			} );
		}
	}
	
	private static void transferAssociationOverride(
			JaxbAssociationOverrideImpl jaxbOverride,
			MutableAnnotationUsage<AssociationOverride> overrideUsage,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		overrideUsage.setAttributeValue( "name", jaxbOverride.getName() );

		final List<JaxbJoinColumnImpl> joinColumns = jaxbOverride.getJoinColumns();
		if ( CollectionHelper.isNotEmpty( joinColumns ) ) {
			overrideUsage.setAttributeValue( 
					"joinColumns",
					JoinColumnProcessing.transformJoinColumnList( joinColumns, memberDetails, xmlDocumentContext ) 
			);
		}
		if ( jaxbOverride.getJoinTable() != null ) {
			overrideUsage.setAttributeValue(
					"joinTable",
					TableProcessing.transformJoinTable( jaxbOverride.getJoinTable(), memberDetails, xmlDocumentContext )
			);
		}
		if ( jaxbOverride.getForeignKeys() != null ) {
			overrideUsage.setAttributeValue(
					"foreignKey",
					ForeignKeyProcessing.createNestedForeignKeyAnnotation( jaxbOverride.getForeignKeys(), memberDetails, xmlDocumentContext )
			);
		}
		
	}

	public static void applyOptimisticLockInclusion(
			boolean inclusion,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<OptimisticLock> annotationUsage = XmlProcessingHelper.makeAnnotation(
				OptimisticLock.class,
				memberDetails,
				xmlDocumentContext
		);
		memberDetails.addAnnotationUsage( annotationUsage );
		annotationUsage.setAttributeValue( "exclude", !inclusion );
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

		final MutableAnnotationUsage<Convert> annotationUsage = XmlProcessingHelper.makeAnnotation(
				Convert.class,
				memberDetails,
				xmlDocumentContext
		);
		memberDetails.addAnnotationUsage( annotationUsage );

		final ClassDetailsRegistry classDetailsRegistry = xmlDocumentContext.getModelBuildingContext().getClassDetailsRegistry();
		final ClassDetails converter;
		if ( StringHelper.isNotEmpty( jaxbConvert.getConverter() ) ) {
			converter = classDetailsRegistry.resolveClassDetails( jaxbConvert.getConverter() );
			annotationUsage.setAttributeValue( "converter", converter );
		}

		XmlProcessingHelper.applyAttributeIfSpecified(
				"attributeName",
				prefixIfNotAlready( jaxbConvert.getAttributeName(), namePrefix ),
				annotationUsage
		);
		XmlProcessingHelper.applyAttributeIfSpecified( "disableConversion", jaxbConvert.isDisableConversion(), annotationUsage );
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
				final MutableAnnotationUsage<Table> tableAnn = XmlProcessingHelper.makeAnnotation( Table.class, target, xmlDocumentContext );
				if ( StringHelper.isNotEmpty( catalog ) ) {
					tableAnn.setAttributeValue( "catalog", catalog );

				}
				if ( StringHelper.isNotEmpty( schema ) ) {
					tableAnn.setAttributeValue( "schema", schema );
				}
			}
		}
		else {
			final MutableAnnotationUsage<Table> tableAnn = XmlProcessingHelper.makeAnnotation( Table.class, target, xmlDocumentContext );
			final AnnotationDescriptor<Table> tableDescriptor = xmlDocumentContext.getModelBuildingContext()
					.getAnnotationDescriptorRegistry()
					.getDescriptor( Table.class );
			applyOr( jaxbTable, JaxbTableImpl::getName, "name", tableAnn, tableDescriptor );
			applyTableAttributes( jaxbTable, target, tableAnn, tableDescriptor, xmlDocumentContext );
		}
	}

	public static void applyTableOverride(
			JaxbTableImpl jaxbTable,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbTable == null ) {
			return;
		}

		final MutableAnnotationUsage<Table> tableAnn = XmlProcessingHelper.getOrMakeAnnotation( Table.class, target, xmlDocumentContext );
		final AnnotationDescriptor<Table> tableDescriptor = xmlDocumentContext.getModelBuildingContext()
				.getAnnotationDescriptorRegistry()
				.getDescriptor( Table.class );

		applyOr( jaxbTable, JaxbTableImpl::getName, "name", tableAnn, tableDescriptor );
		applyTableAttributes( jaxbTable, target, tableAnn, tableDescriptor, xmlDocumentContext );
	}

	public static <A extends Annotation> void applyTableAttributes(
			JaxbTableMapping jaxbTable,
			MutableAnnotationTarget target,
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

	public static void applyNaturalId(
			JaxbNaturalId jaxbNaturalId,
			MutableMemberDetails backingMember,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbNaturalId == null ) {
			return;
		}
		final MutableAnnotationUsage<NaturalId> annotationUsage = XmlProcessingHelper.makeAnnotation(
				NaturalId.class,
				backingMember,
				xmlDocumentContext
		);
		backingMember.addAnnotationUsage( annotationUsage );
		annotationUsage.setAttributeValue( "mutable", jaxbNaturalId.isMutable() );
	}

	public static void applyNaturalIdCache(
			JaxbNaturalId jaxbNaturalId,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbNaturalId == null || jaxbNaturalId.getCaching() == null ) {
			return;
		}

		final MutableAnnotationUsage<NaturalIdCache> annotationUsage = XmlProcessingHelper.makeAnnotation(
				NaturalIdCache.class,
				classDetails,
				xmlDocumentContext
		);
		classDetails.addAnnotationUsage( annotationUsage );

		final JaxbCachingImpl jaxbCaching = jaxbNaturalId.getCaching();
		XmlProcessingHelper.applyAttributeIfSpecified( "region", jaxbCaching.getRegion(), annotationUsage );
	}

	public static void applyId(
			JaxbIdImpl jaxbId,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbId == null ) {
			return;
		}
		final MutableAnnotationUsage<Id> annotationUsage = XmlProcessingHelper.makeAnnotation(
				Id.class,
				memberDetails,
				xmlDocumentContext
		);
		memberDetails.addAnnotationUsage( annotationUsage );
	}

	public static void applyEmbeddedId(
			JaxbEmbeddedIdImpl jaxbEmbeddedId,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbEmbeddedId == null ) {
			return;
		}
		final MutableAnnotationUsage<EmbeddedId> annotationUsage = XmlProcessingHelper.makeAnnotation(
				EmbeddedId.class,
				memberDetails,
				xmlDocumentContext
		);
		memberDetails.addAnnotationUsage( annotationUsage );
	}

	static void applyInheritance(
			JaxbEntity jaxbEntity,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbEntity.getInheritance() == null ) {
			return;
		}

		final MutableAnnotationUsage<Inheritance> inheritanceAnn = XmlProcessingHelper.getOrMakeAnnotation(
				Inheritance.class,
				classDetails,
				xmlDocumentContext
		);
		XmlProcessingHelper.applyAttributeIfSpecified( "strategy", jaxbEntity.getInheritance().getStrategy(), inheritanceAnn );
	}

	public static ClassDetails resolveJavaType(String value, XmlDocumentContext xmlDocumentContext) {
		return resolveJavaType(
				xmlDocumentContext.getXmlDocument().getDefaults().getPackage(),
				value,
				xmlDocumentContext.getModelBuildingContext().getClassDetailsRegistry()
		);
	}

	public static ClassDetails resolveJavaType(String value, SourceModelBuildingContext sourceModelBuildingContext) {
		return resolveJavaType( value, sourceModelBuildingContext.getClassDetailsRegistry() );
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
		final MutableAnnotationUsage<JavaType> typeAnn = XmlProcessingHelper.makeAnnotation( JavaType.class, memberDetails, xmlDocumentContext );
		memberDetails.addAnnotationUsage( typeAnn );

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
		final MutableAnnotationUsage<JdbcType> jdbcTypeAnn = XmlProcessingHelper.makeAnnotation( JdbcType.class, memberDetails, xmlDocumentContext );
		jdbcTypeAnn.setAttributeValue( "value", descriptorClassDetails );

	}

	public static void applyJdbcTypeCode(
			Integer jdbcTypeCode,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jdbcTypeCode == null ) {
			return;
		}

		final MutableAnnotationUsage<JdbcTypeCode> typeCodeAnn = XmlProcessingHelper.makeAnnotation( JdbcTypeCode.class, memberDetails, xmlDocumentContext );
		memberDetails.addAnnotationUsage( typeCodeAnn );
		typeCodeAnn.setAttributeValue( "value", jdbcTypeCode );
	}

	public static void applyFilter(
			JaxbHbmFilterImpl jaxbFilter,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		applyFilter( jaxbFilter, target, Filter.class, xmlDocumentContext );
	}

	public static void applyJoinTableFilter(
			JaxbHbmFilterImpl jaxbFilter,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		applyFilter( jaxbFilter, target, FilterJoinTable.class, xmlDocumentContext );
	}

	private static <F extends Annotation> void applyFilter(
			JaxbHbmFilterImpl jaxbFilter,
			MutableAnnotationTarget target,
			Class<F> filterAnnotationClass,
			XmlDocumentContext xmlDocumentContext) {
		// Since @Filter and @FilterJoinTable have exactly the same attributes,
		// we can use the same method with parametrized annotation class
		final MutableAnnotationUsage<F> filterAnn = XmlProcessingHelper.getOrMakeNamedAnnotation(
				filterAnnotationClass,
				jaxbFilter.getName(),
				target,
				xmlDocumentContext
		);

		XmlProcessingHelper.applyAttributeIfSpecified( "condition", jaxbFilter.getCondition(), filterAnn );
		XmlProcessingHelper.applyAttributeIfSpecified( "deduceAliasInjectionPoints", jaxbFilter.isAutoAliasInjection(), filterAnn );

		final List<JaxbHbmFilterImpl.JaxbAliasesImpl> aliases = jaxbFilter.getAliases();
		if ( !CollectionHelper.isEmpty( aliases ) ) {
			filterAnn.setAttributeValue( "aliases", getSqlFragmentAliases( aliases, target, xmlDocumentContext ) );
		}
	}

	private static List<AnnotationUsage<SqlFragmentAlias>> getSqlFragmentAliases(
			List<JaxbHbmFilterImpl.JaxbAliasesImpl> aliases,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		final List<AnnotationUsage<SqlFragmentAlias>> sqlFragmentAliases = new ArrayList<>( aliases.size() );
		for ( JaxbHbmFilterImpl.JaxbAliasesImpl alias : aliases ) {
			final MutableAnnotationUsage<SqlFragmentAlias> aliasAnn = makeNestedAnnotation( SqlFragmentAlias.class, target, xmlDocumentContext );
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
		applySqlRestriction( sqlRestriction, target, SQLRestriction.class, xmlDocumentContext );
	}

	public static void applySqlJoinTableRestriction(
			String sqlJoinTableRestriction,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		applySqlRestriction( sqlJoinTableRestriction, target, SQLJoinTableRestriction.class, xmlDocumentContext );
	}

	private static <A extends Annotation> void applySqlRestriction(
			String sqlRestriction,
			MutableAnnotationTarget target,
			Class<A> annotationType,
			XmlDocumentContext xmlDocumentContext) {
		if ( StringHelper.isNotEmpty( sqlRestriction ) ) {
			final MutableAnnotationUsage<A> annotation = XmlProcessingHelper.getOrMakeAnnotation( annotationType, target, xmlDocumentContext );
			annotation.setAttributeValue( "value", sqlRestriction );
		}
	}

	public static <A extends Annotation> void applyCustomSql(
			JaxbCustomSqlImpl jaxbCustomSql,
			MutableAnnotationTarget target,
			Class<A> annotationType,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbCustomSql != null ) {
			final MutableAnnotationUsage<A> annotation = XmlProcessingHelper.getOrMakeAnnotation( annotationType, target, xmlDocumentContext );
			annotation.setAttributeValue( "sql", jaxbCustomSql.getValue() );
			annotation.setAttributeValue( "callable", jaxbCustomSql.isCallable() );
			XmlProcessingHelper.applyAttributeIfSpecified( "table", jaxbCustomSql.getTable(), annotation );
			if ( jaxbCustomSql.getResultCheck() != null ) {
				annotation.setAttributeValue( "check", getResultCheckStyle( jaxbCustomSql.getResultCheck() ) );
			}
		}
	}

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
		if ( jaxbIdClass != null ) {
			XmlProcessingHelper.getOrMakeAnnotation( IdClass.class, target, xmlDocumentContext ).setAttributeValue(
					"value",
					xmlDocumentContext.getModelBuildingContext().getClassDetailsRegistry().resolveClassDetails( jaxbIdClass.getClazz() )
			);
		}
	}

	static void applyEntityListener(
			JaxbEntityListenerImpl jaxbEntityListener,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<EntityListeners> entityListeners = XmlProcessingHelper.getOrMakeAnnotation(
				EntityListeners.class,
				classDetails,
				xmlDocumentContext
		);
		final MutableClassDetails entityListenerClass = xmlDocumentContext.resolveJavaType( jaxbEntityListener.getClazz() );
		applyLifecycleCallbacks(
				jaxbEntityListener,
				JpaEventListenerStyle.LISTENER,
				entityListenerClass,
				xmlDocumentContext
		);
		final List<ClassDetails> values = entityListeners.getAttributeValue( "value" );
		if ( values != null ) {
			values.add( entityListenerClass );
		}
		else {
			entityListeners.setAttributeValue( "value", new ArrayList<>( List.of( entityListenerClass ) ) );
		}
	}
	
	static void applyLifecycleCallbacks(
			JaxbLifecycleCallbackContainer lifecycleCallbackContainer,
			JpaEventListenerStyle callbackType,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyLifecycleCallback( lifecycleCallbackContainer.getPrePersist(), callbackType, PrePersist.class, classDetails, xmlDocumentContext );
		applyLifecycleCallback( lifecycleCallbackContainer.getPostPersist(), callbackType, PostPersist.class, classDetails, xmlDocumentContext );
		applyLifecycleCallback( lifecycleCallbackContainer.getPreRemove(), callbackType, PreRemove.class, classDetails, xmlDocumentContext );
		applyLifecycleCallback( lifecycleCallbackContainer.getPostRemove(), callbackType, PostRemove.class, classDetails, xmlDocumentContext );
		applyLifecycleCallback( lifecycleCallbackContainer.getPreUpdate(), callbackType, PreUpdate.class, classDetails, xmlDocumentContext );
		applyLifecycleCallback( lifecycleCallbackContainer.getPostUpdate(), callbackType, PostUpdate.class, classDetails, xmlDocumentContext );
		applyLifecycleCallback( lifecycleCallbackContainer.getPostLoad(), callbackType, PostLoad.class, classDetails, xmlDocumentContext );
	}

	private static <A extends Annotation> void applyLifecycleCallback(
			JaxbLifecycleCallback lifecycleCallback,
			JpaEventListenerStyle callbackType,
			Class<A> lifecycleAnnotation,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( lifecycleCallback != null ) {
			final MethodDetails methodDetails = getCallbackMethodDetails(
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
			XmlProcessingHelper.makeAnnotation( lifecycleAnnotation, (MutableMemberDetails) methodDetails, xmlDocumentContext );
		}
	}

	private static MethodDetails getCallbackMethodDetails(
			String name,
			JpaEventListenerStyle callbackType,
			ClassDetails classDetails) {
		for ( MethodDetails method : classDetails.getMethods() ) {
			if ( method.getName().equals( name ) && JpaEventListener.matchesSignature( callbackType, method ) ) {
				return method;
			}
		}
		return null;
	}

	static void applyRowId(
			String rowId,
			MutableClassDetails target,
			XmlDocumentContext xmlDocumentContext) {
		if ( rowId != null ) {
			final MutableAnnotationUsage<RowId> rowIdAnn = XmlProcessingHelper.getOrMakeAnnotation( RowId.class, target, xmlDocumentContext );
			XmlProcessingHelper.applyAttributeIfSpecified( "value", rowId, rowIdAnn );
		}
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
		if ( discriminatorValue != null ) {
			final MutableAnnotationUsage<DiscriminatorValue> rowIdAnn = XmlProcessingHelper
					.getOrMakeAnnotation( DiscriminatorValue.class, target, xmlDocumentContext );
			XmlProcessingHelper.applyAttributeIfSpecified( "value", discriminatorValue, rowIdAnn );
		}
	}

	static void applyDiscriminatorColumn(
			JaxbDiscriminatorColumnImpl jaxbDiscriminatorColumn,
			MutableClassDetails target,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbDiscriminatorColumn == null ) {
			return;
		}

		final MutableAnnotationUsage<DiscriminatorColumn> discriminatorColumnAnn = XmlProcessingHelper
				.getOrMakeAnnotation( DiscriminatorColumn.class, target, xmlDocumentContext );
		final AnnotationDescriptor<DiscriminatorColumn> discriminatorColumnDescriptor = xmlDocumentContext
				.getModelBuildingContext()
				.getAnnotationDescriptorRegistry()
				.getDescriptor( DiscriminatorColumn.class );
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
				discriminatorColumnDescriptor
		);
		applyOr(
				jaxbDiscriminatorColumn,
				JaxbDiscriminatorColumnImpl::getOptions,
				"options",
				discriminatorColumnAnn,
				discriminatorColumnDescriptor
		);
		applyOr(
				jaxbDiscriminatorColumn,
				JaxbDiscriminatorColumnImpl::getLength,
				"length",
				discriminatorColumnAnn,
				discriminatorColumnDescriptor
		);

		if ( jaxbDiscriminatorColumn.isForceSelection() || jaxbDiscriminatorColumn.isInsertable() == FALSE ) {
			final MutableAnnotationUsage<DiscriminatorOptions> optionsAnn = XmlProcessingHelper.getOrMakeAnnotation(
					DiscriminatorOptions.class,
					target,
					xmlDocumentContext
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
			JaxbDiscriminatorFormulaImpl jaxbDiscriminatorFormula,
			MutableClassDetails target,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbDiscriminatorFormula == null ) {
			return;
		}
		if ( StringHelper.isEmpty( jaxbDiscriminatorFormula.getFragment() ) ) {
			return;
		}

		final MutableAnnotationUsage<DiscriminatorFormula> discriminatorFormulaAnn = XmlProcessingHelper.getOrMakeAnnotation( DiscriminatorFormula.class, target, xmlDocumentContext );

		discriminatorFormulaAnn.setAttributeValue( "value", jaxbDiscriminatorFormula.getFragment() );
		XmlProcessingHelper.applyAttributeIfSpecified( "discriminatorType", jaxbDiscriminatorFormula.getDiscriminatorType(), discriminatorFormulaAnn );



		if ( jaxbDiscriminatorFormula.isForceSelection() ) {
			final MutableAnnotationUsage<DiscriminatorOptions> optionsAnn = XmlProcessingHelper.getOrMakeAnnotation(
					DiscriminatorOptions.class,
					target,
					xmlDocumentContext
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
}
