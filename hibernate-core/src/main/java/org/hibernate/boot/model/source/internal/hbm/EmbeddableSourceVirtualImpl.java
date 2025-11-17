/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmPropertiesType;
import org.hibernate.boot.model.JavaTypeDescriptor;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.AttributeRole;
import org.hibernate.boot.model.source.spi.AttributeSource;
import org.hibernate.boot.model.source.spi.AttributeSourceContainer;
import org.hibernate.boot.model.source.spi.EmbeddableSource;
import org.hibernate.boot.model.source.spi.LocalMetadataBuildingContext;
import org.hibernate.boot.model.source.spi.NaturalIdMutability;
import org.hibernate.boot.model.source.spi.ToolingHintContext;

/**
 * A virtual embeddable; what Hibernate historically (pre-JPA) called an embedded
 * component.  Mainly used to model a {@code <properties/>} mapping
 *
 * @author Steve Ebersole
 */
public class EmbeddableSourceVirtualImpl extends AbstractHbmSourceNode implements EmbeddableSource {
	private final JavaTypeDescriptor typeDescriptor = new JavaTypeDescriptor() {
		@Override
		public String getName() {
			return null;
		}
	};

	private final AttributeRole attributeRoleBase;
	private final AttributePath attributePathBase;

	private final List<AttributeSource> attributeSources;

	private final boolean isUnique;
	private final ToolingHintContext toolingHintContext;

	public EmbeddableSourceVirtualImpl(
			MappingDocument mappingDocument,
			final AttributesHelper.Callback containingCallback,
			EmbeddableSourceContainer container,
			List attributeJaxbMappings,
			String logicalTableName,
			NaturalIdMutability naturalIdMutability,
			JaxbHbmPropertiesType jaxbPropertiesGroup) {
		super( mappingDocument );
		this.attributeRoleBase = container.getAttributeRoleBase();
		this.attributePathBase = container.getAttributePathBase();

		this.attributeSources = new ArrayList<>();
		AttributesHelper.processAttributes(
				mappingDocument,
				new AttributesHelper.Callback() {
					@Override
					public AttributeSourceContainer getAttributeSourceContainer() {
						return EmbeddableSourceVirtualImpl.this;
					}

					@Override
					public void addAttributeSource(AttributeSource attributeSource) {
						attributeSources.add( attributeSource );
					}
				},
				attributeJaxbMappings,
				logicalTableName,
				naturalIdMutability
		);

		this.isUnique = jaxbPropertiesGroup.isUnique();

		this.toolingHintContext = container.getToolingHintContextBaselineForEmbeddable();
	}

	@Override
	public JavaTypeDescriptor getTypeDescriptor() {
		return typeDescriptor;
	}

	@Override
	public String getParentReferenceAttributeName() {
		return null;
	}

	@Override
	public boolean isDynamic() {
		return false;
	}

	@Override
	public boolean isUnique() {
		return isUnique;
	}

	@Override
	public AttributePath getAttributePathBase() {
		return attributePathBase;
	}

	@Override
	public AttributeRole getAttributeRoleBase() {
		return attributeRoleBase;
	}

	@Override
	public List<AttributeSource> attributeSources() {
		return attributeSources;
	}

	@Override
	public LocalMetadataBuildingContext getLocalMetadataBuildingContext() {
		return metadataBuildingContext();
	}

	@Override
	public ToolingHintContext getToolingHintContext() {
		return toolingHintContext;
	}
}
