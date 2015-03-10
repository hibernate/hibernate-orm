/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.EntityMode;
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

		this.attributeSources = new ArrayList<AttributeSource>();
		for ( Object attribute : rootEntitySource.jaxbEntityMapping().getCompositeId().getKeyPropertyOrKeyManyToOne() ) {
			if ( JaxbHbmCompositeKeyBasicAttributeType.class.isInstance( attribute ) ) {
				attributeSources.add(
						new CompositeIdentifierSingularAttributeSourceBasicImpl(
								sourceMappingDocument,
								this,
								(JaxbHbmCompositeKeyBasicAttributeType) attribute
						)
				);
			}
			else {
				attributeSources.add(
						new CompositeIdentifierSingularAttributeSourceManyToOneImpl(
								sourceMappingDocument,
								this,
								(JaxbHbmCompositeKeyManyToOneType) attribute
						)
				);
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
	public Map<EntityMode, String> getTuplizerClassMap() {
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
