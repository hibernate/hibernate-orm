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

import java.util.List;

import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.source.MetaAttributeContext;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSingularAttributeBinding
		extends AbstractAttributeBinding
		implements SingularAttributeBinding {

	private final boolean isLazy;
	private final NaturalIdMutability naturalIdMutability;

	protected AbstractSingularAttributeBinding(
			AttributeBindingContainer container,
			SingularAttribute attribute,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean isLazy,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext) {
		super( container, attribute, propertyAccessorName, includedInOptimisticLocking, metaAttributeContext );
		this.isLazy = isLazy;
		this.naturalIdMutability = naturalIdMutability;
	}

	@Override
	public boolean isNullable() {
		return hasNullableRelationalValueBinding( getRelationalValueBindings() );
	}

	@Override
	public boolean isIncludedInInsert() {
		return hasInsertableRelationalValueBinding( getRelationalValueBindings() );
	}

	@Override
	public boolean isIncludedInUpdate() {
		return hasUpdateableRelationalValueBinding( getRelationalValueBindings() );
	}

	protected static boolean hasNullableRelationalValueBinding(List<RelationalValueBinding> relationalValueBindings) {
		for ( RelationalValueBinding relationalValueBinding : relationalValueBindings ) {
			if ( relationalValueBinding.isNullable() ) {
				return true;
			}
		}
		return false;
	}

	protected static boolean hasInsertableRelationalValueBinding(List<RelationalValueBinding> relationalValueBindings) {
		for ( RelationalValueBinding relationalValueBinding : relationalValueBindings ) {
			if ( relationalValueBinding.isIncludeInInsert() ) {
				return true;
			}
		}
		return false;
	}

	protected static boolean hasUpdateableRelationalValueBinding(List<RelationalValueBinding> relationalValueBindings) {
		for ( RelationalValueBinding relationalValueBinding : relationalValueBindings ) {
			if ( relationalValueBinding.isIncludeInUpdate() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public SingularAttribute getAttribute() {
		return (SingularAttribute) super.getAttribute();
	}

	@Override
	public boolean isLazy() {
		return isLazy;
	}

	@Override
	public NaturalIdMutability getNaturalIdMutability() {
		return naturalIdMutability;
	}

	protected abstract void collectRelationalValueBindings(List<RelationalValueBinding> valueBindings);
}
