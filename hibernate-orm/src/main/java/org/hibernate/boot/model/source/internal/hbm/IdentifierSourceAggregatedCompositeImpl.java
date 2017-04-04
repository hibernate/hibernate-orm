/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.Collections;
import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeIdType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmToolingHintType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmTuplizerType;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.AttributeRole;
import org.hibernate.boot.model.source.spi.EmbeddableMapping;
import org.hibernate.boot.model.source.spi.EmbeddableSource;
import org.hibernate.boot.model.source.spi.EmbeddedAttributeMapping;
import org.hibernate.boot.model.source.spi.IdentifierSourceAggregatedComposite;
import org.hibernate.boot.model.source.spi.MapsIdSource;
import org.hibernate.boot.model.source.spi.NaturalIdMutability;
import org.hibernate.boot.model.source.spi.SingularAttributeSourceEmbedded;
import org.hibernate.boot.model.source.spi.ToolingHintContext;
import org.hibernate.id.EntityIdentifierNature;

/**
 * Models a {@code <composite-id/>} mapping where we have a named (embeddable) attribute.
 *
 * @author Steve Ebersole
 */
class IdentifierSourceAggregatedCompositeImpl implements IdentifierSourceAggregatedComposite {
	private final SingularAttributeSourceAggregatedCompositeIdentifierImpl attributeSource;
	private final IdentifierGeneratorDefinition generatorDefinition;
	private final ToolingHintContext toolingHintContext;

	public IdentifierSourceAggregatedCompositeImpl(final RootEntitySourceImpl rootEntitySource) {
		final EmbeddedAttributeMappingAdapterAggregatedCompositeId compositeIdAdapter =
				new EmbeddedAttributeMappingAdapterAggregatedCompositeId( rootEntitySource );

		this.attributeSource = new SingularAttributeSourceAggregatedCompositeIdentifierImpl(
				rootEntitySource.sourceMappingDocument(),
				compositeIdAdapter
		);
		this.generatorDefinition = EntityHierarchySourceImpl.interpretGeneratorDefinition(
				rootEntitySource.sourceMappingDocument(),
				rootEntitySource.getEntityNamingSource(),
				rootEntitySource.jaxbEntityMapping().getCompositeId().getGenerator()
		);

		this.toolingHintContext = Helper.collectToolingHints(
				rootEntitySource.getToolingHintContext(),
				rootEntitySource.jaxbEntityMapping().getCompositeId()
		);

	}

	@Override
	public SingularAttributeSourceEmbedded getIdentifierAttributeSource() {
		return attributeSource;
	}

	@Override
	public List<MapsIdSource> getMapsIdSources() {
		return Collections.emptyList();
	}

	@Override
	public IdentifierGeneratorDefinition getIndividualAttributeIdGenerator(String identifierAttributeName) {
		// for now, return null.  this is that stupid specj bs
		return null;
	}

	@Override
	public IdentifierGeneratorDefinition getIdentifierGeneratorDescriptor() {
		return generatorDefinition;
	}

	@Override
	public EntityIdentifierNature getNature() {
		return EntityIdentifierNature.AGGREGATED_COMPOSITE;
	}

	@Override
	public EmbeddableSource getEmbeddableSource() {
		return attributeSource.getEmbeddableSource();
	}

	@Override
	public ToolingHintContext getToolingHintContext() {
		return toolingHintContext;
	}

	private static class SingularAttributeSourceAggregatedCompositeIdentifierImpl
			extends AbstractSingularAttributeSourceEmbeddedImpl {
		private final EmbeddedAttributeMappingAdapterAggregatedCompositeId compositeIdAdapter;

		protected SingularAttributeSourceAggregatedCompositeIdentifierImpl(
				MappingDocument mappingDocument,
				EmbeddedAttributeMappingAdapterAggregatedCompositeId compositeIdAdapter) {
			super(
					mappingDocument,
					compositeIdAdapter,
					new EmbeddableSourceImpl(
							mappingDocument,
							compositeIdAdapter,
							compositeIdAdapter,
							compositeIdAdapter.getAttributes(),
							false,
							false,
							null,
							NaturalIdMutability.NOT_NATURAL_ID
					),
					NaturalIdMutability.NOT_NATURAL_ID
			);
			this.compositeIdAdapter = compositeIdAdapter;
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
		public XmlElementMetadata getSourceType() {
			return XmlElementMetadata.COMPOSITE_ID;
		}

		@Override
		public String getXmlNodeName() {
			return compositeIdAdapter.getXmlNodeName();
		}

		@Override
		public AttributePath getAttributePath() {
			return getEmbeddableSource().getAttributePathBase();
		}

		@Override
		public AttributeRole getAttributeRole() {
			return getEmbeddableSource().getAttributeRoleBase();
		}

		@Override
		public boolean isIncludedInOptimisticLocking() {
			return false;
		}

		@Override
		public ToolingHintContext getToolingHintContext() {
			return compositeIdAdapter.toolingHintContext;
		}
	}

	private static class EmbeddedAttributeMappingAdapterAggregatedCompositeId
			implements EmbeddedAttributeMapping, EmbeddableSourceContainer, EmbeddableMapping {
		private final RootEntitySourceImpl rootEntitySource;
		private final JaxbHbmCompositeIdType jaxbCompositeIdMapping;

		private final AttributeRole idAttributeRole;
		private final AttributePath idAttributePath;
		private final ToolingHintContext toolingHintContext;

		private EmbeddedAttributeMappingAdapterAggregatedCompositeId(
				RootEntitySourceImpl rootEntitySource) {
			this.rootEntitySource = rootEntitySource;
			this.jaxbCompositeIdMapping = rootEntitySource.jaxbEntityMapping().getCompositeId();

			this.idAttributeRole = rootEntitySource.getAttributeRoleBase().append( jaxbCompositeIdMapping.getName() );
			this.idAttributePath = rootEntitySource.getAttributePathBase().append( jaxbCompositeIdMapping.getName() );

			this.toolingHintContext = Helper.collectToolingHints(
					rootEntitySource.getToolingHintContext(),
					jaxbCompositeIdMapping
			);
		}

		@Override
		public String getName() {
			return jaxbCompositeIdMapping.getName();
		}

		@Override
		public String getAccess() {
			return jaxbCompositeIdMapping.getAccess();
		}

		@Override
		public String getClazz() {
			return jaxbCompositeIdMapping.getClazz();
		}

		@Override
		public List<JaxbHbmTuplizerType> getTuplizer() {
			return Collections.emptyList();
		}

		@Override
		public String getParent() {
			return null;
		}

		@Override
		public List<JaxbHbmToolingHintType> getToolingHints() {
			return jaxbCompositeIdMapping.getToolingHints();
		}

		@Override
		public AttributeRole getAttributeRoleBase() {
			return idAttributeRole;
		}

		@Override
		public AttributePath getAttributePathBase() {
			return idAttributePath;
		}

		@Override
		public ToolingHintContext getToolingHintContextBaselineForEmbeddable() {
			return toolingHintContext;
		}

		public List getAttributes() {
			return jaxbCompositeIdMapping.getKeyPropertyOrKeyManyToOne();
		}

		public String getXmlNodeName() {
			return jaxbCompositeIdMapping.getNode();
		}

		@Override
		public boolean isUnique() {
			return false;
		}

		@Override
		public EmbeddableMapping getEmbeddableMapping() {
			return this;
		}
	}
}
