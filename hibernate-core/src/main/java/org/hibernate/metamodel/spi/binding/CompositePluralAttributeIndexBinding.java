/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
import java.util.List;
import java.util.Map;

import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.spi.MetaAttributeContext;
import org.hibernate.metamodel.spi.PluralAttributeIndexNature;
import org.hibernate.metamodel.spi.domain.Aggregate;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.tuple.component.ComponentTuplizer;

/**
 * Describes plural attributes of {@link org.hibernate.metamodel.spi.PluralAttributeElementNature#AGGREGATE} elements
 *
 * @author Gail Badner
 */
public class CompositePluralAttributeIndexBinding extends AbstractPluralAttributeIndexBinding {

	// TODO: Come up with a more descriptive name for compositeAttributeBindingContainer.
	private AbstractEmbeddableBinding compositeAttributeBindingContainer;

	public CompositePluralAttributeIndexBinding(IndexedPluralAttributeBinding binding) {
		super( binding );
	}

	@Override
	public PluralAttributeIndexNature getNature() {
		return PluralAttributeIndexNature.AGGREGATE;
	}

	public EmbeddableBinding createCompositeAttributeBindingContainer(
			final Aggregate aggregate,
			MetaAttributeContext metaAttributeContext,
			SingularAttribute parentReference,
			Class<? extends ComponentTuplizer> tuplizerClass) {
		compositeAttributeBindingContainer = new AbstractEmbeddableBinding(
				getIndexedPluralAttributeBinding().getContainer().seekEntityBinding(),
				aggregate,
				getIndexedPluralAttributeBinding().getPluralAttributeKeyBinding().getCollectionTable(),
				getIndexedPluralAttributeBinding().getAttributeRole().append( "key" ),
				getIndexedPluralAttributeBinding().getAttributePath().append( "key" ),
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
		return compositeAttributeBindingContainer;
	}

	public EmbeddableBinding getCompositeAttributeBindingContainer() {
		return compositeAttributeBindingContainer;
	}

	@Override
	public List<RelationalValueBinding> getRelationalValueBindings() {
		return compositeAttributeBindingContainer.getRelationalValueBindingContainer().relationalValueBindings();
	}

	@Override
	public List<Value> getValues() {
		return compositeAttributeBindingContainer.getRelationalValueBindingContainer().values();
	}
}
