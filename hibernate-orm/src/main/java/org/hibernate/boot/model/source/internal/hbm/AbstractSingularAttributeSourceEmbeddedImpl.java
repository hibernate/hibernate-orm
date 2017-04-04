/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.List;

import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.AttributeRole;
import org.hibernate.boot.model.source.spi.AttributeSourceContainer;
import org.hibernate.boot.model.source.spi.EmbeddableSource;
import org.hibernate.boot.model.source.spi.EmbeddedAttributeMapping;
import org.hibernate.boot.model.source.spi.HibernateTypeSource;
import org.hibernate.boot.model.source.spi.NaturalIdMutability;
import org.hibernate.boot.model.source.spi.SingularAttributeNature;
import org.hibernate.boot.model.source.spi.SingularAttributeSourceEmbedded;
import org.hibernate.boot.model.source.spi.ToolingHintContext;
import org.hibernate.tuple.GenerationTiming;

/**
 * Common base class for <component/> and <composite-id/> mappings.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSingularAttributeSourceEmbeddedImpl
		extends AbstractHbmSourceNode
		implements SingularAttributeSourceEmbedded {

	private final EmbeddedAttributeMapping jaxbEmbeddedAttributeMapping;
	private final EmbeddableSource embeddableSource;
	private NaturalIdMutability naturalIdMutability;

	protected AbstractSingularAttributeSourceEmbeddedImpl(
			final MappingDocument sourceMappingDocument,
			final AttributeSourceContainer container,
			final EmbeddedAttributeMapping embeddedAttributeMapping,
			List nestedAttributeMappings,
			boolean isDynamic,
			NaturalIdMutability naturalIdMutability,
			String logicalTableName) {
		this(
				sourceMappingDocument,
				embeddedAttributeMapping,
				new EmbeddableSourceImpl(
						sourceMappingDocument,
						new EmbeddableSourceContainer() {
							final AttributeRole role = container.getAttributeRoleBase().append(
									embeddedAttributeMapping.getName()
							);
							final AttributePath path = container.getAttributePathBase().append(
									embeddedAttributeMapping.getName()
							);
							final ToolingHintContext toolingHintContext = Helper.collectToolingHints(
									sourceMappingDocument.getToolingHintContext(),
									embeddedAttributeMapping
							);

							@Override
							public AttributeRole getAttributeRoleBase() {
								return role;
							}

							@Override
							public AttributePath getAttributePathBase() {
								return path;
							}

							@Override
							public ToolingHintContext getToolingHintContextBaselineForEmbeddable() {
								return toolingHintContext;
							}
						},
						embeddedAttributeMapping.getEmbeddableMapping(),
						nestedAttributeMappings,
						isDynamic,
						embeddedAttributeMapping.isUnique(),
						logicalTableName,
						naturalIdMutability
				),
				naturalIdMutability
		);
	}

	public AbstractSingularAttributeSourceEmbeddedImpl(
			MappingDocument sourceMappingDocument,
			EmbeddedAttributeMapping jaxbEmbeddedAttributeMapping,
			EmbeddableSource embeddableSource,
			NaturalIdMutability naturalIdMutability) {
		super( sourceMappingDocument );
		this.jaxbEmbeddedAttributeMapping = jaxbEmbeddedAttributeMapping;
		this.embeddableSource = embeddableSource;
		this.naturalIdMutability = naturalIdMutability;
	}

	@Override
	public EmbeddableSource getEmbeddableSource() {
		return embeddableSource;
	}

	@Override
	public String getName() {
		return jaxbEmbeddedAttributeMapping.getName();
	}

	@Override
	public boolean isSingular() {
		return true;
	}

	@Override
	public boolean isVirtualAttribute() {
		return false;
	}

	@Override
	public SingularAttributeNature getSingularAttributeNature() {
		return SingularAttributeNature.COMPOSITE;
	}

	@Override
	public HibernateTypeSource getTypeInformation() {
		// <component/> does not support type information.
		return null;
	}

	@Override
	public String getPropertyAccessorName() {
		return jaxbEmbeddedAttributeMapping.getAccess();
	}

	@Override
	public NaturalIdMutability getNaturalIdMutability() {
		return naturalIdMutability;
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		// todo : is this correct here?
		return null;
	}
}
