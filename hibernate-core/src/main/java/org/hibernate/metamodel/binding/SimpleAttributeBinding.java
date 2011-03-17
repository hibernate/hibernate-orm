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

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class SimpleAttributeBinding extends AbstractAttributeBinding implements KeyValueBinding {
	private String propertyAccessorName;
	private String cascade;
	private PropertyGeneration generation;
	private boolean insertable;
	private boolean updateable;
	private boolean optimisticLockable;
	private boolean isLazy;
	private boolean keyCasadeDeleteEnabled;
	private String unsaveValue;

	// DOM4J specific...
	private String nodeName;

	SimpleAttributeBinding(EntityBinding entityBinding) {
		super( entityBinding );
	}

	@Override
	public boolean isSimpleValue() {
		return true;
	}

	public String getPropertyAccessorName() {
		return propertyAccessorName;
	}

	public void setPropertyAccessorName(String propertyAccessorName) {
		this.propertyAccessorName = propertyAccessorName;
	}

	public String getCascade() {
		return cascade;
	}

	public void setCascade(String cascade) {
		this.cascade = cascade;
	}

	public PropertyGeneration getGeneration() {
		return generation;
	}

	public void setGeneration(PropertyGeneration generation) {
		this.generation = generation;
	}

	public boolean isInsertable() {
		return insertable;
	}

	public void setInsertable(boolean insertable) {
		this.insertable = insertable;
	}

	@Override
	public boolean isKeyCasadeDeleteEnabled() {
		return keyCasadeDeleteEnabled;
	}

	public void setKeyCasadeDeleteEnabled(boolean keyCasadeDeleteEnabled) {
		this.keyCasadeDeleteEnabled = keyCasadeDeleteEnabled;
	}

	@Override
	public String getUnsavedValue() {
		return unsaveValue;
	}

	public void setUnsaveValue(String unsaveValue) {
		this.unsaveValue = unsaveValue;
	}

	public boolean isUpdateable() {
		return updateable;
	}

	public void setUpdateable(boolean updateable) {
		this.updateable = updateable;
	}

	public boolean isOptimisticLockable() {
		return optimisticLockable;
	}

	public void setOptimisticLockable(boolean optimisticLockable) {
		this.optimisticLockable = optimisticLockable;
	}

	public boolean isLazy() {
		return isLazy;
	}

	public void setLazy(boolean lazy) {
		isLazy = lazy;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}
}
