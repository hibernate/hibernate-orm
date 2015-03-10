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

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSimpleIdType;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.AttributeRole;
import org.hibernate.boot.model.source.spi.AttributeSourceContainer;
import org.hibernate.boot.model.source.spi.NaturalIdMutability;
import org.hibernate.boot.model.source.spi.RelationalValueSource;
import org.hibernate.boot.model.source.spi.RelationalValueSourceContainer;
import org.hibernate.boot.model.source.spi.SingularAttributeNature;
import org.hibernate.boot.model.source.spi.SingularAttributeSource;
import org.hibernate.boot.model.source.spi.SizeSource;
import org.hibernate.boot.model.source.spi.ToolingHintContext;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.tuple.GenerationTiming;

/**
 * Implementation for {@code <id/>} mappings
 *
 * @author Steve Ebersole
 */
class SingularIdentifierAttributeSourceImpl
		extends AbstractHbmSourceNode
		implements SingularAttributeSource, RelationalValueSourceContainer {

	private final String name;
	private final String xmlNodeName;
	private final String accessName;

	private final HibernateTypeSourceImpl typeSource;
	private final List<RelationalValueSource> valueSources;

	private final AttributeRole attributeRole;
	private final AttributePath attributePath;

	private ToolingHintContext toolingHintContext;

	public SingularIdentifierAttributeSourceImpl(
			MappingDocument mappingDocument,
			AttributeSourceContainer container,
			final JaxbHbmSimpleIdType idElement) {
		super( mappingDocument );

		if ( StringHelper.isEmpty( idElement.getName() ) ) {
			DeprecationLogger.DEPRECATION_LOGGER.logDeprecationOfNonNamedIdAttribute( container.getAttributeRoleBase().getFullPath() );
			name = null;
		}
		else {
			name = idElement.getName();
		}

		this.xmlNodeName = idElement.getNode();
		this.accessName = idElement.getAccess();

		this.typeSource = new HibernateTypeSourceImpl( idElement );

		this.valueSources = RelationalValueSourceHelper.buildValueSources(
				sourceMappingDocument(),
				null,
				new RelationalValueSourceHelper.AbstractColumnsAndFormulasSource() {
					@Override
					public XmlElementMetadata getSourceType() {
						return XmlElementMetadata.ID;
					}

					@Override
					public String getSourceName() {
						return idElement.getName();
					}

					@Override
					public String getColumnAttribute() {
						return idElement.getColumnAttribute();
					}

					@Override
					public List getColumnOrFormulaElements() {
						return idElement.getColumn();
					}

					@Override
					public SizeSource getSizeSource() {
						return Helper.interpretSizeSource(
								idElement.getLength(),
								(Integer) null,
								null
						);
					}

					@Override
					public Boolean isNullable() {
						return false;
					}
				}
		);

		this.attributeRole = container.getAttributeRoleBase().append( name );
		this.attributePath = container.getAttributePathBase().append( name );

		this.toolingHintContext = Helper.collectToolingHints(
				container.getToolingHintContext(),
				idElement
		);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public AttributePath getAttributePath() {
		return attributePath;
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
		return accessName;
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return null;
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
	public Boolean isInsertable() {
		return true;
	}

	@Override
	public Boolean isUpdatable() {
		return false;
	}

	@Override
	public XmlElementMetadata getSourceType() {
		return XmlElementMetadata.ID;
	}

	@Override
	public String getXmlNodeName() {
		return xmlNodeName;
	}

	@Override
	public ToolingHintContext getToolingHintContext() {
		return toolingHintContext;
	}

	@Override
	public List<RelationalValueSource> getRelationalValueSources() {
		return valueSources;
	}

	@Override
	public boolean isSingular() {
		return true;
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
}
