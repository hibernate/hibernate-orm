/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.spi.binding;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.spi.MetaAttributeContext;
import org.hibernate.metamodel.spi.PluralAttributeElementNature;
import org.hibernate.metamodel.spi.PluralAttributeNature;
import org.hibernate.metamodel.spi.domain.Aggregate;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.tuple.component.ComponentTuplizer;

/**
 * Describes plural attributes of {@link org.hibernate.metamodel.spi.PluralAttributeElementNature#AGGREGATE} elements
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class PluralAttributeElementBindingEmbedded
		extends AbstractPluralAttributeElementBinding
		implements Cascadeable, EmbeddableBindingContributor {

	private AbstractEmbeddableBinding embeddableBinding;
	private CascadeStyle cascadeStyle;

	public PluralAttributeElementBindingEmbedded(AbstractPluralAttributeBinding binding) {
		super( binding );
	}

	@Override
	public PluralAttributeElementNature getNature() {
		return PluralAttributeElementNature.AGGREGATE;
	}

	@Override
	public RelationalValueBindingContainer getRelationalValueContainer() {
		return embeddableBinding.getRelationalValueBindingContainer();
	}

	@Override
	public CascadeStyle getCascadeStyle() {
		return cascadeStyle;
	}

	@Override
	public void setCascadeStyle(CascadeStyle cascadeStyle) {
		this.cascadeStyle = cascadeStyle;
	}

	@Override
	public EmbeddableBinding getEmbeddableBinding() {
		return embeddableBinding;

	}

	public EmbeddableBinding createBindingContainer(
			final Aggregate aggregate,
			MetaAttributeContext metaAttributeContext,
			SingularAttribute parentReference,
			Class<? extends ComponentTuplizer> tuplizerClass) {
		if ( embeddableBinding != null ) {
			throw new IllegalStateException( "EmbeddableBinding was already set" );
		}

		final boolean isMap = getPluralAttributeBinding().getAttribute().getPluralAttributeNature()
				== PluralAttributeNature.MAP;
		final String virtualAttributeName = isMap ? "value" : "element";

		embeddableBinding = new AbstractEmbeddableBinding(
				getPluralAttributeBinding().getContainer().seekEntityBinding(),
				aggregate,
				getPluralAttributeBinding().getPluralAttributeKeyBinding().getCollectionTable(),
				getPluralAttributeBinding().getAttributeRole().append( virtualAttributeName ),
				getPluralAttributeBinding().getAttributePath().append( virtualAttributeName ),
				metaAttributeContext,
				parentReference,
				tuplizerClass) {
			final Map<String,AttributeBinding> attributeBindingMap = new LinkedHashMap<String, AttributeBinding>();

			@Override
			protected boolean isModifiable() {
				return true;
			}

			@Override
			protected Map<String, AttributeBinding> attributeBindingMapInternal() {
				return attributeBindingMap;
			}

			@Override
			public boolean isAggregated() {
				return true;
			}

			@Override
			public JavaTypeDescriptor getTypeDescriptor() {
				return aggregate.getDescriptor();
			}
		};

		return embeddableBinding;
	}
}
