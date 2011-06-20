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
package org.hibernate.type;

import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.Joinable;

/**
 * A type that represents some kind of association between entities.
 * @see org.hibernate.engine.internal.Cascade
 * @author Gavin King
 */
public interface AssociationType extends Type {

	/**
	 * Get the foreign key directionality of this association
	 */
	public ForeignKeyDirection getForeignKeyDirection();

	//TODO: move these to a new JoinableType abstract class,
	//extended by EntityType and PersistentCollectionType:

	/**
	 * Is the primary key of the owning entity table
	 * to be used in the join?
	 */
	public boolean useLHSPrimaryKey();
	/**
	 * Get the name of a property in the owning entity 
	 * that provides the join key (null if the identifier)
	 */
	public String getLHSPropertyName();
	
	/**
	 * The name of a unique property of the associated entity 
	 * that provides the join key (null if the identifier of
	 * an entity, or key of a collection)
	 */
	public String getRHSUniqueKeyPropertyName();

	/**
	 * Get the "persister" for this association - a class or
	 * collection persister
	 */
	public Joinable getAssociatedJoinable(SessionFactoryImplementor factory) throws MappingException;
	
	/**
	 * Get the entity name of the associated entity
	 */
	public String getAssociatedEntityName(SessionFactoryImplementor factory) throws MappingException;
	
	/**
	 * Get the "filtering" SQL fragment that is applied in the 
	 * SQL on clause, in addition to the usual join condition
	 */	
	public String getOnCondition(String alias, SessionFactoryImplementor factory, Map enabledFilters) 
	throws MappingException;
	
	/**
	 * Do we dirty check this association, even when there are
	 * no columns to be updated?
	 */
	public abstract boolean isAlwaysDirtyChecked();
	
	public boolean isEmbeddedInXML();
}






