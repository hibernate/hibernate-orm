/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmVersionAttributeType;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.AttributeRole;
import org.hibernate.boot.model.source.spi.NaturalIdMutability;
import org.hibernate.boot.model.source.spi.RelationalValueSource;
import org.hibernate.boot.model.source.spi.SingularAttributeNature;
import org.hibernate.boot.model.source.spi.ToolingHintContext;
import org.hibernate.boot.model.source.spi.VersionAttributeSource;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.tuple.GenerationTiming;

/**
 * Implementation for {@code <version/>} mappings
 *
 * @author Steve Ebersole
 */
class VersionAttributeSourceImpl
		extends AbstractHbmSourceNode
		implements VersionAttributeSource {
	private final JaxbHbmVersionAttributeType versionElement;
	private final HibernateTypeSourceImpl typeSource;

	private final List<RelationalValueSource> relationalValueSources;

	private final AttributePath attributePath;
	private final AttributeRole attributeRole;
	private final ToolingHintContext toolingHints;

	VersionAttributeSourceImpl(
			MappingDocument mappingDocument,
			RootEntitySourceImpl rootEntitySource,
			JaxbHbmVersionAttributeType versionElement) {
		super( mappingDocument );
		this.versionElement = versionElement;

		this.relationalValueSources = RelationalValueSourceHelper.buildValueSources(
				mappingDocument,
				null,
				new RelationalValueSourceHelper.AbstractColumnsAndFormulasSource() {
					@Override
					public XmlElementMetadata getSourceType() {
						return VersionAttributeSourceImpl.this.getSourceType();
					}

					@Override
					public String getSourceName() {
						return VersionAttributeSourceImpl.this.versionElement.getName();
					}

					@Override
					public String getColumnAttribute() {
						return VersionAttributeSourceImpl.this.versionElement.getColumnAttribute();
					}

					@Override
					public List getColumnOrFormulaElements() {
						return VersionAttributeSourceImpl.this.versionElement.getColumn();
					}

					@Override
					public Boolean isNullable() {
						return false;
					}
				}
		);

		this.typeSource = new HibernateTypeSourceImpl(
				versionElement.getType() == null
						? "integer"
						: versionElement.getType()
		);

		this.attributePath = rootEntitySource.getAttributePathBase().append( getName() );
		this.attributeRole = rootEntitySource.getAttributeRoleBase().append( getName() );

		this.toolingHints = Helper.collectToolingHints(
				rootEntitySource.getToolingHintContext(),
				versionElement
		);
	}

	@Override
	public XmlElementMetadata getSourceType() {
		return XmlElementMetadata.VERSION;
	}

	@Override
	public String getName() {
		return versionElement.getName();
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
		return versionElement.getAccess();
	}

	@Override
	public List<RelationalValueSource> getRelationalValueSources() {
		return relationalValueSources;
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return true;
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return true;
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return false;
	}

	@Override
	public String getUnsavedValue() {
		return versionElement.getUnsavedValue().value();
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return versionElement.getGenerated();
	}

	@Override
	public Boolean isInsertable() {
		return versionElement.isInsert() == null ? true : versionElement.isInsert();
	}

	@Override
	public Boolean isUpdatable() {
		return true;
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
	public boolean isIncludedInOptimisticLocking() {
		return false;
	}

	@Override
	public SingularAttributeNature getSingularAttributeNature() {
		return SingularAttributeNature.BASIC;
	}

	@Override
	public boolean isVirtualAttribute() {
		return false;
	}

	@Override
	public boolean isSingular() {
		return true;
	}

	@Override
	public String getXmlNodeName() {
		return versionElement.getNode();
	}

	@Override
	public MetadataBuildingContext getBuildingContext() {
		return metadataBuildingContext();
	}

	@Override
	public ToolingHintContext getToolingHintContext() {
		return toolingHints;
	}
}
