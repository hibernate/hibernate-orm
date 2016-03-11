/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
		return ! Helper.getValue(
				propertyElement.isNotNull(),
				naturalIdMutability != NaturalIdMutability.NOT_NATURAL_ID
		);
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
