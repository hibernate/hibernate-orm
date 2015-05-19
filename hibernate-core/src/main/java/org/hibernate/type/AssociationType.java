/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Map;
import java.util.Set;

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
	 * Get the "filtering" SQL fragment that is applied in the
	 * SQL on clause, in addition to the usual join condition
	 */
	public String getOnCondition(String alias, SessionFactoryImplementor factory, Map enabledFilters, Set<String> treatAsDeclarations);
	
	/**
	 * Do we dirty check this association, even when there are
	 * no columns to be updated?
	 */
	public abstract boolean isAlwaysDirtyChecked();

	/**
	 * @deprecated To be removed in 5.  Removed as part of removing the notion of DOM entity-mode.
	 * See Jira issue: <a href="https://hibernate.onjira.com/browse/HHH-7771">HHH-7771</a>
	 */
	@Deprecated
	public boolean isEmbeddedInXML();
}






