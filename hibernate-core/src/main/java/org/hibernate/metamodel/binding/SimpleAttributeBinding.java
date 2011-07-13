/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.binding;

import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.domain.AbstractAttributeContainer;
import org.hibernate.metamodel.source.MetaAttributeContext;
import org.hibernate.metamodel.binding.state.SimpleAttributeBindingState;
import org.hibernate.metamodel.domain.SingularAttribute;
import org.hibernate.metamodel.relational.state.ColumnRelationalState;
import org.hibernate.metamodel.relational.state.ValueRelationalState;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class SimpleAttributeBinding extends AbstractAttributeBinding implements KeyValueBinding {
	private boolean insertable;
	private boolean updatable;
	private PropertyGeneration generation;

	private String propertyAccessorName;
	private String unsavedValue;

	private boolean forceNonNullable;
	private boolean forceUnique;
	private boolean keyCascadeDeleteEnabled;

	private boolean includedInOptimisticLocking;
	private MetaAttributeContext metaAttributeContext;

	SimpleAttributeBinding(EntityBinding entityBinding, boolean forceNonNullable, boolean forceUnique) {
		super( entityBinding );
		this.forceNonNullable = forceNonNullable;
		this.forceUnique = forceUnique;
	}

	public final SimpleAttributeBinding initialize(SimpleAttributeBindingState state) {
		super.initialize( state );
		insertable = state.isInsertable();
		updatable = state.isUpdatable();
		keyCascadeDeleteEnabled = state.isKeyCascadeDeleteEnabled();
		unsavedValue = state.getUnsavedValue();
		generation = state.getPropertyGeneration() == null ? PropertyGeneration.NEVER : state.getPropertyGeneration();
		return this;
	}

	public SimpleAttributeBinding initialize(ValueRelationalState state) {
		super.initializeValueRelationalState( state );
		return this;
	}

	private boolean isUnique(ColumnRelationalState state) {
		return isPrimaryKey() || state.isUnique();
	}

	@Override
	public SingularAttribute getAttribute() {
		return (SingularAttribute) super.getAttribute();
	}

	@Override
	public boolean isSimpleValue() {
		return true;
	}

	public boolean isInsertable() {
		return insertable;
	}

	public void setInsertable(boolean insertable) {
		this.insertable = insertable;
	}

	public boolean isUpdatable() {
		return updatable;
	}

	public void setUpdatable(boolean updatable) {
		this.updatable = updatable;
	}

	@Override
	public boolean isKeyCascadeDeleteEnabled() {
		return keyCascadeDeleteEnabled;
	}

	public void setKeyCascadeDeleteEnabled(boolean keyCascadeDeleteEnabled) {
		this.keyCascadeDeleteEnabled = keyCascadeDeleteEnabled;
	}

	@Override
	public String getUnsavedValue() {
		return unsavedValue;
	}

	public void setUnsavedValue(String unsaveValue) {
		this.unsavedValue = unsaveValue;
	}

	public boolean forceNonNullable() {
		return forceNonNullable;
	}

	public boolean forceUnique() {
		return forceUnique;
	}

	public PropertyGeneration getGeneration() {
		return generation;
	}

	public void setGeneration(PropertyGeneration generation) {
		this.generation = generation;
	}

	public String getPropertyAccessorName() {
		return propertyAccessorName;
	}

	public void setPropertyAccessorName(String propertyAccessorName) {
		this.propertyAccessorName = propertyAccessorName;
	}

	public boolean isIncludedInOptimisticLocking() {
		return includedInOptimisticLocking;
	}

	public void setIncludedInOptimisticLocking(boolean includedInOptimisticLocking) {
		this.includedInOptimisticLocking = includedInOptimisticLocking;
	}

	public MetaAttributeContext getMetaAttributeContext() {
		return metaAttributeContext;
	}

	public void setMetaAttributeContext(MetaAttributeContext metaAttributeContext) {
		this.metaAttributeContext = metaAttributeContext;
	}
}
