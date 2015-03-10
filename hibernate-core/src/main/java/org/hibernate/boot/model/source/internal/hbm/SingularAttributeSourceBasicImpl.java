/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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

import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicAttributeType;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.AttributeRole;
import org.hibernate.boot.model.source.spi.AttributeSourceContainer;
import org.hibernate.boot.model.source.spi.NaturalIdMutability;
import org.hibernate.boot.model.source.spi.RelationalValueSource;
import org.hibernate.boot.model.source.spi.SingularAttributeNature;
import org.hibernate.boot.model.source.spi.SingularAttributeSourceBasic;
import org.hibernate.boot.model.source.spi.ToolingHintContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.tuple.GenerationTiming;

/**
 * Implementation for {@code <property/>} mappings
 *
 * @author Steve Ebersole
 */
class SingularAttributeSourceBasicImpl
		extends AbstractHbmSourceNode
		implements SingularAttributeSourceBasic {
	private final JaxbHbmBasicAttributeType propertyElement;
	private final HibernateTypeSourceImpl typeSource;
	private final NaturalIdMutability naturalIdMutability;

	private final List<RelationalValueSource> relationalValueSources;

	private final AttributeRole attributeRole;
	private final AttributePath attributePath;

	private ToolingHintContext toolingHintContext;

	SingularAttributeSourceBasicImpl(
			MappingDocument sourceMappingDocument,
			AttributeSourceContainer container,
			final JaxbHbmBasicAttributeType propertyElement,
			final String logicalTableName,
			NaturalIdMutability naturalIdMutability) {
		super( sourceMappingDocument );
		this.propertyElement = propertyElement;
		this.typeSource = new HibernateTypeSourceImpl( propertyElement );
		this.naturalIdMutability = naturalIdMutability;

		this.relationalValueSources = RelationalValueSourceHelper.buildValueSources(
				sourceMappingDocument,
				logicalTableName,
				new BasicAttributeColumnsAndFormulasSource( propertyElement )
		);

		this.attributeRole = container.getAttributeRoleBase().append( getName() );
		this.attributePath = container.getAttributePathBase().append( getName() );

		this.toolingHintContext = Helper.collectToolingHints(
				container.getToolingHintContext(),
				propertyElement
		);
	}

	@Override
	public boolean isSingular() {
		return true;
	}

	@Override
	public SingularAttributeNature getSingularAttributeNature() {
		return SingularAttributeNature.BASIC;
	}

	@Override
	public XmlElementMetadata getSourceType() {
		return XmlElementMetadata.PROPERTY;
	}

	@Override
	public String getName() {
		return propertyElement.getName();
	}

	@Override
	public String getXmlNodeName() {
		return propertyElement.getNode();
	}

	@Override
	public AttributePath getAttributePath() {
		return attributePath;
	}

	@Override
	public boolean isCollectionElement() {
		return false;
	}

	@Override
	public AttributeRole getAttributeRole() {
		return attributeRole;
	}

	@Override
	public HibernateTypeSourceImpl getTypeInformation() {
		return typeSource;
	}

	@Override
	public String getPropertyAccessorName() {
		return propertyElement.getAccess();
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return propertyElement.getGenerated();
	}

	@Override
	public Boolean isInsertable() {
		return propertyElement.isInsert() == null
				? true
				: propertyElement.isInsert();
	}

	@Override
	public Boolean isUpdatable() {
		return propertyElement.isUpdate() == null
				? true
				: propertyElement.isUpdate();
	}

	@Override
	public boolean isBytecodeLazy() {
		return Helper.getValue( propertyElement.isLazy(), false );
	}

	@Override
	public NaturalIdMutability getNaturalIdMutability() {
		return naturalIdMutability;
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return Helper.getValue( propertyElement.isOptimisticLock(), true );
	}

	@Override
	public boolean isVirtualAttribute() {
		return false;
	}

	@Override
	public List<RelationalValueSource> getRelationalValueSources() {
		return relationalValueSources;
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return Helper.getValue( propertyElement.isInsert(), true );
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return Helper.getValue( propertyElement.isUpdate(), true );
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return ! Helper.getValue( propertyElement.isNotNull(), false );
	}

	@Override
	public ToolingHintContext getToolingHintContext() {
		return toolingHintContext;
	}

	@Override
	public MetadataBuildingContext getBuildingContext() {
		return sourceMappingDocument();
	}
}
