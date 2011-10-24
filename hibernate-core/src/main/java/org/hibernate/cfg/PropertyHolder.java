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
package org.hibernate.cfg;
import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;

/**
 * Property holder abstract property containers from their direct implementation
 *
 * @author Emmanuel Bernard
 */
public interface PropertyHolder {
	String getClassName();

	String getEntityOwnerClassName();

	Table getTable();

	void addProperty(Property prop, XClass declaringClass);

	void addProperty(Property prop, Ejb3Column[] columns, XClass declaringClass);

	KeyValue getIdentifier();

	/**
	 * Return true if this component is or is embedded in a @EmbeddedId
	 */
	boolean isOrWithinEmbeddedId();

	PersistentClass getPersistentClass();

	boolean isComponent();

	boolean isEntity();

	void setParentProperty(String parentProperty);

	String getPath();

	/**
	 * return null if the column is not overridden, or an array of column if true
	 */
	Column[] getOverriddenColumn(String propertyName);

	/**
	 * return null if the column is not overridden, or an array of column if true
	 */
	JoinColumn[] getOverriddenJoinColumn(String propertyName);

	/**
	 * return
	 *  - null if no join table is present,
	 *  - the join table if not overridden,
	 *  - the overridden join table otherwise
	 */
	JoinTable getJoinTable(XProperty property);

	String getEntityName();

	Join addJoin(JoinTable joinTableAnn, boolean noDelayInPkColumnCreation);

	boolean isInIdClass();

	void setInIdClass(Boolean isInIdClass);
}
