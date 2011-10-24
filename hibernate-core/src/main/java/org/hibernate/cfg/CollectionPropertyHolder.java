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
import javax.persistence.JoinTable;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;

/**
 * @author Emmanuel Bernard
 */
public class CollectionPropertyHolder extends AbstractPropertyHolder {
	Collection collection;

	public CollectionPropertyHolder(
			Collection collection,
			String path,
			XClass clazzToProcess,
			XProperty property,
			PropertyHolder parentPropertyHolder,
			Mappings mappings) {
		super( path, parentPropertyHolder, clazzToProcess, mappings );
		this.collection = collection;
		setCurrentProperty( property );
	}

	public String getClassName() {
		throw new AssertionFailure( "Collection property holder does not have a class name" );
	}

	public String getEntityOwnerClassName() {
		return null;
	}

	public Table getTable() {
		return collection.getCollectionTable();
	}

	public void addProperty(Property prop, XClass declaringClass) {
		throw new AssertionFailure( "Cannot add property to a collection" );
	}

	public KeyValue getIdentifier() {
		throw new AssertionFailure( "Identifier collection not yet managed" );
	}

	public boolean isOrWithinEmbeddedId() {
		return false;
	}

	public PersistentClass getPersistentClass() {
		return collection.getOwner();
	}

	public boolean isComponent() {
		return false;
	}

	public boolean isEntity() {
		return false;
	}

	public String getEntityName() {
		return collection.getOwner().getEntityName();
	}

	public void addProperty(Property prop, Ejb3Column[] columns, XClass declaringClass) {
		//Ejb3Column.checkPropertyConsistency( ); //already called earlier
		throw new AssertionFailure( "addProperty to a join table of a collection: does it make sense?" );
	}

	public Join addJoin(JoinTable joinTableAnn, boolean noDelayInPkColumnCreation) {
		throw new AssertionFailure( "Add a <join> in a second pass" );
	}
}
