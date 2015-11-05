/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
