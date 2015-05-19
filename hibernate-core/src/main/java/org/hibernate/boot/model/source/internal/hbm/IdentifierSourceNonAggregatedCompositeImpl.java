/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeIdType;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.JavaTypeDescriptor;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.AttributeRole;
import org.hibernate.boot.model.source.spi.AttributeSource;
import org.hibernate.boot.model.source.spi.AttributeSourceContainer;
import org.hibernate.boot.model.source.spi.EmbeddableSource;
import org.hibernate.boot.model.source.spi.IdentifierSourceNonAggregatedComposite;
import org.hibernate.boot.model.source.spi.LocalMetadataBuildingContext;
import org.hibernate.boot.model.source.spi.SingularAttributeSource;
import org.hibernate.boot.model.source.spi.ToolingHintContext;
import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.internal.util.StringHelper;

/**
 * Models a composite identifier with is not not encapsulated in a dedicated "id class".
 *
 * @author Steve Ebersole
 */
class IdentifierSourceNonAggregatedCompositeImpl implements IdentifierSourceNonAggregatedComposite, EmbeddableSource {
	private final RootEntitySourceImpl rootEntitySource;

	private final AttributePath attributePathBase;
	private final AttributeRole attributeRoleBase;
	private final IdentifierGeneratorDefinition generatorDefinition;

	// NOTE: not typed because we need to expose as both:
	// 		List<AttributeSource>
	//		List<SingularAttributeSource>
	// :(
	private final List attributeSources;

	private final EmbeddableSource idClassSource;
	private final ToolingHintContext toolingHintContext;

	IdentifierSourceNonAggregatedCompositeImpl(RootEntitySourceImpl rootEntitySource) {
		this.rootEntitySource = rootEntitySource;

		this.attributePathBase = rootEntitySource.getAttributePathBase().append( "<id>" );
		this.attributeRoleBase = rootEntitySource.getAttributeRoleBase().append( "<id>" );
		this.generatorDefinition = EntityHierarchySourceImpl.interpretGeneratorDefinition(
				rootEntitySource.sourceMappingDocument(),
				rootEntitySource.getEntityNamingSource(),
				rootEntitySource.jaxbEntityMapping().getCompositeId().getGenerator()
		);

		this.attributeSources = new ArrayList();
		AttributesHelper.processCompositeKeySubAttributes(
				rootEntitySource.sourceMappingDocument(),
				new AttributesHelper.Callback() {
					@Override
					public AttributeSourceContainer getAttributeSourceContainer() {
						return IdentifierSourceNonAggregatedCompositeImpl.this;
					}

					@Override
					@SuppressWarnings("unchecked")
					public void addAttributeSource(AttributeSource attributeSource) {
						attributeSources.add( attributeSource );
					}

					@Override
					public void registerIndexColumn(
							String constraintName,
							String logicalTableName,
							String columnName) {
						// todo : determine the best option here...
						//		probably (here) delegate back to root entity, but need a general strategy
					}

					@Override
					public void registerUniqueKeyColumn(
							String constraintName,
							String logicalTableName,
							String columnName) {
						// todo : determine the best option here...
						//		probably (here) delegate back to root entity, but need a general strategy
					}
				},
				rootEntitySource.jaxbEntityMapping().getCompositeId().getKeyPropertyOrKeyManyToOne()
		);

		// NOTE : the HBM support for IdClass is very limited.  Essentially
		// we assume that all identifier attributes occur in the IdClass
		// using the same name and type.
		this.idClassSource = interpretIdClass(
				rootEntitySource.sourceMappingDocument(),
				rootEntitySource.jaxbEntityMapping().getCompositeId()
		);

		this.toolingHintContext = Helper.collectToolingHints(
				rootEntitySource.getToolingHintContext(),
				rootEntitySource.jaxbEntityMapping().getCompositeId()
		);
	}

	private EmbeddableSource interpretIdClass(
			MappingDocument mappingDocument,
			JaxbHbmCompositeIdType jaxbHbmCompositeIdMapping) {
		// if <composite-id/> is null here we have much bigger problems :)

		if ( !jaxbHbmCompositeIdMapping.isMapped() ) {
			return null;
		}

		final String className = jaxbHbmCompositeIdMapping.getClazz();
		if ( StringHelper.isEmpty( className ) ) {
			return null;
		}

		final String idClassQualifiedName = mappingDocument.qualifyClassName( className );
		final JavaTypeDescriptor idClassTypeDescriptor = new JavaTypeDescriptor() {
			@Override
			public String getName() {
				return idClassQualifiedName;
			}
		};
		return new IdClassSource( idClassTypeDescriptor, rootEntitySource, mappingDocument );
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<SingularAttributeSource> getAttributeSourcesMakingUpIdentifier() {
		return attributeSources;
	}


	@Override
	public EmbeddableSource getIdClassSource() {
		return idClassSource;
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
		return EntityIdentifierNature.NON_AGGREGATED_COMPOSITE;
	}

	@Override
	public JavaTypeDescriptor getTypeDescriptor() {
		return null;
	}

	@Override
	public String getParentReferenceAttributeName() {
		return null;
	}

	@Override
	public Map<EntityMode, String> getTuplizerClassMap() {
		return null;
	}

	@Override
	public boolean isDynamic() {
		return false;
	}

	@Override
	public boolean isUnique() {
		return false;
	}

	@Override
	public AttributePath getAttributePathBase() {
		return attributePathBase;
	}

	@Override
	public AttributeRole getAttributeRoleBase() {
		return attributeRoleBase;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<AttributeSource> attributeSources() {
		return attributeSources;
	}

	@Override
	public LocalMetadataBuildingContext getLocalMetadataBuildingContext() {
		return rootEntitySource.metadataBuildingContext();
	}

	@Override
	public EmbeddableSource getEmbeddableSource() {
		return this;
	}

	@Override
	public ToolingHintContext getToolingHintContext() {
		return toolingHintContext;
	}
}
