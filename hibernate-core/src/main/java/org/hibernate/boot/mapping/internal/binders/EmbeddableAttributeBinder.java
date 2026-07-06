/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.annotations.CompositeType;
import org.hibernate.annotations.EmbeddedTable;
import org.hibernate.boot.mapping.internal.model.AggregateMappingIntent;
import org.hibernate.boot.mapping.internal.materialize.EmbeddableMappingMaterializer;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.mapping.internal.sources.BasicValueSource;
import org.hibernate.boot.mapping.internal.sources.ColumnSource;
import org.hibernate.boot.mapping.internal.sources.ComponentSource;
import org.hibernate.boot.mapping.internal.view.AttributeBindingView;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.relational.TableReference;
import org.hibernate.boot.mapping.internal.categorize.AttributeMetadata;
import org.hibernate.boot.mapping.internal.categorize.IdentifiableTypeMetadata;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.property.access.internal.PropertyAccessStrategyCompositeUserTypeImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyGetterImpl;
import org.hibernate.usertype.CompositeUserType;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;

import static org.hibernate.boot.model.internal.TimeZoneStorageHelper.resolveTimeZoneStorageCompositeUserType;

/// Binds component-valued singular attributes.
///
/// The binder creates the `Component` value for an embedded attribute and then
/// delegates nested member binding to [ComponentBinder].  The component has a
/// single physical table: either the owner's primary table or one secondary table
/// selected by effective column/join-column declarations.  Hibernate does not
/// support a single embeddable attribute spanning multiple tables, so conflicting
/// nested table declarations are rejected here.
///
/// @since 9.0
/// @author Steve Ebersole
class EmbeddableAttributeBinder {
	private final IdentifiableTypeMetadata ownerType;
	private final AttributeBindingView attributeBinding;
	private final PersistentClass ownerBinding;
	private final AttributeMetadata attributeMetadata;
	private final Table primaryTable;
	private final ModelBinders modelBinders;
	private final BindingState bindingState;
	private final BindingOptions bindingOptions;
	private final BindingContext bindingContext;
	private final boolean registerCollectionBindings;
	private ComponentSource componentSource;

	EmbeddableAttributeBinder(
			IdentifiableTypeMetadata ownerType,
			AttributeBindingView attributeBinding,
			PersistentClass ownerBinding,
			AttributeMetadata attributeMetadata,
			Table primaryTable,
			ModelBinders modelBinders,
			BindingState bindingState,
			BindingOptions bindingOptions,
			BindingContext bindingContext) {
		this(
				ownerType,
				attributeBinding,
				ownerBinding,
				attributeMetadata,
				primaryTable,
				modelBinders,
				bindingState,
				bindingOptions,
				bindingContext,
				true
		);
	}

	EmbeddableAttributeBinder(
			IdentifiableTypeMetadata ownerType,
			AttributeBindingView attributeBinding,
			PersistentClass ownerBinding,
			AttributeMetadata attributeMetadata,
			Table primaryTable,
			ModelBinders modelBinders,
			BindingState bindingState,
			BindingOptions bindingOptions,
			BindingContext bindingContext,
			boolean registerCollectionBindings) {
		this.ownerType = ownerType;
		this.attributeBinding = attributeBinding;
		this.ownerBinding = ownerBinding;
		this.attributeMetadata = attributeMetadata;
		this.primaryTable = primaryTable;
		this.modelBinders = modelBinders;
		this.bindingState = bindingState;
		this.bindingOptions = bindingOptions;
		this.bindingContext = bindingContext;
		this.registerCollectionBindings = registerCollectionBindings;
	}

