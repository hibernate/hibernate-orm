/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.materialize;

import java.util.Locale;

import org.hibernate.MappingException;
import org.hibernate.annotations.EmbeddedColumnNaming;
import org.hibernate.boot.models.bind.internal.model.EmbeddableContribution;
import org.hibernate.boot.models.bind.internal.sources.ComponentSource;
import org.hibernate.boot.models.bind.internal.view.EmbeddableContributionView;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

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

	public EmbeddableContributionView createContributionView(ComponentSource source) {
		final EmbeddableContribution contribution = EmbeddableContribution.from( source );
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
		component.setComponentClassName( source.componentType().getClassName() );
		component.setTable( table );
		component.setTypeUsingReflection( ownerClassName, attributeName );
		return component;
	}

	public Component createNestedComponent(
			ComponentSource source,
			Component parent,
			Table table,
			String ownerClassName,
			String attributeName) {
		final Component component = new Component( state.getMetadataBuildingContext(), parent );
		component.setComponentClassName( source.componentType().getClassName() );
		component.setTable( table );
		component.setTypeUsingReflection( ownerClassName, attributeName );
		return component;
	}

	public Component createCollectionElementComponent(ComponentSource source, Collection collection, Table table) {
		final Component component = new Component( state.getMetadataBuildingContext(), collection );
		component.setFlattened( true );
		component.setComponentClassName( source.componentType().getClassName() );
		component.setTable( table );
		component.setRoleName( collection.getRole() );
		return component;
	}

	public Component createMapKeyComponent(ComponentSource source, Collection collection, Table table) {
		final Component component = new Component( state.getMetadataBuildingContext(), collection );
		component.setFlattened( true );
		component.setComponentClassName( source.componentType().getClassName() );
		component.setTable( table );
		component.setRoleName( collection.getRole() + ".key" );
		return component;
	}

	public void prepareComponentForBinding(Component component, ComponentSource source) {
		applyComponentCustomizations( component, source.sourceMember(), source.componentType() );
		applyColumnNamingPattern( component, source.sourceMember() );
	}

	private static void applyComponentCustomizations(
			Component component,
			MemberDetails sourceMember,
			ClassDetails componentType) {
		final org.hibernate.annotations.CompositeType compositeType = sourceMember == null
				? null
				: sourceMember.getDirectAnnotationUsage( org.hibernate.annotations.CompositeType.class );
		if ( compositeType != null ) {
			component.setTypeName( compositeType.value().getName() );
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
					"@EmbeddedColumnNaming expects pattern with exactly 1 format marker, but found %s - `%s` (%s#%s)",
					markerCount,
					pattern,
					sourceMember.getDeclaringType().getName(),
					sourceMember.getName()
			) );
		}
		component.setColumnNamingPattern( pattern );
	}
}
