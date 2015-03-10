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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.EntityMode;
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

		this.attributeSources = new ArrayList<AttributeSource>();
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

					@Override
					public void registerIndexColumn(
							String constraintName,
							String logicalTableName,
							String columnName) {
						containingCallback.registerIndexColumn( constraintName, logicalTableName, columnName );
					}

					@Override
					public void registerUniqueKeyColumn(
							String constraintName,
							String logicalTableName,
							String columnName) {
						containingCallback.registerUniqueKeyColumn( constraintName, logicalTableName, columnName );
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
	public Map<EntityMode,String> getTuplizerClassMap() {
		return Collections.emptyMap();
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
