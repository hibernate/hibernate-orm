/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeKeyBasicAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeKeyManyToOneType;
import org.hibernate.boot.model.JavaTypeDescriptor;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.AttributeRole;
import org.hibernate.boot.model.source.spi.AttributeSource;
import org.hibernate.boot.model.source.spi.EmbeddableSource;
import org.hibernate.boot.model.source.spi.LocalMetadataBuildingContext;
import org.hibernate.boot.model.source.spi.ToolingHintContext;

/**
 * @author Steve Ebersole
 */
class IdClassSource implements EmbeddableSource {
	private final JavaTypeDescriptor idClassDescriptor;
	private final RootEntitySourceImpl rootEntitySource;
	private final MappingDocument sourceMappingDocument;
	private final AttributePath attributePathBase;
	private final AttributeRole attributeRoleBase;

	private final List<AttributeSource> attributeSources;


	IdClassSource(
			JavaTypeDescriptor idClassDescriptor,
			RootEntitySourceImpl rootEntitySource,
			MappingDocument sourceMappingDocument) {
		this.idClassDescriptor = idClassDescriptor;
		this.rootEntitySource = rootEntitySource;
		this.sourceMappingDocument = sourceMappingDocument;
		this.attributePathBase = rootEntitySource.getAttributePathBase().append( "<IdClass>" );
		this.attributeRoleBase = rootEntitySource.getAttributeRoleBase().append( "<IdClass>" );

		this.attributeSources = new ArrayList<>();
		for ( Object attribute : rootEntitySource.jaxbEntityMapping().getCompositeId().getKeyPropertyOrKeyManyToOne() ) {
			if ( attribute instanceof JaxbHbmCompositeKeyBasicAttributeType compositeKeyBasicAttributeType ) {
				attributeSources.add(
						new CompositeIdentifierSingularAttributeSourceBasicImpl(
								sourceMappingDocument,
								this,
								compositeKeyBasicAttributeType
						)
				);
			}
			else if ( attribute instanceof JaxbHbmCompositeKeyManyToOneType compositeKeyManyToOneAttribute ) {
				attributeSources.add(
						new CompositeIdentifierSingularAttributeSourceManyToOneImpl(
								sourceMappingDocument,
								this,
								compositeKeyManyToOneAttribute
						)
				);
			}
			else {
				throw new AssertionFailure( "Unexpected attribute type" );
			}
		}
	}

	@Override
	public JavaTypeDescriptor getTypeDescriptor() {
		return idClassDescriptor;
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
		return false;
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
		return rootEntitySource.getLocalMetadataBuildingContext();
	}

	@Override
	public ToolingHintContext getToolingHintContext() {
		return rootEntitySource.getToolingHintContext();
	}
}
