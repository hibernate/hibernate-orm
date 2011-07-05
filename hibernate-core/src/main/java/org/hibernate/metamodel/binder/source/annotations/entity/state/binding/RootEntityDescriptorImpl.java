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
package org.hibernate.metamodel.binder.source.annotations.entity.state.binding;

import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.metamodel.binder.source.BindingContext;
import org.hibernate.metamodel.binder.source.RootEntityDescriptor;
import org.hibernate.metamodel.binder.source.TableDescriptor;
import org.hibernate.metamodel.binder.source.annotations.entity.ConfiguredClass;
import org.hibernate.metamodel.binding.Caching;
import org.hibernate.metamodel.binding.InheritanceType;

/**
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class RootEntityDescriptorImpl
		extends AbstractEntityDescriptorImpl
		implements RootEntityDescriptor {

	private boolean mutable;
	private boolean explicitPolymorphism;
	private String whereFilter;
	private String rowId;
	private OptimisticLockStyle optimisticLockStyle;

	private Caching caching;

	private TableDescriptor baseTableDescriptor;

	public RootEntityDescriptorImpl(
			ConfiguredClass configuredClass,
			String superEntityName,
			BindingContext bindingContext) {
		super( configuredClass, superEntityName, bindingContext );
		if ( configuredClass.getInheritanceType() != InheritanceType.NO_INHERITANCE ) {
			// throw exception?
		}
	}

	@Override
	public boolean isMutable() {
		return mutable;
	}

	public void setMutable(boolean mutable) {
		this.mutable = mutable;
	}

	@Override
	public boolean isExplicitPolymorphism() {
		return explicitPolymorphism;
	}

	public void setExplicitPolymorphism(boolean explicitPolymorphism) {
		this.explicitPolymorphism = explicitPolymorphism;
	}

	@Override
	public String getWhereFilter() {
		return whereFilter;
	}

	public void setWhereFilter(String whereFilter) {
		this.whereFilter = whereFilter;
	}

	@Override
	public String getRowId() {
		return rowId;
	}

	public void setRowId(String rowId) {
		this.rowId = rowId;
	}

	@Override
	public OptimisticLockStyle getOptimisticLockStyle() {
		return optimisticLockStyle;
	}

	public void setOptimisticLockType(OptimisticLockType optimisticLockType) {
		switch ( optimisticLockType ) {
			case NONE: {
				this.optimisticLockStyle = OptimisticLockStyle.NONE;
				break;
			}
			case DIRTY: {
				this.optimisticLockStyle = OptimisticLockStyle.DIRTY;
				break;
			}
			case ALL: {
				this.optimisticLockStyle = OptimisticLockStyle.ALL;
				break;
			}
			default: {
				this.optimisticLockStyle = OptimisticLockStyle.VERSION;
			}
		}
	}

	@Override
	public Caching getCaching() {
		return caching;
	}

	public void setCaching(Caching caching) {
		this.caching = caching;
	}

	@Override
	public TableDescriptor getBaseTable() {
		return baseTableDescriptor;
	}

	public void setBaseTableDescriptor(TableDescriptor baseTableDescriptor) {
		this.baseTableDescriptor = baseTableDescriptor;
	}
}
