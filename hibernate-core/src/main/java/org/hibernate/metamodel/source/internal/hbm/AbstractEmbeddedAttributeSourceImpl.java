/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal.hbm;

import java.util.Collection;
import java.util.List;

import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.source.internal.jaxb.hbm.ComponentSourceElement;
import org.hibernate.metamodel.source.spi.AttributeSourceContainer;
import org.hibernate.metamodel.source.spi.EmbeddableSource;
import org.hibernate.metamodel.source.spi.EmbeddedAttributeSource;
import org.hibernate.metamodel.source.spi.HibernateTypeSource;
import org.hibernate.metamodel.source.spi.RelationalValueSource;
import org.hibernate.metamodel.source.spi.ToolingHintSource;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.NaturalIdMutability;
import org.hibernate.metamodel.spi.SingularAttributeNature;

/**
 * Common bas class for <component/> and <composite-id/> mappings.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractEmbeddedAttributeSourceImpl
		extends AbstractHbmSourceNode
		implements EmbeddedAttributeSource {

	private final ComponentSourceElement jaxbComponentSourceElement;
	private final EmbeddableSourceImpl embeddableSource;

	protected AbstractEmbeddedAttributeSourceImpl(
			MappingDocument sourceMappingDocument,
			AttributeSourceContainer parentContainer,
			AttributeRole attributeRoleBase,
			AttributePath attributePathBase,
			ComponentSourceElement jaxbComponentSourceElement,
			EmbeddableJaxbSource embeddableJaxbSource,
			NaturalIdMutability naturalIdMutability,
			String logicalTableName) {
		super( sourceMappingDocument );
		this.jaxbComponentSourceElement = jaxbComponentSourceElement;
		this.embeddableSource = new EmbeddableSourceImpl(
				sourceMappingDocument,
				attributeRoleBase,
				attributePathBase,
				embeddableJaxbSource,
				logicalTableName,
				naturalIdMutability
		);
	}

	protected ComponentSourceElement jaxbComponentSourceElement() {
		return jaxbComponentSourceElement;
	}

	@Override
	public EmbeddableSource getEmbeddableSource() {
		return embeddableSource;
	}

	@Override
	public String getName() {
		return jaxbComponentSourceElement.getName();
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
		return jaxbComponentSourceElement.getAccess();
	}

	@Override
	public NaturalIdMutability getNaturalIdMutability() {
		return null;
	}

	@Override
	public Collection<? extends ToolingHintSource> getToolingHintSources() {
		return jaxbComponentSourceElement.getMeta();
	}

	@Override
	public PropertyGeneration getGeneration() {
		// todo : is this correct here?
		return null;
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return true;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// relational value source info comes from the individual sub-attributes

	@Override
	public String getContainingTableName() {
		return null;
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		// none, they are defined on the simple sub-attributes
		return null;
	}
}
