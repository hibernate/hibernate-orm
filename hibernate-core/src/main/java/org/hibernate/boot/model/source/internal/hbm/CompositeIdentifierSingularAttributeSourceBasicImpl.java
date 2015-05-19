/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeKeyBasicAttributeType;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.AttributeRole;
import org.hibernate.boot.model.source.spi.AttributeSourceContainer;
import org.hibernate.boot.model.source.spi.NaturalIdMutability;
import org.hibernate.boot.model.source.spi.RelationalValueSource;
import org.hibernate.boot.model.source.spi.SingularAttributeNature;
import org.hibernate.boot.model.source.spi.SingularAttributeSourceBasic;
import org.hibernate.boot.model.source.spi.SizeSource;
import org.hibernate.boot.model.source.spi.ToolingHintContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.tuple.GenerationTiming;

/**
 * Descriptor for {@code <key-property/>}
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class CompositeIdentifierSingularAttributeSourceBasicImpl
		extends AbstractHbmSourceNode
		implements SingularAttributeSourceBasic {
	private final JaxbHbmCompositeKeyBasicAttributeType keyPropertyElement;
	private final HibernateTypeSourceImpl typeSource;
	private final List<RelationalValueSource> valueSources;

	private final AttributePath attributePath;
	private final AttributeRole attributeRole;
	private final ToolingHintContext toolingHintContext;

	public CompositeIdentifierSingularAttributeSourceBasicImpl(
			MappingDocument mappingDocument,
			AttributeSourceContainer container,
			final JaxbHbmCompositeKeyBasicAttributeType keyPropertyElement) {
		super( mappingDocument );
		this.keyPropertyElement = keyPropertyElement;
		this.typeSource = new HibernateTypeSourceImpl( keyPropertyElement );

		this.valueSources = RelationalValueSourceHelper.buildValueSources(
				sourceMappingDocument(),
				null,
				new RelationalValueSourceHelper.AbstractColumnsAndFormulasSource() {
					@Override
					public XmlElementMetadata getSourceType() {
						return XmlElementMetadata.KEY_PROPERTY;
					}

					@Override
					public String getSourceName() {
						return keyPropertyElement.getName();
					}

					@Override
					public String getColumnAttribute() {
						return keyPropertyElement.getColumnAttribute();
					}

					@Override
					public List getColumnOrFormulaElements() {
						return keyPropertyElement.getColumn();
					}

					@Override
					public SizeSource getSizeSource() {
						return Helper.interpretSizeSource(
								keyPropertyElement.getLength(),
								(Integer) null,
								null
						);
					}
				}
		);

		this.attributePath = container.getAttributePathBase().append( getName() );
		this.attributeRole = container.getAttributeRoleBase().append( getName() );

		this.toolingHintContext = Helper.collectToolingHints(
				container.getToolingHintContext(),
				keyPropertyElement
		);
	}

	@Override
	public SingularAttributeNature getSingularAttributeNature() {
		return SingularAttributeNature.BASIC;
	}

	@Override
	public XmlElementMetadata getSourceType() {
		return XmlElementMetadata.KEY_PROPERTY;
	}

	@Override
	public boolean isSingular() {
		return true;
	}

	@Override
	public String getName() {
		return keyPropertyElement.getName();
	}

	@Override
	public String getXmlNodeName() {
		return keyPropertyElement.getNode();
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
		return keyPropertyElement.getAccess();
	}

	@Override
	public boolean isVirtualAttribute() {
		return false;
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return false;
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return true;
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return false;
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return false;
	}

	@Override
	public List<RelationalValueSource> getRelationalValueSources() {
		return valueSources;
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return GenerationTiming.NEVER;
	}

	@Override
	public Boolean isInsertable() {
		return true;
	}

	@Override
	public Boolean isUpdatable() {
		return false;
	}

	@Override
	public boolean isBytecodeLazy() {
		return false;
	}

	@Override
	public NaturalIdMutability getNaturalIdMutability() {
		return NaturalIdMutability.NOT_NATURAL_ID;
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
