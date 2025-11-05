/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;

import jakarta.persistence.Column;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;

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

	void addProperty(Property prop, AnnotatedColumns columns, XClass declaringClass);

	void movePropertyToJoin(Property prop, Join join, XClass declaringClass);

	KeyValue getIdentifier();

	/**
	 * Return true if this component is or is embedded in a @EmbeddedId
	 */
	boolean isOrWithinEmbeddedId();

	/**
	 * Return true if this component is within an @ElementCollection.
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
	 * return null if hte foreign key is not overridden, or the foreign key if true
	 */
	default ForeignKey getOverriddenForeignKey(String propertyName) {
		// todo: does this necessarily need to be a default method?
		return null;
	}

	ColumnTransformer getOverriddenColumnTransformer(String logicalColumnName);

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
	 * @return The ConverterDescriptor
	 */
	ConverterDescriptor resolveAttributeConverterDescriptor(XProperty property);
}
