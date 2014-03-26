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
package org.hibernate.metamodel.source.internal.annotations;

import java.util.List;

import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.annotations.entity.EmbeddableTypeMetadata;
import org.hibernate.metamodel.source.spi.AttributeSource;
import org.hibernate.metamodel.source.spi.EmbeddableSource;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.LocalBindingContext;

/**
 * @author Steve Ebersole
 */
public class EmbeddableSourceImpl implements EmbeddableSource {
	private final EmbeddableTypeMetadata embeddableTypeMetadata;

	private final List<AttributeSource> attributeSources;

	public EmbeddableSourceImpl(
			EmbeddableTypeMetadata embeddableTypeMetadata,
			SourceHelper.AttributeBuilder attributeBuilder) {
		this.embeddableTypeMetadata = embeddableTypeMetadata;
		this.attributeSources = SourceHelper.buildAttributeSources( embeddableTypeMetadata, attributeBuilder );
	}

	protected EmbeddableTypeMetadata getEmbeddableTypeMetadata() {
		return embeddableTypeMetadata;
	}

	@Override
	public AttributePath getAttributePathBase() {
		return embeddableTypeMetadata.getAttributePathBase();
	}

	@Override
	public AttributeRole getAttributeRoleBase() {
		return embeddableTypeMetadata.getAttributeRoleBase();
	}

	@Override
	public List<AttributeSource> attributeSources() {
		return attributeSources;
	}

	@Override
	public JavaTypeDescriptor getTypeDescriptor() {
		return embeddableTypeMetadata.getJavaTypeDescriptor();
	}

	@Override
	public String getParentReferenceAttributeName() {
		return embeddableTypeMetadata.getParentReferencingAttributeName();
	}

	@Override
	public String getExplicitTuplizerClassName() {
		return embeddableTypeMetadata.getCustomTuplizerClassName();
	}

	@Override
	public LocalBindingContext getLocalBindingContext() {
		return embeddableTypeMetadata.getLocalBindingContext();
	}
}
