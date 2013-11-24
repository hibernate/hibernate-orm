/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.hql.internal.ast.util;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.internal.JoinSequence;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.NameGenerator;
import org.hibernate.hql.internal.ast.DetailedSemanticException;
import org.hibernate.hql.internal.ast.QuerySyntaxException;
import org.hibernate.hql.internal.ast.tree.SqlNode;
import org.hibernate.persister.collection.CollectionPropertyMapping;
import org.hibernate.persister.collection.CollectionPropertyNames;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.JoinType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import antlr.SemanticException;
import antlr.collections.AST;

/**
 * Helper for performing common and/or complex operations with the
 * {@link SessionFactoryImplementor} during translation of an HQL query.
 *
 * @author Joshua Davis
 */
public class SessionFactoryHelper {

	private final SessionFactoryImplementor sfi;
	private final Map<String, PropertyMapping> collectionPropertyMappingByRole;

	/**
	 * Construct a new SessionFactoryHelper instance.
	 *
	 * @param sfi The SessionFactory impl to be encapsulated.
	 */
	public SessionFactoryHelper(SessionFactoryImplementor sfi) {
		this.sfi = sfi;
		this.collectionPropertyMappingByRole = new HashMap<String, PropertyMapping>();
	}

	/**
	 * Get a handle to the encapsulated SessionFactory.
	 *
	 * @return The encapsulated SessionFactory.
	 */
	public SessionFactoryImplementor getFactory() {
		return sfi;
	}

