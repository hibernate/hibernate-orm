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

import org.hibernate.metamodel.source.spi.MetaAttributeContext;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.NaturalIdMutability;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;

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
			MetaAttributeContext metaAttributeContext,
			AttributeRole attributeRole,
			AttributePath attributePath) {
		super(
				container,
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				metaAttributeContext ,
				attributeRole,
				attributePath
		);
		this.isLazy = isLazy;
		this.naturalIdMutability = naturalIdMutability;
	}

	protected abstract RelationalValueBindingContainer getRelationalValueBindingContainer();

	@Override
	public List<RelationalValueBinding> getRelationalValueBindings() {
		return getRelationalValueBindingContainer().relationalValueBindings();
	}

	@Override
	public List<Value> getValues() {
		return getRelationalValueBindingContainer().values();
	}

	@Override
	public boolean hasDerivedValue() {
		return getRelationalValueBindingContainer().hasDerivedValue();
	}

	@Override
	public boolean isNullable() {
		return !getRelationalValueBindingContainer().hasNonNullableRelationalValueBinding();
	}

	@Override
	public boolean isOptional() {
		final EntityBinding entityBinding = getContainer().seekEntityBinding();
		final TableSpecification entityPrimaryTable = entityBinding.getPrimaryTable();
		for (RelationalValueBinding relationalValueBinding : getRelationalValueBindings() ) {
			final TableSpecification table = relationalValueBinding.getTable();
			if ( table.equals( entityPrimaryTable ) ) {
				// primary table is not optional.
				return false;
			}
			final SecondaryTable secondaryTable = entityBinding.getSecondaryTables().get( table.getLogicalName() );
			// a secondaryTable can be null if it is a non-joined, collection/association table
			if ( secondaryTable == null || ! secondaryTable.isOptional() ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isIncludedInInsert() {
		return getRelationalValueBindingContainer().hasInsertableRelationalValueBinding();
	}

	@Override
	public boolean isIncludedInUpdate() {
		return getRelationalValueBindingContainer().hasUpdateableRelationalValueBinding();
	}

	@Override
	public SingularAttribute getAttribute() {
		return (SingularAttribute) super.getAttribute();
	}

	@Override
	public boolean isCascadeable() {
		return isAssociation();
	}

	@Override
	public boolean isLazy() {
		return isLazy;
	}

	@Override
	public NaturalIdMutability getNaturalIdMutability() {
		return naturalIdMutability;
	}

	public void setAlternateUniqueKey(boolean isAlternateUniqueKey) {
		super.setAlternateUniqueKey( isAlternateUniqueKey );
	}

	protected abstract void collectRelationalValueBindings(RelationalValueBindingContainer relationalValueBindingContainer);
}
