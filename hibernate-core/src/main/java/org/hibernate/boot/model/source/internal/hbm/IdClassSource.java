/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
