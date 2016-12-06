/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.spi.AttributeConverterDescriptor;
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

	/**
	 * Return true if this component is withing an @ElementCollection.
	 */
	boolean isWithinElementCollection();

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

	/**
	 * Called during binding to allow the PropertyHolder to inspect its discovered properties.  Mainly
	 * this is used in collecting attribute conversion declarations (via @Convert/@Converts).
	 *
	 * @param property The property
	 */
	void startingProperty(XProperty property);

	/**
	 * Determine the AttributeConverter to use for the given property.
	 *
	 * @param property
	 * @return
	 */
	AttributeConverterDescriptor resolveAttributeConverterDescriptor(XProperty property);
}