	Component bind(Property property) {
		final MemberDetails member = attributeBinding.member();
		final Class<? extends CompositeUserType<?>> compositeUserTypeClass = resolveCompositeUserType( member );
		if ( compositeUserTypeClass != null ) {
			componentSource = ComponentSource.syntheticEmbeddedAttribute(
					member,
					resolveCompositeUserTypeEmbeddable( compositeUserTypeClass ),
					ownerType.getClassDetails(),
					ownerType.getHierarchy().getRoot().getClassDetails(),
					ownerType.getAccessType(),
					bindingContext
			);
		}
		else if ( isPluralAggregateBasic( member ) ) {
			componentSource = ComponentSource.pluralAggregateAttribute(
					member,
					ownerType.getClassDetails(),
					ownerType.getHierarchy().getRoot().getClassDetails(),
					ownerType.getAccessType(),
					bindingContext
			);
		}
		else {
			componentSource = ComponentSource.embeddedAttribute(
					member,
					ownerType.getClassDetails(),
					ownerType.getHierarchy().getRoot().getClassDetails(),
					ownerType.getAccessType(),
					bindingContext
			);
		}
		final Table componentTable = resolveComponentTable( member );
		final Component component = new EmbeddableMappingMaterializer( bindingState ).createEmbeddedAttributeComponent(
				componentSource,
				ownerBinding,
				componentTable,
				ownerType.getClassDetails().getClassName(),
				attributeBinding.attributeName()
		);
		final CompositeUserType<?> compositeUserType = compositeUserTypeClass == null
				? null
				: instantiateCompositeUserType( compositeUserTypeClass );
		if ( compositeUserType != null ) {
			component.setTypeName( compositeUserTypeClass.getName() );
		}
		bindDiscriminator( component, componentTable );

		new ComponentBinder( modelBinders, bindingState, bindingOptions, bindingContext ).bindBasicProperties(
				ownerType,
				ownerBinding,
				componentSource,
				component,
				componentTable,
				(ignored, column) -> {
				},
				false,
				true,
				true,
				registerCollectionBindings
		);
		if ( compositeUserType != null ) {
			processCompositeUserType( component, compositeUserType );
		}
		property.setOptional( true );
		return component;
	}

	private static boolean isPluralAggregateBasic(MemberDetails member) {
		return AggregateMappingIntent.isAggregateArray( member, member.getType() );
	}

	private Class<? extends CompositeUserType<?>> resolveCompositeUserType(MemberDetails member) {
		final CompositeType compositeType = member.getDirectAnnotationUsage( CompositeType.class );
		if ( compositeType != null ) {
			return compositeType.value();
		}

		final var returnedClass = attributeBinding.resolvedType().determineRawClass();
		final Class<? extends CompositeUserType<?>> timeZoneStorageCompositeUserType =
				resolveTimeZoneStorageCompositeUserType(
						member,
						returnedClass,
						bindingState.getMetadataBuildingContext()
				);
		if ( timeZoneStorageCompositeUserType != null ) {
			return timeZoneStorageCompositeUserType;
		}
		if ( returnedClass == null || !returnedClass.isRealClass() ) {
			return null;
		}
		final Class<?> javaClass = returnedClass.toJavaClass();
		return javaClass == null ? null : bindingState.findRegisteredCompositeUserType( javaClass );
	}

	private ClassDetails resolveCompositeUserTypeEmbeddable(
			Class<? extends CompositeUserType<?>> compositeUserTypeClass) {
		final CompositeUserType<?> compositeUserType = instantiateCompositeUserType( compositeUserTypeClass );
		return bindingContext.getClassDetailsRegistry()
				.resolveClassDetails( compositeUserType.embeddable().getName() );
	}

	private CompositeUserType<?> instantiateCompositeUserType(
			Class<? extends CompositeUserType<?>> compositeUserTypeClass) {
		return bindingContext.getBuildingPlan().isAllowExtensionsInCdi()
				? bindingContext.getManagedBeanRegistry().getBean( compositeUserTypeClass ).getBeanInstance()
				: FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( compositeUserTypeClass );
	}

