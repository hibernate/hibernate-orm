/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import java.util.Locale;

import org.hibernate.MappingException;
import org.hibernate.annotations.EmbeddedColumnNaming;
import org.hibernate.boot.mapping.internal.model.EmbeddableContribution;
import org.hibernate.boot.mapping.internal.binders.MappedSuperTypeBinder;
import org.hibernate.boot.mapping.internal.sources.ComponentSource;
import org.hibernate.boot.mapping.internal.view.EmbeddableContributionView;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.MappingHelper;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.mapping.MappingRole;
import org.hibernate.usertype.CompositeUserType;

import static org.hibernate.internal.util.StringHelper.count;
import static org.hibernate.internal.util.StringHelper.isEmpty;

/// Materializes legacy component mapping objects from embeddable contribution
/// source facts.
///
/// This is the first embeddable-side bridge toward the horizontal
/// binding/view/materialization flow.  The semantic source facts are captured as
/// an [EmbeddableContribution], exposed through an [EmbeddableContributionView],
/// and then used to create/configure the mutable `org.hibernate.mapping`
/// [Component] instances required by current boot/runtime consumers.
///
/// @since 9.0
/// @author Steve Ebersole
public class EmbeddableMappingMaterializer {
	private final BindingState state;

	public EmbeddableMappingMaterializer(BindingState state) {
		this.state = state;
	}

	public EmbeddableContributionView createContributionView(ComponentSource source, BindingContext bindingContext) {
		final EmbeddableContribution contribution = EmbeddableContribution.from( source, state, bindingContext );
		state.getBootBindingModel().addEmbeddableContribution( contribution );
		return state.getBootBindingModel().embeddableContributionView( contribution );
	}

	public Component createEmbeddedAttributeComponent(
			ComponentSource source,
			PersistentClass ownerBinding,
			Table table,
			String ownerClassName,
			String attributeName) {
		final Component component = new Component( state.getMetadataBuildingContext(), table, ownerBinding );
		applyComponentType( component, source.componentType() );
		applyComponentMappedSuperclass( component, source.componentType() );
		component.setTable( table );
		component.setTypeUsingReflection( ownerClassName, attributeName, state.getMetadataBuildingContext() );
		return component;
	}

	public Component createNestedComponent(
			ComponentSource source,
			Component parent,
			Table table,
			String ownerClassName,
			String attributeName) {
		final Component component = new Component( state.getMetadataBuildingContext(), parent );
		applyComponentType( component, source.componentType() );
		applyComponentMappedSuperclass( component, source.componentType() );
		component.setTable( table );
		component.setTypeUsingReflection( ownerClassName, attributeName, state.getMetadataBuildingContext() );
		return component;
	}

	public Component createCollectionElementComponent(ComponentSource source, Collection collection, Table table) {
		final Component component = new Component( state.getMetadataBuildingContext(), collection );
		component.setMappingRole( MappingRole.collection( collection.getRole() ).append( MappingRole.PartKind.ELEMENT ) );
		component.setFlattened( true );
		applyComponentType( component, source.componentType() );
		applyComponentMappedSuperclass( component, source.componentType() );
		component.setTable( table );
		component.setRoleName( collection.getRole() );
		return component;
	}

	public Component createMapKeyComponent(ComponentSource source, Collection collection, Table table) {
		final Component component = new Component( state.getMetadataBuildingContext(), collection );
		component.setMappingRole( MappingRole.collection( collection.getRole() ).append( MappingRole.PartKind.INDEX ) );
		component.setFlattened( true );
		applyComponentType( component, source.componentType() );
		applyComponentMappedSuperclass( component, source.componentType() );
		component.setTable( table );
		component.setRoleName( collection.getRole() + ".key" );
		return component;
	}

	public static void applyComponentType(Component component, ClassDetails componentType) {
		component.setComponentClassDetails( componentType );
	}

