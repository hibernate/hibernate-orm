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

import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmTuplizerType;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.AttributeRole;
import org.hibernate.boot.model.source.spi.EmbeddableMapping;
import org.hibernate.boot.model.source.spi.EmbeddableSource;
import org.hibernate.boot.model.source.spi.NaturalIdMutability;
import org.hibernate.boot.model.source.spi.PluralAttributeElementNature;
import org.hibernate.boot.model.source.spi.PluralAttributeElementSourceEmbedded;
import org.hibernate.boot.model.source.spi.ToolingHintContext;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class PluralAttributeElementSourceEmbeddedImpl
		extends AbstractHbmSourceNode
		implements PluralAttributeElementSourceEmbedded {

	private final EmbeddableSourceImpl embeddableSource;
	private final ToolingHintContext toolingHintContext;

	public PluralAttributeElementSourceEmbeddedImpl(
			MappingDocument mappingDocument,
			final AbstractPluralAttributeSourceImpl pluralAttributeSource,
			final JaxbHbmCompositeCollectionElementType jaxbCompositeElement) {
		super( mappingDocument );

		this.toolingHintContext = Helper.collectToolingHints(
				pluralAttributeSource.getToolingHintContext(),
				jaxbCompositeElement
		);

		this.embeddableSource = new EmbeddableSourceImpl(
				mappingDocument,
				new EmbeddableSourceContainer() {
					@Override
					public AttributeRole getAttributeRoleBase() {
						return pluralAttributeSource.getAttributeRole().append( "element" );
					}

					@Override
					public AttributePath getAttributePathBase() {
						return pluralAttributeSource.getAttributePath().append( "element" );
					}

					@Override
					public ToolingHintContext getToolingHintContextBaselineForEmbeddable() {
						return toolingHintContext;
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
				new EmbeddableMapping() {
					@Override
					public String getClazz() {
						return jaxbCompositeElement.getClazz();
					}

					@Override
					public List<JaxbHbmTuplizerType> getTuplizer() {
						return jaxbCompositeElement.getTuplizer();
					}

					@Override
					public String getParent() {
						return jaxbCompositeElement.getParent() == null
								? null
								: jaxbCompositeElement.getParent().getName();
					}
				},
				jaxbCompositeElement.getAttributes(),
				false,
				false,
				null,
				NaturalIdMutability.NOT_NATURAL_ID
		);
	}

	@Override
	public PluralAttributeElementNature getNature() {
		return PluralAttributeElementNature.AGGREGATE;
	}

	@Override
	public EmbeddableSource getEmbeddableSource() {
		return embeddableSource;
	}

	@Override
	public ToolingHintContext getToolingHintContext() {
		return toolingHintContext;
	}

}
