/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
