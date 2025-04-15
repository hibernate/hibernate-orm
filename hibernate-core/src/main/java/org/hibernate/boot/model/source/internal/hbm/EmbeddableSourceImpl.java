/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.ToolingHintContainer;
import org.hibernate.boot.model.JavaTypeDescriptor;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.AttributeRole;
import org.hibernate.boot.model.source.spi.AttributeSource;
import org.hibernate.boot.model.source.spi.AttributeSourceContainer;
import org.hibernate.boot.model.source.spi.EmbeddableMapping;
import org.hibernate.boot.model.source.spi.EmbeddableSource;
import org.hibernate.boot.model.source.spi.LocalMetadataBuildingContext;
import org.hibernate.boot.model.source.spi.NaturalIdMutability;
import org.hibernate.boot.model.source.spi.ToolingHintContext;

/**
 * @author Steve Ebersole
 */
public class EmbeddableSourceImpl extends AbstractHbmSourceNode implements EmbeddableSource {
	private final EmbeddableMapping jaxbEmbeddableMapping;
	private final JavaTypeDescriptor typeDescriptor;

	private final AttributeRole attributeRoleBase;
	private final AttributePath attributePathBase;
	private final ToolingHintContext toolingHintContext;

	private final boolean isDynamic;
	private final boolean isUnique;

	private final List<AttributeSource> attributeSources;

	public EmbeddableSourceImpl(
			MappingDocument mappingDocument,
			EmbeddableSourceContainer container,
			EmbeddableMapping jaxbEmbeddableMapping,
			List attributeMappings,
			boolean isDynamic,
			boolean isUnique,
			String logicalTableName,
			NaturalIdMutability naturalIdMutability) {
		super( mappingDocument );
		this.attributeRoleBase = container.getAttributeRoleBase();
		this.attributePathBase = container.getAttributePathBase();
		if ( jaxbEmbeddableMapping instanceof ToolingHintContainer toolingHintContainer ) {
			this.toolingHintContext = Helper.collectToolingHints(
					container.getToolingHintContextBaselineForEmbeddable(),
					toolingHintContainer
			);
		}
		else {
			this.toolingHintContext = container.getToolingHintContextBaselineForEmbeddable();
		}

		this.jaxbEmbeddableMapping = jaxbEmbeddableMapping;
		this.isDynamic = isDynamic;
		this.isUnique = isUnique;

		final String typeName = isDynamic
				? jaxbEmbeddableMapping.getClazz()
				: mappingDocument.qualifyClassName( jaxbEmbeddableMapping.getClazz() );
		this.typeDescriptor = new JavaTypeDescriptor() {
			@Override
			public String getName() {
				return typeName;
			}
		};

		this.attributeSources = new ArrayList<>();
		AttributesHelper.processAttributes(
				mappingDocument,
				new AttributesHelper.Callback() {
					@Override
					public AttributeSourceContainer getAttributeSourceContainer() {
						return EmbeddableSourceImpl.this;
					}

					@Override
					public void addAttributeSource(AttributeSource attributeSource) {
						attributeSources.add( attributeSource );
					}
				},
				attributeMappings,
				logicalTableName,
				naturalIdMutability
		);
	}

	@Override
	public JavaTypeDescriptor getTypeDescriptor() {
		return typeDescriptor;
	}

	@Override
	public String getParentReferenceAttributeName() {
		return jaxbEmbeddableMapping.getParent();
	}

	@Override
	public boolean isDynamic() {
		return isDynamic;
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
