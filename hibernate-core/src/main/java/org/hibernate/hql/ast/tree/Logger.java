/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.hibernate.hql.ast.tree;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.TRACE;
import org.hibernate.persister.entity.Queryable.Declarer;
import org.hibernate.type.Type;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;
import antlr.collections.AST;

/**
 * Interface defining messages that may be logged by the outer class
 */
@MessageLogger
interface Logger extends BasicLogger {

    @LogMessage( level = DEBUG )
    @Message( value = "addCollectionJoinFromElementByPath() : %s -> %s" )
    void addCollectionJoinFromElementByPath( String path,
                                             FromElement destination );

    @LogMessage( level = DEBUG )
    @Message( value = "addJoinByPathMap() : %s -> %s" )
    void addJoinByPathMap( String path,
                           String displayText );

    @LogMessage( level = TRACE )
    @Message( value = "Attempt to disable subclass-inclusions : %s" )
    void attemptToDisableSubclassInclusions( Exception exception );

    @LogMessage( level = DEBUG )
    @Message( value = "collectionProperty() :  name=%s type=%s" )
    void collectionProperty( AST name,
                             Type type );

    @LogMessage( level = DEBUG )
    @Message( value = "%s() : correlated subquery" )
    void correlatedSubquery( String methodName );

    @LogMessage( level = DEBUG )
    @Message( value = "createEntityAssociation() : One to many - path = %s role = %s associatedEntityName = %s" )
    void createEntityAssociation( String path,
                                  String role,
                                  String associatedEntityName );

    @LogMessage( level = DEBUG )
    @Message( value = "createEntityJoin() : Implied multi-table entity join" )
    void createEntityJoin();

    @LogMessage( level = DEBUG )
    @Message( value = "createFromElementInSubselect() : creating a new FROM element..." )
    void createFromElementInSubselect();

    @LogMessage( level = DEBUG )
    @Message( value = "createFromElementInSubselect() : path = %s" )
    void createFromElementInSubselect( String path );

    @LogMessage( level = DEBUG )
    @Message( value = "createFromElementInSubselect() : %s -> %s" )
    void createFromElementInSubselect( String path,
                                       FromElement fromElement );

    @LogMessage( level = DEBUG )
    @Message( value = "Creating elements for %s" )
    void creatingElements( String path );

    @LogMessage( level = DEBUG )
    @Message( value = "Creating join for many-to-many elements for %s" )
    void creatingJoinForManyToManyElements( String path );

    @LogMessage( level = DEBUG )
    @Message( value = "createManyToMany() : path = %s role = %s associatedEntityName = %s" )
    void createManyToMany( String path,
                           String role,
                           String associatedEntityName );

    @LogMessage( level = DEBUG )
    @Message( value = "dereferenceCollection() : Created new FROM element for %s : %s" )
    void dereferenceCollection( String propName,
                                FromElement elem );

    @LogMessage( level = DEBUG )
    @Message( value = "dereferenceEntityJoin() : generating join for %s in %s (%s) parent = %s" )
    void dereferenceEntityJoin( String propertyName,
                                String className,
                                String string,
                                String debugString );

    @LogMessage( level = DEBUG )
    @Message( value = "dereferenceShortcut() : property %s in %s does not require a join." )
    void dereferenceShortcut( String propertyName,
                              String className );

    @LogMessage( level = DEBUG )
    @Message( value = "%s :  %s (%s) -> %s" )
    void fromClause( FromClause fromClause,
                     String className,
                     String string,
                     String tableAlias );

    @LogMessage( level = DEBUG )
    @Message( value = "FROM element found for collection join path %s" )
    void fromElementFound( String path );

    @LogMessage( level = DEBUG )
    @Message( value = "getDataType() : %s -> %s" )
    void getDataType( String propertyPath,
                      Type propertyType );

    @LogMessage( level = DEBUG )
    @Message( value = "getOrderByClause() : Creating a new ORDER BY clause" )
    void getOrderByClause();

    @LogMessage( level = TRACE )
    @Message( value = "Handling property dereference [%s (%s) -> %s (%s)]" )
    void handlingPropertyDereference( String entityName,
                                      String classAlias,
                                      String propertyName,
                                      Declarer propertyDeclarer );

    @Message( value = "<no alias>" )
    String noAlias();

    @LogMessage( level = DEBUG )
    @Message( value = "No FROM element found for the elements of collection join path %s, created %s" )
    void noFromElementFound( String path,
                             FromElement elem );

    @LogMessage( level = DEBUG )
    @Message( value = "Promoting [%s] to [%s]" )
    void promoting( FromElement elem,
                    FromClause fromClause );

    @LogMessage( level = DEBUG )
    @Message( value = "Resolved :  %s -> %s" )
    void resolved( String path,
                   String text );

    @Message( value = "Stack-trace source" )
    String stackTraceSource();

    @LogMessage( level = DEBUG )
    @Message( value = "Terminal propertyPath = [%s]" )
    void terminalPropertyPath( String propertyPath );

    @LogMessage( level = DEBUG )
    @Message( value = "toColumns(%s,%s) : subquery = %s" )
    void toColumns( String tableAlias,
                    String path,
                    String subquery );

    @LogMessage( level = DEBUG )
    @Message( value = "Unresolved property path is now '%s'" )
    void unresolvedPropertyPathIsNow( String propertyPath );

    @LogMessage( level = TRACE )
    @Message( value = "Using non-qualified column reference [%s -> (%s)]" )
    void usingNonQualifiedColumnReference( String path,
                                           String string );
}