	static void processCompositeUserType(Component component, CompositeUserType<?> compositeUserType) {
		component.sortProperties();
		final List<String> sortedPropertyNames = new ArrayList<>( component.getPropertySpan() );
		final List<Type> sortedPropertyTypes = new ArrayList<>( component.getPropertySpan() );
		final var strategy = new PropertyAccessStrategyCompositeUserTypeImpl(
				compositeUserType,
				sortedPropertyNames,
				sortedPropertyTypes
		);
		for ( var property : component.getProperties() ) {
			final String propertyName = property.getName();
			sortedPropertyNames.add( propertyName );
			sortedPropertyTypes.add(
					PropertyAccessStrategyGetterImpl.INSTANCE.buildPropertyAccess(
							compositeUserType.embeddable(),
							propertyName,
							false
					).getGetter().getReturnType()
			);
			property.setPropertyAccessStrategy( strategy );
		}
	}

	private void bindDiscriminator(Component component, Table componentTable) {
		bindDiscriminator(
				component,
				componentTable,
				componentSource,
				attributeBinding.attributeName() + "_DTYPE",
				bindingState,
				bindingOptions,
				bindingContext
		);
	}

	static void bindDiscriminator(
			Component component,
			Table componentTable,
			ComponentSource componentSource,
			String implicitColumnName,
			BindingState bindingState,
			BindingOptions bindingOptions,
			BindingContext bindingContext) {
		final Map<Object, String> discriminatorValues = new LinkedHashMap<>();
		final Map<String, String> subclassToSuperclass = new LinkedHashMap<>();
		collectDiscriminatorValue( componentSource.componentType(), discriminatorValues );
		collectPersistentSuperclassLinks( componentSource.componentType(), subclassToSuperclass );
		final List<ClassDetails> subtypes = collectConcreteComponentSubtypes( componentSource, bindingContext );
		subtypes.sort( Comparator
				.comparingInt( (ClassDetails subtype) -> hierarchyDistance( subtype, componentSource.componentType() ) )
				.thenComparing( ClassDetails::getName ) );
		subtypes.forEach( embeddableType -> {
			collectDiscriminatorValue( embeddableType, discriminatorValues );
			collectPersistentSuperclassLinks( embeddableType, subclassToSuperclass );
		} );
		collectRuntimeSubtypeSuperclassLinks( componentSource, bindingContext, subclassToSuperclass );
		if ( discriminatorValues.size() <= 1 ) {
			return;
		}

		final BasicValue discriminator = BasicValue.unregistered( bindingState.getMetadataBuildingContext(), componentTable );
		discriminator.setTable( componentTable );
		discriminator.setTypeName( String.class.getName() );
		final ColumnSource overrideColumnSource = componentSource.discriminatorColumnSource();
		final DiscriminatorColumn discriminatorColumn =
				componentSource.componentType().getDirectAnnotationUsage( DiscriminatorColumn.class );
		if ( overrideColumnSource != null ) {
			final org.hibernate.mapping.Column column = ColumnBinder.bindColumn(
					overrideColumnSource,
					() -> implicitColumnName,
					false,
					true
			);
			componentTable.addColumn( column );
			discriminator.addColumn( column, true, true );
		}
		else if ( discriminatorColumn == null ) {
			final org.hibernate.mapping.Column column = ColumnBinder.bindColumn(
					null,
					() -> implicitColumnName,
					false,
					true
			);
			componentTable.addColumn( column );
			discriminator.addColumn( column, true, true );
		}
		else {
			ColumnBinder.bindDiscriminatorColumn(
					bindingContext,
					null,
					discriminator,
					discriminatorColumn,
					bindingOptions,
					bindingState
			);
		}
		bindingState.addAttributeValueResolution(
				AttributeBindingPhase.valueResolution( discriminator, BasicValueSource.discriminator( String.class ) )
		);
		component.setDiscriminator( discriminator );
		component.setDiscriminatorValues( discriminatorValues );
		component.setSubclassToSuperclass( subclassToSuperclass );
	}

