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

import java.util.Collections;
import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmTimestampAttributeType;
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
 * Implementation for {@code <timestamp/>} mappings
 *
 * @author Steve Ebersole
 */
class TimestampAttributeSourceImpl
		extends AbstractHbmSourceNode
		implements VersionAttributeSource {
	private final JaxbHbmTimestampAttributeType timestampElement;
	private final HibernateTypeSourceImpl typeSource;

	private final List<RelationalValueSource> relationalValueSources;

	private final AttributePath attributePath;
	private final AttributeRole attributeRole;
	private final ToolingHintContext toolingHintContext;

	TimestampAttributeSourceImpl(
			MappingDocument mappingDocument,
			RootEntitySourceImpl rootEntitySource,
			JaxbHbmTimestampAttributeType timestampElement) {
		super( mappingDocument );
		this.timestampElement = timestampElement;
		this.typeSource = new HibernateTypeSourceImpl(
				"db".equals( timestampElement.getSource().value() )
						? "dbtimestamp"
						: "timestamp"
		);

		final RelationalValueSource columnSource = RelationalValueSourceHelper.buildColumnSource(
				mappingDocument,
				null,
				new RelationalValueSourceHelper.AbstractColumnsAndFormulasSource() {
					@Override
					public XmlElementMetadata getSourceType() {
						return XmlElementMetadata.TIMESTAMP;
					}

					@Override
					public String getSourceName() {
						return TimestampAttributeSourceImpl.this.getName();
					}

					@Override
					public String getColumnAttribute() {
						return TimestampAttributeSourceImpl.this.timestampElement.getColumnAttribute();
					}

					@Override
					public Boolean isNullable() {
						return false;
					}
				}
		);
		this.relationalValueSources = Collections.singletonList( columnSource );

		this.attributePath = rootEntitySource.getAttributePathBase().append( getName() );
		this.attributeRole = rootEntitySource.getAttributeRoleBase().append( getName() );

		this.toolingHintContext = Helper.collectToolingHints(
				rootEntitySource.getToolingHintContext(),
				timestampElement
		);
	}

	@Override
	public String getName() {
		return timestampElement.getName();
	}

	@Override
	public XmlElementMetadata getSourceType() {
		return XmlElementMetadata.TIMESTAMP;
	}

	@Override
	public String getXmlNodeName() {
		return timestampElement.getNode();
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
	public String getPropertyAccessorName() {
		return timestampElement.getAccess();
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return timestampElement.getGenerated();
	}

	@Override
	public Boolean isInsertable() {
		return true;
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
	public String getUnsavedValue() {
		return timestampElement.getUnsavedValue().value();
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
