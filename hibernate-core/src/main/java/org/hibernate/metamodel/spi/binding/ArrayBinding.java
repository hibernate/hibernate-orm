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

import org.hibernate.metamodel.spi.domain.PluralAttribute;
import org.hibernate.metamodel.spi.source.MetaAttributeContext;

/**
 * @author Brett Meyer
 */
public class ArrayBinding extends AbstractPluralAttributeBinding implements IndexedPluralAttributeBinding {

	private final PluralAttributeIndexBinding pluralAttributeIndexBinding;
	private final int base;

	public ArrayBinding(AttributeBindingContainer container,
			PluralAttribute attribute,
			PluralAttributeElementBinding.Nature pluralAttributeElementNature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName, boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext, int base) {
		super( container, attribute, pluralAttributeElementNature,
				referencedAttributeBinding, propertyAccessorName,
				includedInOptimisticLocking, metaAttributeContext );
		pluralAttributeIndexBinding = new BasicPluralAttributeIndexBinding(
				this
		);
		this.base = base;
	}

	public int base() {
		return base;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.hibernate.metamodel.spi.binding.IndexedPluralAttributeBinding#getPluralAttributeIndexBinding()
	 */
	@Override
	public PluralAttributeIndexBinding getPluralAttributeIndexBinding() {
		return pluralAttributeIndexBinding;
	}
}