	private static List<ClassDetails> collectConcreteComponentSubtypes(
			ComponentSource componentSource,
			BindingContext bindingContext) {
		final Map<String, ClassDetails> subtypes = new LinkedHashMap<>();
		bindingContext.getCategorizedDomainModel().forEachEmbeddable( (name, embeddableType) -> {
			if ( isSubtypeOf( embeddableType, componentSource.componentType() ) ) {
				subtypes.put( className( embeddableType ), embeddableType );
			}
		} );
		return new ArrayList<>( subtypes.values() );
	}

	private static void collectRuntimeSubtypeSuperclassLinks(
			ComponentSource componentSource,
			BindingContext bindingContext,
			Map<String, String> subclassToSuperclass) {
		bindingContext.getClassDetailsRegistry().forEachClassDetails( (classDetails) -> {
			if ( isSubtypeOf( classDetails, componentSource.componentType() ) ) {
				collectPersistentSuperclassLinks( classDetails, subclassToSuperclass );
			}
		} );
	}

	private static void collectDiscriminatorValue(ClassDetails embeddableType, Map<Object, String> discriminatorValues) {
		final DiscriminatorValue discriminatorValue = embeddableType.getDirectAnnotationUsage( DiscriminatorValue.class );
		final String value = discriminatorValue == null || StringHelper.isBlank( discriminatorValue.value() )
				? StringHelper.unqualify( className( embeddableType ) )
				: discriminatorValue.value();
		discriminatorValues.put( value, className( embeddableType ).intern() );
	}

	private static void collectPersistentSuperclassLinks(
			ClassDetails componentType,
			Map<String, String> subclassToSuperclass) {
		for ( ClassDetails current = componentType; current != null; current = current.getSuperClass() ) {
			final ClassDetails superClass = current.getSuperClass();
			if ( !isPersistentComponentSuperType( superClass ) ) {
				return;
			}
			subclassToSuperclass.put( className( current ), className( superClass ) );
		}
	}

	private static boolean isPersistentComponentSuperType(ClassDetails superClass) {
		return superClass != null
			&& superClass != ClassDetails.OBJECT_CLASS_DETAILS;
	}

	private static boolean isSubtypeOf(ClassDetails subtype, ClassDetails supertype) {
		for ( ClassDetails candidate = subtype.getSuperClass(); candidate != null; candidate = candidate.getSuperClass() ) {
			if ( className( candidate ).equals( className( supertype ) ) ) {
				return true;
			}
		}
		return false;
	}

	private static int hierarchyDistance(ClassDetails subtype, ClassDetails supertype) {
		int distance = 0;
		for ( ClassDetails candidate = subtype; candidate != null; candidate = candidate.getSuperClass() ) {
			if ( className( candidate ).equals( className( supertype ) ) ) {
				return distance;
			}
			distance++;
		}
		return Integer.MAX_VALUE;
	}

	private static String className(ClassDetails classDetails) {
		final String className = classDetails.getClassName();
		return className == null ? classDetails.getName() : className;
	}

	private Table resolveComponentTable(MemberDetails attributeMember) {
		return resolveExplicitEmbeddedTable( attributeMember );
	}

	private Table resolveExplicitEmbeddedTable(MemberDetails attributeMember) {
		final EmbeddedTable embeddedTable = attributeMember.getDirectAnnotationUsage( EmbeddedTable.class );
		if ( embeddedTable == null ) {
			return primaryTable;
		}

		final Identifier identifier = Identifier.toIdentifier( embeddedTable.value() );
		final TableReference tableReference = bindingState.getTableByName( identifier.getCanonicalName() );
		if ( tableReference == null ) {
			throw new MappingException( String.format( Locale.ROOT,
					"Could not resolve @EmbeddedTable table `%s` for %s.%s",
					embeddedTable.value(),
					attributeMember.getDeclaringType().getName(),
					attributeBinding.attributeName()
			) );
			}
			return tableReference.binding();
		}
}
