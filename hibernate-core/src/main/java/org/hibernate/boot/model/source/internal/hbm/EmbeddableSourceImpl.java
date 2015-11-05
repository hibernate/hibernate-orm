/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmTuplizerType;
import org.hibernate.boot.jaxb.hbm.spi.ToolingHintContainer;
import org.hibernate.boot.model.JavaTypeDescriptor;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.AttributeRole;
import org.hibernate.boot.model.source.spi.AttributeSource;
import org.hibernate.boot.model.source.spi.AttributeSourceContainer;
import org.hibernate.boot.model.source.spi.EmbeddableMapping;
import org.hibernate.boot.model.source.spi.EmbeddableSource;
import org.hibernate.boot.model.source.spi.LocalMetadataBuildingContext;
import org.hibernate.boot.model.source.spi.NaturalIdMutability;
import org.hibernate.boot.model.source.spi.ToolingHintContext;
import org.hibernate.internal.log.DeprecationLogger;

/**
 * @author Steve Ebersole
 */
public class EmbeddableSourceImpl extends AbstractHbmSourceNode implements EmbeddableSource {
	private final EmbeddableMapping jaxbEmbeddableMapping;
	private final JavaTypeDescriptor typeDescriptor;

	private final AttributeRole attributeRoleBase;
	private final AttributePath attributePathBase;
	private final ToolingHintContext toolingHintContext;

	private final boolean isDynamic;
	private final boolean isUnique;

	private final Map<EntityMode,String> tuplizerClassMap;

	private final List<AttributeSource> attributeSources;

	public EmbeddableSourceImpl(
			MappingDocument mappingDocument,
			EmbeddableSourceContainer container,
			EmbeddableMapping jaxbEmbeddableMapping,
			List attributeMappings,
			boolean isDynamic,
			boolean isUnique,
			String logicalTableName,
			NaturalIdMutability naturalIdMutability) {
		super( mappingDocument );
		this.attributeRoleBase = container.getAttributeRoleBase();
		this.attributePathBase = container.getAttributePathBase();
		if ( ToolingHintContainer.class.isInstance( jaxbEmbeddableMapping ) ) {
			this.toolingHintContext = Helper.collectToolingHints(
					container.getToolingHintContextBaselineForEmbeddable(),
					(ToolingHintContainer) jaxbEmbeddableMapping
			);
		}
		else {
			this.toolingHintContext = container.getToolingHintContextBaselineForEmbeddable();
		}

		this.jaxbEmbeddableMapping = jaxbEmbeddableMapping;
		this.isDynamic = isDynamic;
		this.isUnique = isUnique;

		final String typeName = isDynamic
				? jaxbEmbeddableMapping.getClazz()
				: mappingDocument.qualifyClassName( jaxbEmbeddableMapping.getClazz() );
		this.typeDescriptor = new JavaTypeDescriptor() {
			@Override
			public String getName() {
				return typeName;
			}
		};

		if ( jaxbEmbeddableMapping.getTuplizer().isEmpty() ) {
			tuplizerClassMap = Collections.emptyMap();
		}
		else {
			if ( jaxbEmbeddableMapping.getTuplizer().size() > 1 ) {
				DeprecationLogger.DEPRECATION_LOGGER.logDeprecationOfMultipleEntityModeSupport();
			}

			tuplizerClassMap = new HashMap<EntityMode, String>(  );
			for ( JaxbHbmTuplizerType tuplizerBinding : jaxbEmbeddableMapping.getTuplizer() ) {
				tuplizerClassMap.put(
						tuplizerBinding.getEntityMode(),
						tuplizerBinding.getClazz()
				);
			}
		}

		this.attributeSources = new ArrayList<AttributeSource>();
		AttributesHelper.processAttributes(
				mappingDocument,
				new AttributesHelper.Callback() {
					@Override
					public AttributeSourceContainer getAttributeSourceContainer() {
						return EmbeddableSourceImpl.this;
					}

					@Override
					public void addAttributeSource(AttributeSource attributeSource) {
						attributeSources.add( attributeSource );
					}
				},
				attributeMappings,
				logicalTableName,
				naturalIdMutability
		);
	}

	@Override
	public JavaTypeDescriptor getTypeDescriptor() {
		return typeDescriptor;
	}

	@Override
	public String getParentReferenceAttributeName() {
		return jaxbEmbeddableMapping.getParent();
	}

	@Override
	public Map<EntityMode,String> getTuplizerClassMap() {
		return tuplizerClassMap;
	}

	@Override
	public boolean isDynamic() {
		return isDynamic;
	}

	@Override
	public boolean isUnique() {
		return isUnique;
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
	public List<AttributeSource> attributeSources() {
		return attributeSources;
	}

	@Override
	public LocalMetadataBuildingContext getLocalMetadataBuildingContext() {
		return metadataBuildingContext();
	}

	@Override
	public ToolingHintContext getToolingHintContext() {
		return toolingHintContext;
	}
}