	public static void applyComponentMappedSuperclass(
			Component component,
			ClassDetails componentType,
			BindingState state) {
		for ( ClassDetails candidate = componentType.getSuperClass();
				candidate != null && candidate != ClassDetails.OBJECT_CLASS_DETAILS;
				candidate = candidate.getSuperClass() ) {
			if ( state.getTypeBinder( candidate ) instanceof MappedSuperTypeBinder mappedSuperTypeBinder ) {
				component.setMappedSuperclass( mappedSuperTypeBinder.getTypeBinding() );
				return;
			}
			final MappedSuperclass mappedSuperclass = state.getMetadataBuildingContext()
					.getMetadataCollector()
					.getMappedSuperclass( candidate );
			if ( mappedSuperclass != null ) {
				component.setMappedSuperclass( mappedSuperclass );
				return;
			}
		}
	}

	private void applyComponentMappedSuperclass(Component component, ClassDetails componentType) {
		applyComponentMappedSuperclass( component, componentType, state );
	}

	public void prepareComponentForBinding(Component component, ComponentSource source) {
		applyComponentCustomizations( component, source.sourceMember(), source.componentType() );
		applyColumnNamingPattern( component, source.sourceMember() );
	}

	private void applyComponentCustomizations(
			Component component,
			MemberDetails sourceMember,
			ClassDetails componentType) {
		final org.hibernate.annotations.CompositeType compositeType = sourceMember == null
				? null
				: sourceMember.getDirectAnnotationUsage( org.hibernate.annotations.CompositeType.class );
		if ( compositeType != null ) {
			component.setCompositeUserType( instantiateCompositeUserType( compositeType.value() ) );
		}

		final org.hibernate.annotations.EmbeddableInstantiator memberInstantiator = sourceMember == null
				? null
				: sourceMember.getDirectAnnotationUsage( org.hibernate.annotations.EmbeddableInstantiator.class );
		final org.hibernate.annotations.EmbeddableInstantiator typeInstantiator =
				componentType.getDirectAnnotationUsage( org.hibernate.annotations.EmbeddableInstantiator.class );
		final org.hibernate.annotations.EmbeddableInstantiator instantiator =
				memberInstantiator == null ? typeInstantiator : memberInstantiator;
		if ( instantiator != null ) {
			component.setCustomInstantiator( instantiator.value() );
			return;
		}

		if ( !componentType.isRealClass() ) {
			return;
		}

		final Class<? extends EmbeddableInstantiator> registeredInstantiator =
				state.findRegisteredEmbeddableInstantiator( componentType.toJavaClass() );
		if ( registeredInstantiator != null ) {
			component.setCustomInstantiator( registeredInstantiator );
		}
	}

	private static void applyColumnNamingPattern(Component component, MemberDetails sourceMember) {
		if ( sourceMember == null ) {
			return;
		}

		final EmbeddedColumnNaming columnNaming = sourceMember.getDirectAnnotationUsage( EmbeddedColumnNaming.class );
		if ( columnNaming == null ) {
			return;
		}

		final String pattern = isEmpty( columnNaming.value() )
				? sourceMember.resolveAttributeName() + "_%s"
				: columnNaming.value();
		final int markerCount = count( pattern, '%' );
		if ( markerCount != 1 ) {
			throw new MappingException( String.format(
					Locale.ROOT,
					"@EmbeddedColumnNaming expects pattern with exactly 1 format maker, but found %s - `%s` (%s#%s)",
					markerCount,
					pattern,
					sourceMember.getDeclaringType().getName(),
					sourceMember.getName()
			) );
		}
		component.setColumnNamingPattern( pattern );
	}

	private CompositeUserType<?> instantiateCompositeUserType(
			Class<? extends CompositeUserType<?>> compositeUserTypeClass) {
		final var buildingContext = state.getMetadataBuildingContext();
		return MappingHelper.createCompositeUserType(
				compositeUserTypeClass,
				buildingContext.getManagedBeanRegistry(),
				buildingContext.getBuildingPlan().isAllowExtensionsInCdi()
		);
	}
}