	/**
	 * Does the given persister define a physical discriminator column
	 * for the purpose of inheritance discrimination?
	 *
	 * @param persister The persister to be checked.
	 *
	 * @return True if the persister does define an actual discriminator column.
	 */
	public boolean hasPhysicalDiscriminatorColumn(Queryable persister) {
		if ( persister.getDiscriminatorType() != null ) {
			String discrimColumnName = persister.getDiscriminatorColumnName();
			// Needed the "clazz_" check to work around union-subclasses
			// TODO : is there a way to tell whether a persister is truly discrim-column based inheritence?
			if ( discrimColumnName != null && !"clazz_".equals( discrimColumnName ) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Given a (potentially unqualified) class name, locate its imported qualified name.
	 *
	 * @param className The potentially unqualified class name
	 *
	 * @return The qualified class name.
	 */
	public String getImportedClassName(String className) {
		return sfi.getImportedClassName( className );
	}

	/**
	 * Given a (potentially unqualified) class name, locate its persister.
	 *
	 * @param className The (potentially unqualified) class name.
	 *
	 * @return The defined persister for this class, or null if none found.
	 */
	public Queryable findQueryableUsingImports(String className) {
		return findQueryableUsingImports( sfi, className );
	}


	/**
	 * Given a (potentially unqualified) class name, locate its persister.
	 *
	 * @param sfi The session factory implementor.
	 * @param className The (potentially unqualified) class name.
	 *
	 * @return The defined persister for this class, or null if none found.
	 */
	public static Queryable findQueryableUsingImports(SessionFactoryImplementor sfi, String className) {
		final String importedClassName = sfi.getImportedClassName( className );
		if ( importedClassName == null ) {
			return null;
		}
		try {
			return (Queryable) sfi.getEntityPersister( importedClassName );
		}
		catch ( MappingException me ) {
			return null;
		}
	}

	/**
	 * Locate the persister by class or entity name.
	 *
	 * @param name The class or entity name
	 *
	 * @return The defined persister for this entity, or null if none found.
	 *
	 * @throws MappingException
	 */
	private EntityPersister findEntityPersisterByName(String name) throws MappingException {
		// First, try to get the persister using the given name directly.
		try {
			return sfi.getEntityPersister( name );
		}
		catch ( MappingException ignore ) {
			// unable to locate it using this name
		}

		// If that didn't work, try using the 'import' name.
		String importedClassName = sfi.getImportedClassName( name );
		if ( importedClassName == null ) {
			return null;
		}
		return sfi.getEntityPersister( importedClassName );
	}

	/**
	 * Locate the persister by class or entity name, requiring that such a persister
	 * exist.
	 *
	 * @param name The class or entity name
	 *
	 * @return The defined persister for this entity
	 *
	 * @throws SemanticException Indicates the persister could not be found
	 */
	public EntityPersister requireClassPersister(String name) throws SemanticException {
		EntityPersister cp;
		try {
			cp = findEntityPersisterByName( name );
			if ( cp == null ) {
				throw new QuerySyntaxException( name + " is not mapped" );
			}
		}
		catch ( MappingException e ) {
			throw new DetailedSemanticException( e.getMessage(), e );
		}
		return cp;
	}

	/**
	 * Locate the collection persister by the collection role.
	 *
	 * @param role The collection role name.
	 *
	 * @return The defined CollectionPersister for this collection role, or null.
	 */
	public QueryableCollection getCollectionPersister(String role) {
		try {
			return (QueryableCollection) sfi.getCollectionPersister( role );
		}
		catch ( ClassCastException cce ) {
			throw new QueryException( "collection is not queryable: " + role );
		}
		catch ( Exception e ) {
			throw new QueryException( "collection not found: " + role );
		}
	}

	/**
	 * Locate the collection persister by the collection role, requiring that
	 * such a persister exist.
	 *
	 * @param role The collection role name.
	 *
	 * @return The defined CollectionPersister for this collection role.
	 *
	 * @throws QueryException Indicates that the collection persister could not be found.
	 */
	public QueryableCollection requireQueryableCollection(String role) throws QueryException {
		try {
			QueryableCollection queryableCollection = (QueryableCollection) sfi.getCollectionPersister( role );
			if ( queryableCollection != null ) {
				collectionPropertyMappingByRole.put( role, new CollectionPropertyMapping( queryableCollection ) );
			}
			return queryableCollection;
		}
		catch ( ClassCastException cce ) {
			throw new QueryException( "collection role is not queryable: " + role );
		}
		catch ( Exception e ) {
			throw new QueryException( "collection role not found: " + role );
		}
	}

	/**
	 * Retrieve a PropertyMapping describing the given collection role.
	 *
	 * @param role The collection role for which to retrieve the property mapping.
	 *
	 * @return The property mapping.
	 */
	public PropertyMapping getCollectionPropertyMapping(String role) {
		return collectionPropertyMappingByRole.get( role );
	}

	/**
	 * Retrieves the column names corresponding to the collection elements for the given
	 * collection role.
	 *
	 * @param role The collection role
	 * @param roleAlias The sql column-qualification alias (i.e., the table alias)
	 *
	 * @return the collection element columns
	 */
	public String[] getCollectionElementColumns(String role, String roleAlias) {
		return getCollectionPropertyMapping( role ).toColumns( roleAlias, CollectionPropertyNames.COLLECTION_ELEMENTS );
	}

	/**
	 * Generate an empty join sequence instance.
	 *
	 * @return The generate join sequence.
	 */
	public JoinSequence createJoinSequence() {
		return new JoinSequence( sfi );
	}

	/**
	 * Generate a join sequence representing the given association type.
	 *
	 * @param implicit Should implicit joins (theta-style) or explicit joins (ANSI-style) be rendered
	 * @param associationType The type representing the thing to be joined into.
	 * @param tableAlias The table alias to use in qualifying the join conditions
	 * @param joinType The type of join to render (inner, outer, etc);  see {@link org.hibernate.sql.JoinFragment}
	 * @param columns The columns making up the condition of the join.
	 *
	 * @return The generated join sequence.
	 */
	public JoinSequence createJoinSequence(boolean implicit, AssociationType associationType, String tableAlias, JoinType joinType, String[] columns) {
		JoinSequence joinSequence = createJoinSequence();
		joinSequence.setUseThetaStyle( implicit );    // Implicit joins use theta style (WHERE pk = fk), explicit joins use JOIN (after from)
		joinSequence.addJoin( associationType, tableAlias, joinType, columns );
		return joinSequence;
	}

	/**
	 * Create a join sequence rooted at the given collection.
	 *
	 * @param collPersister The persister for the collection at which the join should be rooted.
	 * @param collectionName The alias to use for qualifying column references.
	 *
	 * @return The generated join sequence.
	 */
	public JoinSequence createCollectionJoinSequence(QueryableCollection collPersister, String collectionName) {
		JoinSequence joinSequence = createJoinSequence();
		joinSequence.setRoot( collPersister, collectionName );
		joinSequence.setUseThetaStyle( true );        // TODO: figure out how this should be set.
///////////////////////////////////////////////////////////////////////////////
// This was the reason for failures regarding INDEX_OP and subclass joins on
// theta-join dialects; not sure what behavior we were trying to emulate ;)
//		joinSequence = joinSequence.getFromPart();	// Emulate the old addFromOnly behavior.
		return joinSequence;
	}

	/**
	 * Determine the name of the property for the entity encapsulated by the
	 * given type which represents the id or unique-key.
	 *
	 * @param entityType The type representing the entity.
	 *
	 * @return The corresponding property name
	 *
	 * @throws QueryException Indicates such a property could not be found.
	 */
	public String getIdentifierOrUniqueKeyPropertyName(EntityType entityType) {
		try {
			return entityType.getIdentifierOrUniqueKeyPropertyName( sfi );
		}
		catch ( MappingException me ) {
			throw new QueryException( me );
		}
	}

	/**
	 * Retrieve the number of columns represented by this type.
	 *
	 * @param type The type.
	 *
	 * @return The number of columns.
	 */
	public int getColumnSpan(Type type) {
		return type.getColumnSpan( sfi );
	}

	/**
	 * Given a collection type, determine the entity name of the elements
	 * contained within instance of that collection.
	 *
	 * @param collectionType The collection type to check.
	 *
	 * @return The entity name of the elements of this collection.
	 */
	public String getAssociatedEntityName(CollectionType collectionType) {
		return collectionType.getAssociatedEntityName( sfi );
	}

	/**
	 * Given a collection type, determine the Type representing elements
	 * within instances of that collection.
	 *
	 * @param collectionType The collection type to be checked.
	 *
	 * @return The Type of the elements of the collection.
	 */
	private Type getElementType(CollectionType collectionType) {
		return collectionType.getElementType( sfi );
	}

	/**
	 * Essentially the same as {@link #getElementType}, but requiring that the
	 * element type be an association type.
	 *
	 * @param collectionType The collection type to be checked.
	 *
	 * @return The AssociationType of the elements of the collection.
	 */
	public AssociationType getElementAssociationType(CollectionType collectionType) {
		return (AssociationType) getElementType( collectionType );
	}

	/**
	 * Locate a registered sql function by name.
	 *
	 * @param functionName The name of the function to locate
	 *
	 * @return The sql function, or null if not found.
	 */
	public SQLFunction findSQLFunction(String functionName) {
		return sfi.getSqlFunctionRegistry().findSQLFunction( functionName );
	}

	/**
	 * Locate a registered sql function by name, requiring that such a registered function exist.
	 *
	 * @param functionName The name of the function to locate
	 *
	 * @return The sql function.
	 *
	 * @throws QueryException Indicates no matching sql functions could be found.
	 */
	private SQLFunction requireSQLFunction(String functionName) {
		SQLFunction f = findSQLFunction( functionName );
		if ( f == null ) {
			throw new QueryException( "Unable to find SQL function: " + functionName );
		}
		return f;
	}

	/**
	 * Find the function return type given the function name and the first argument expression node.
	 *
	 * @param functionName The function name.
	 * @param first The first argument expression.
	 *
	 * @return the function return type given the function name and the first argument expression node.
	 */
	public Type findFunctionReturnType(String functionName, AST first) {
		SQLFunction sqlFunction = requireSQLFunction( functionName );
		return findFunctionReturnType( functionName, sqlFunction, first );
	}

	public Type findFunctionReturnType(String functionName, SQLFunction sqlFunction, AST firstArgument) {
		// determine the type of the first argument...
		Type argumentType = null;
		if ( firstArgument != null ) {
			if ( "cast".equals( functionName ) ) {
				argumentType = sfi.getTypeResolver().heuristicType( firstArgument.getNextSibling().getText() );
			}
			else if ( SqlNode.class.isInstance( firstArgument ) ) {
				argumentType = ( (SqlNode) firstArgument ).getDataType();
			}
		}

		return sqlFunction.getReturnType( argumentType, sfi );
	}

	public String[][] generateColumnNames(Type[] sqlResultTypes) {
		return NameGenerator.generateColumnNames( sqlResultTypes, sfi );
	}

	public boolean isStrictJPAQLComplianceEnabled() {
		return sfi.getSettings().isStrictJPAQLCompliance();
	}
}
