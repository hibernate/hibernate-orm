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

import org.dom4j.Element;

import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.source.util.DomHelper;

/**
 * TODO : javadoc
 *
 * @author Gail Badner
 */
public abstract class SingularAttributeBinding extends AbstractAttributeBinding implements KeyValueBinding {
	public static interface DomainState extends AbstractAttributeBinding.DomainState {
		boolean isInsertable();
		boolean isUpdateable();
		boolean isKeyCasadeDeleteEnabled();
		String getUnsavedValue();
	}

	private boolean insertable;
	private boolean updateable;
	private boolean keyCasadeDeleteEnabled;
	private String unsavedValue;

	SingularAttributeBinding(EntityBinding entityBinding) {
		super( entityBinding );
	}

	public final void initialize(DomainState state) {
		super.initialize( state );
		insertable = state.isInsertable();
		updateable = state.isUpdateable();
		keyCasadeDeleteEnabled = state.isKeyCasadeDeleteEnabled();
		unsavedValue = state.getUnsavedValue();
	}

	public boolean isInsertable() {
		return insertable;
	}

	protected void setInsertable(boolean insertable) {
		this.insertable = insertable;
	}

	public boolean isUpdateable() {
		return updateable;
	}

	protected void setUpdateable(boolean updateable) {
		this.updateable = updateable;
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
		return unsavedValue;
	}

	public void setUnsavedValue(String unsaveValue) {
		this.unsavedValue = unsaveValue;
	}
}
