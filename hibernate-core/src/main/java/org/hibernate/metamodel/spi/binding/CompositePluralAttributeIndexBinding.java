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

import org.hibernate.metamodel.spi.domain.Aggregate;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.source.MetaAttributeContext;

/**
 * Describes plural attributes of {@link org.hibernate.metamodel.spi.binding.PluralAttributeElementBinding.Nature#AGGREGATE} elements
 *
 * @author Gail Badner
 */
public class CompositePluralAttributeIndexBinding extends AbstractPluralAttributeIndexBinding {

	// TODO: Come up with a more descriptive name for compositeAttributeBindingContainer.
	private AbstractCompositeAttributeBindingContainer compositeAttributeBindingContainer;

	public CompositePluralAttributeIndexBinding(IndexedPluralAttributeBinding binding) {
		super( binding );
	}

	@Override
	public Nature getNature() {
		return Nature.AGGREGATE;
	}

	public CompositeAttributeBindingContainer createCompositeAttributeBindingContainer(
			Aggregate aggregate,
			MetaAttributeContext metaAttributeContext,
			SingularAttribute parentReference
	) {
		compositeAttributeBindingContainer =
				new AbstractCompositeAttributeBindingContainer(
						getIndexedPluralAttributeBinding().getContainer().seekEntityBinding(),
						aggregate,
						getIndexedPluralAttributeBinding().getPluralAttributeKeyBinding().getCollectionTable(),
						aggregate.getRoleBaseName(),
						metaAttributeContext,
						parentReference
				) {
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
				};
		return compositeAttributeBindingContainer;
	}

	public CompositeAttributeBindingContainer getCompositeAttributeBindingContainer() {
		return compositeAttributeBindingContainer;
	}

	@Override
	public List<RelationalValueBinding> getRelationalValueBindings() {
		return compositeAttributeBindingContainer.getRelationalValueBindingContainer().relationalValueBindings();
	}
}
