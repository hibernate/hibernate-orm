/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.transform;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeAttributeType;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributesContainer;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableAttributesContainerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;

import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/**
 * Handles {@code <component/>} to embeddable conversion during HBM XML transformation.
 * <p>
 * This class encapsulates the embeddable creation logic so it can be used both
 * during the full {@link HbmXmlTransformer} transformation (with a complete boot model)
 * and standalone at build time (without a boot model) to discover embeddable class names.
 *
 * @see HbmXmlTransformer
 */
public class HbmXmlTransformerComponentHandler {

	/**
	 * Callback for processing all nested attributes inside a component,
	 * used during the full HBM XML transformation.
	 */
	@FunctionalInterface
	public interface NestedAttributeProcessor {
		void processAttributes(
				String roleBase,
				List<?> hbmAttributeMappings,
				ManagedTypeInfo managedTypeInfo,
				JaxbAttributesContainer attributes);
	}

	private final Map<String, ComponentTypeInfo> embeddableInfoByRole;
	private final JaxbEntityMappingsImpl mappingRoot;
	private final NestedAttributeProcessor nestedAttributeProcessor;

	// todo (7.0) : use transformation-state instead
	private final Map<String, JaxbEmbeddableImpl> jaxbEmbeddableByClassName = new HashMap<>();
	private int counter = 1;

	/**
	 * Creates a handler for use during the full HBM XML transformation.
	 *
	 * @param embeddableInfoByRole component type info populated from the boot model
	 * @param mappingRoot the target mapping root to add embeddables to
	 * @param nestedAttributeProcessor callback to process nested attributes inside a component
	 */
	public HbmXmlTransformerComponentHandler(
			Map<String, ComponentTypeInfo> embeddableInfoByRole,
			JaxbEntityMappingsImpl mappingRoot,
			NestedAttributeProcessor nestedAttributeProcessor) {
		this.embeddableInfoByRole = embeddableInfoByRole;
		this.mappingRoot = mappingRoot;
		this.nestedAttributeProcessor = nestedAttributeProcessor;
	}

	/**
	 * Creates a lightweight handler that only collects component class names,
	 * without a boot model. Nested components inside a component are discovered
	 * automatically.
	 *
	 * @param mappingRoot the target mapping root to add embeddables to
	 */
	public HbmXmlTransformerComponentHandler(JaxbEntityMappingsImpl mappingRoot) {
		this( Map.of(), mappingRoot, null );
	}

	/**
	 * Handles a {@code <component/>} mapping found during attribute traversal.
	 *
	 * @return the created or cached embeddable
	 */
	public JaxbEmbeddableImpl applyEmbeddable(
			String roleBase,
			JaxbHbmCompositeAttributeType hbmComponent,
			ComponentTypeInfo componentTypeInfo) {
		final String embeddableClassName = componentTypeInfo != null
				? componentTypeInfo.getComponent().getComponentClassName()
				: hbmComponent.getClazz();
		if ( isNotEmpty( embeddableClassName ) ) {
			final var existing = jaxbEmbeddableByClassName.get( embeddableClassName );
			if ( existing != null ) {
				return existing;
			}
		}

		final String role = roleBase + "." + hbmComponent.getName();
		final String embeddableName = determineEmbeddableName( embeddableClassName, hbmComponent.getName() );
		final var jaxbEmbeddable = convertEmbeddable(
				role,
				embeddableName,
				embeddableClassName,
				hbmComponent
		);
		mappingRoot.getEmbeddables().add( jaxbEmbeddable );

		if ( isNotEmpty( embeddableClassName ) ) {
			jaxbEmbeddableByClassName.put( embeddableClassName, jaxbEmbeddable );
		}

		return jaxbEmbeddable;
	}

	public JaxbEmbeddedImpl transformEmbedded(
			JaxbEmbeddableImpl jaxbEmbeddable,
			JaxbHbmCompositeAttributeType hbmComponent) {
		final var embedded = new JaxbEmbeddedImpl();
		embedded.setName( hbmComponent.getName() );
		embedded.setTarget( jaxbEmbeddable.getName() );
		return embedded;
	}

	private JaxbEmbeddableImpl convertEmbeddable(
			String role,
			String embeddableName,
			String embeddableClassName,
			JaxbHbmCompositeAttributeType hbmComponent) {
		final var componentTypeInfo = embeddableInfoByRole.get( role );

		final var embeddable = new JaxbEmbeddableImpl();
		embeddable.setMetadataComplete( true );
		embeddable.setName( embeddableName );
		embeddable.setClazz( embeddableClassName );

		embeddable.setAttributes( new JaxbEmbeddableAttributesContainerImpl() );
		if ( nestedAttributeProcessor != null ) {
			nestedAttributeProcessor.processAttributes(
					role,
					hbmComponent.getAttributes(),
					componentTypeInfo,
					embeddable.getAttributes()
			);
		}
		else {
			for ( Object attr : hbmComponent.getAttributes() ) {
				if ( attr instanceof JaxbHbmCompositeAttributeType nested ) {
					applyEmbeddable( role, nested, null );
				}
			}
		}
		return embeddable;
	}

	public String determineEmbeddableName(String componentClassName, String attributeName) {
		if ( isNotEmpty( componentClassName ) ) {
			return componentClassName;
		}
		return attributeName + "_" + counter++;
	}

	public Map<String, JaxbEmbeddableImpl> getJaxbEmbeddableByClassName() {
		return jaxbEmbeddableByClassName;
	}
}
