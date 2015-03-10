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

import java.util.Collections;
import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeIndexType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmMapKeyCompositeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmTuplizerType;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.AttributeRole;
import org.hibernate.boot.model.source.spi.EmbeddableMapping;
import org.hibernate.boot.model.source.spi.EmbeddableSource;
import org.hibernate.boot.model.source.spi.HibernateTypeSource;
import org.hibernate.boot.model.source.spi.NaturalIdMutability;
import org.hibernate.boot.model.source.spi.PluralAttributeIndexNature;
import org.hibernate.boot.model.source.spi.PluralAttributeMapKeySourceEmbedded;
import org.hibernate.boot.model.source.spi.ToolingHintContext;

/**
 * @author Gail Badner
 */
public class PluralAttributeMapKeySourceEmbeddedImpl
		extends AbstractHbmSourceNode
		implements PluralAttributeMapKeySourceEmbedded {

	private final EmbeddableSourceImpl embeddableSource;

	public PluralAttributeMapKeySourceEmbeddedImpl(
			MappingDocument mappingDocument,
			AbstractPluralAttributeSourceImpl pluralAttributeSource,
			final JaxbHbmCompositeIndexType jaxbCompositeIndexElement) {
		this(
				mappingDocument,
				pluralAttributeSource,
				new EmbeddableMapping() {
					@Override
					public String getClazz() {
						return jaxbCompositeIndexElement.getClazz();
					}

					@Override
					public List<JaxbHbmTuplizerType> getTuplizer() {
						return Collections.emptyList();
					}

					@Override
					public String getParent() {
						return null;
					}
				},
				jaxbCompositeIndexElement.getAttributes()
		);
	}

	public PluralAttributeMapKeySourceEmbeddedImpl(
			MappingDocument mappingDocument,
			AbstractPluralAttributeSourceImpl pluralAttributeSource,
			final JaxbHbmMapKeyCompositeType jaxbCompositeMapKey) {
		this(
				mappingDocument,
				pluralAttributeSource,
				new EmbeddableMapping() {
					@Override
					public String getClazz() {
						return jaxbCompositeMapKey.getClazz();
					}

					@Override
					public List<JaxbHbmTuplizerType> getTuplizer() {
						return Collections.emptyList();
					}

					@Override
					public String getParent() {
						return null;
					}
				},
				jaxbCompositeMapKey.getAttributes()
		);
	}

	private PluralAttributeMapKeySourceEmbeddedImpl(
			MappingDocument mappingDocument,
			final AbstractPluralAttributeSourceImpl pluralAttributeSource,
			EmbeddableMapping jaxbEmbeddable,
			List attributeMappings) {
		super( mappingDocument );
		this.embeddableSource = new EmbeddableSourceImpl(
				mappingDocument,
				new EmbeddableSourceContainer() {
					@Override
					public AttributeRole getAttributeRoleBase() {
						return pluralAttributeSource.getAttributeRole().append( "key" );
					}

					@Override
					public AttributePath getAttributePathBase() {
						return pluralAttributeSource.getAttributePath().append( "key" );
					}

					@Override
					public ToolingHintContext getToolingHintContextBaselineForEmbeddable() {
						return pluralAttributeSource.getToolingHintContext();
					}

					@Override
					public void registerIndexConstraintColumn(
							String constraintName,
							String logicalTableName,
							String columnName) {
						// todo : how should this be handled?
					}

					@Override
					public void registerUniqueKeyConstraintColumn(
							String constraintName,
							String logicalTableName,
							String columnName) {
						// todo : how should this be handled?
					}
				},
				jaxbEmbeddable,
				attributeMappings,
				false,
				false,
				null,
				NaturalIdMutability.NOT_NATURAL_ID
		);
	}

	@Override
	public PluralAttributeIndexNature getNature() {
		return PluralAttributeIndexNature.AGGREGATE;
	}

	@Override
	public EmbeddableSource getEmbeddableSource() {
		return embeddableSource;
	}

	@Override
	public HibernateTypeSource getTypeInformation() {
		return null;
	}

	@Override
	public String getXmlNodeName() {
		return null;
	}

}
