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
package org.hibernate.hql.internal.ast.tree;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import antlr.SemanticException;
import antlr.collections.AST;

import org.hibernate.QueryException;
import org.hibernate.hql.internal.antlr.SqlTokenTypes;
import org.hibernate.hql.internal.ast.util.ASTAppender;
import org.hibernate.hql.internal.ast.util.ASTIterator;
import org.hibernate.hql.internal.ast.util.ASTPrinter;
import org.hibernate.type.Type;

/**
 * Represents the list of expressions in a SELECT clause.
 *
 * @author josh
 */
public class SelectClause extends SelectExpressionList {

	private boolean prepared = false;
	private boolean scalarSelect;

	private List fromElementsForLoad = new ArrayList();
	//private Type[] sqlResultTypes;
	private Type[] queryReturnTypes;
	private String[][] columnNames;
	private List collectionFromElements;
	private String[] aliases;
	private int[] columnNamesStartPositions;

	// Currently we can only have one...
	private AggregatedSelectExpression aggregatedSelectExpression;

	/**
	 * Does this SelectClause represent a scalar query
	 *
	 * @return True if this is a scalara select clause; false otherwise.
	 */
	public boolean isScalarSelect() {
		return scalarSelect;
	}

	public boolean isDistinct() {
		return getFirstChild() != null && getFirstChild().getType() == SqlTokenTypes.DISTINCT;
	}

	/**
	 * FromElements which need to be accounted for in the load phase (either for return or for fetch).
	 *
	 * @return List of appropriate FromElements.
	 */
	public List getFromElementsForLoad() {
		return fromElementsForLoad;
	}

	/*
	 * The types represented in the SQL result set.
	 *
	 * @return The types represented in the SQL result set.
	 */
	/*public Type[] getSqlResultTypes() {
		return sqlResultTypes;
	}*/

	/**
	 * The types actually being returned from this query at the "object level".
	 *
	 * @return The query return types.
	 */
	public Type[] getQueryReturnTypes() {
		return queryReturnTypes;
	}
	
	/**
	 * The HQL aliases, or generated aliases
	 *
	 * @return the aliases
	 */
	public String[] getQueryReturnAliases() {
		return aliases;
	}

	/**
	 * The column alias names being used in the generated SQL.
	 *
	 * @return The SQL column aliases.
	 */
	public String[][] getColumnNames() {
		return columnNames;
	}

	public AggregatedSelectExpression getAggregatedSelectExpression() {
		return aggregatedSelectExpression;
	}

	/**
	 * Prepares an explicitly defined select clause.
	 *
	 * @param fromClause The from clause linked to this select clause.
	 * @throws SemanticException indicates a semntic issue with the explicit select clause.
	 */
	public void initializeExplicitSelectClause(FromClause fromClause) throws SemanticException {
		if ( prepared ) {
			throw new IllegalStateException( "SelectClause was already prepared!" );
		}

		//explicit = true;	// This is an explict Select.
		//ArrayList sqlResultTypeList = new ArrayList();
		ArrayList queryReturnTypeList = new ArrayList();

		// First, collect all of the select expressions.
		// NOTE: This must be done *before* invoking setScalarColumnText() because setScalarColumnText()
		// changes the AST!!!
		SelectExpression[] selectExpressions = collectSelectExpressions();
		
		for ( int i = 0; i < selectExpressions.length; i++ ) {
			SelectExpression selectExpression = selectExpressions[i];

			if ( AggregatedSelectExpression.class.isInstance( selectExpression ) ) {
				aggregatedSelectExpression = (AggregatedSelectExpression) selectExpression;
				queryReturnTypeList.addAll( aggregatedSelectExpression.getAggregatedSelectionTypeList() );
				scalarSelect = true;
			}
			else {
				Type type = selectExpression.getDataType();
				if ( type == null ) {
					throw new IllegalStateException( "No data type for node: " + selectExpression.getClass().getName() + " "
							+ new ASTPrinter( SqlTokenTypes.class ).showAsString( ( AST ) selectExpression, "" ) );
				}
				//sqlResultTypeList.add( type );

				// If the data type is not an association type, it could not have been in the FROM clause.
				if ( selectExpression.isScalar() ) {
					scalarSelect = true;
				}

				if ( isReturnableEntity( selectExpression ) ) {
					fromElementsForLoad.add( selectExpression.getFromElement() );
				}

				// Always add the type to the return type list.
				queryReturnTypeList.add( type );
			}
		}

		//init the aliases, after initing the constructornode
		initAliases(selectExpressions);

		if ( !getWalker().isShallowQuery() ) {
			// add the fetched entities
			List fromElements = fromClause.getProjectionList();
	
			ASTAppender appender = new ASTAppender( getASTFactory(), this );	// Get ready to start adding nodes.
			int size = fromElements.size();
	
			Iterator iterator = fromElements.iterator();
			for ( int k = 0; iterator.hasNext(); k++ ) {
				FromElement fromElement = ( FromElement ) iterator.next();
	
				if ( fromElement.isFetch() ) {
					FromElement origin = null;
					if ( fromElement.getRealOrigin() == null ) {
						// work around that crazy issue where the tree contains
						// "empty" FromElements (no text); afaict, this is caused
						// by FromElementFactory.createCollectionJoin()
						if ( fromElement.getOrigin() == null ) {
							throw new QueryException( "Unable to determine origin of join fetch [" + fromElement.getDisplayText() + "]" );
						}
						else {
							origin = fromElement.getOrigin();
						}
					}
					else {
						origin = fromElement.getRealOrigin();
					}
					if ( !fromElementsForLoad.contains( origin ) ) {
						throw new QueryException(
								"query specified join fetching, but the owner " +
								"of the fetched association was not present in the select list " +
								"[" + fromElement.getDisplayText() + "]"
						);
					}
					Type type = fromElement.getSelectType();
					addCollectionFromElement( fromElement );
					if ( type != null ) {
						boolean collectionOfElements = fromElement.isCollectionOfValuesOrComponents();
						if ( !collectionOfElements ) {
							// Add the type to the list of returned sqlResultTypes.
							fromElement.setIncludeSubclasses( true );
							fromElementsForLoad.add( fromElement );
							//sqlResultTypeList.add( type );
							// Generate the select expression.
							String text = fromElement.renderIdentifierSelect( size, k );
							SelectExpressionImpl generatedExpr = ( SelectExpressionImpl ) appender.append( SqlTokenTypes.SELECT_EXPR, text, false );
							if ( generatedExpr != null ) {
								generatedExpr.setFromElement( fromElement );
							}
						}
					}
				}
			}
	
			// generate id select fragment and then property select fragment for
			// each expression, just like generateSelectFragments().
			renderNonScalarSelects( collectSelectExpressions(), fromClause );
		}

		if ( scalarSelect || getWalker().isShallowQuery() ) {
			// If there are any scalars (non-entities) selected, render the select column aliases.
			renderScalarSelects( selectExpressions, fromClause );
		}

		finishInitialization( /*sqlResultTypeList,*/ queryReturnTypeList );
	}

	private void finishInitialization(ArrayList queryReturnTypeList) {
		queryReturnTypes = ( Type[] ) queryReturnTypeList.toArray( new Type[queryReturnTypeList.size()] );
		initializeColumnNames();
		prepared = true;
	}

	private void initializeColumnNames() {
		// Generate an 2d array of column names, the first dimension is parallel with the
		// return types array.  The second dimension is the list of column names for each
		// type.

		// todo: we should really just collect these from the various SelectExpressions, rather than regenerating here
		columnNames = getSessionFactoryHelper().generateColumnNames( queryReturnTypes );
		columnNamesStartPositions = new int[ columnNames.length ];
		int startPosition = 1;
		for ( int i = 0 ; i < columnNames.length ; i ++ ) {
			columnNamesStartPositions[ i ] = startPosition;
			startPosition += columnNames[ i ].length;
		}
	}

	public int getColumnNamesStartPosition(int i) {
		return columnNamesStartPositions[ i ];
	}

	/**
	 * Prepares a derived (i.e., not explicitly defined in the query) select clause.
	 *
	 * @param fromClause The from clause to which this select clause is linked.
	 */
	public void initializeDerivedSelectClause(FromClause fromClause) throws SemanticException {
		if ( prepared ) {
			throw new IllegalStateException( "SelectClause was already prepared!" );
		}
		//Used to be tested by the TCK but the test is no longer here
//		if ( getSessionFactoryHelper().isStrictJPAQLComplianceEnabled() && !getWalker().isSubQuery() ) {
//			// NOTE : the isSubQuery() bit is a temporary hack...
//			throw new QuerySyntaxException( "JPA-QL compliance requires select clause" );
//		}
		List fromElements = fromClause.getProjectionList();

		ASTAppender appender = new ASTAppender( getASTFactory(), this );	// Get ready to start adding nodes.
		int size = fromElements.size();
		ArrayList queryReturnTypeList = new ArrayList( size );

		Iterator iterator = fromElements.iterator();
		for ( int k = 0; iterator.hasNext(); k++ ) {
			FromElement fromElement = ( FromElement ) iterator.next();
			Type type = fromElement.getSelectType();

			addCollectionFromElement( fromElement );

			if ( type != null ) {
				boolean collectionOfElements = fromElement.isCollectionOfValuesOrComponents();
				if ( !collectionOfElements ) {
					if ( !fromElement.isFetch() ) {
						// Add the type to the list of returned sqlResultTypes.
						queryReturnTypeList.add( type );
					}
					fromElementsForLoad.add( fromElement );
					// Generate the select expression.
					String text = fromElement.renderIdentifierSelect( size, k );
					SelectExpressionImpl generatedExpr = ( SelectExpressionImpl ) appender.append( SqlTokenTypes.SELECT_EXPR, text, false );
					if ( generatedExpr != null ) {
						generatedExpr.setFromElement( fromElement );
					}
				}
			}
		}

		// Get all the select expressions (that we just generated) and render the select.
		SelectExpression[] selectExpressions = collectSelectExpressions();

		if ( getWalker().isShallowQuery() ) {
			renderScalarSelects( selectExpressions, fromClause );
		}
		else {
			renderNonScalarSelects( selectExpressions, fromClause );
		}
		finishInitialization( queryReturnTypeList );
	}
	
	public static boolean VERSION2_SQL = false;

	private void addCollectionFromElement(FromElement fromElement) {
		if ( fromElement.isFetch() ) {
			if ( fromElement.isCollectionJoin() || fromElement.getQueryableCollection() != null ) {
				String suffix;
				if (collectionFromElements==null) {
					collectionFromElements = new ArrayList();
					suffix = VERSION2_SQL ? "__" : "0__";
				}
				else {
					suffix = Integer.toString( collectionFromElements.size() ) + "__";
				}
				collectionFromElements.add( fromElement );
				fromElement.setCollectionSuffix( suffix );
			}
		}
	}

	protected AST getFirstSelectExpression() {
		AST n = getFirstChild();
		// Skip 'DISTINCT' and 'ALL', so we return the first expression node.
		while ( n != null && ( n.getType() == SqlTokenTypes.DISTINCT || n.getType() == SqlTokenTypes.ALL ) ) {
			n = n.getNextSibling();
		}
		return n;
	}

	private boolean isReturnableEntity(SelectExpression selectExpression) throws SemanticException {
		FromElement fromElement = selectExpression.getFromElement();
		boolean isFetchOrValueCollection = fromElement != null && 
				( fromElement.isFetch() || fromElement.isCollectionOfValuesOrComponents() );
		if ( isFetchOrValueCollection ) {
			return false;
		}
		else {
			return selectExpression.isReturnableEntity();
		}
	}

	private void renderScalarSelects(SelectExpression[] se, FromClause currentFromClause) throws SemanticException {
		if ( !currentFromClause.isSubQuery() ) {
			for ( int i = 0; i < se.length; i++ ) {
				SelectExpression expr = se[i];
				expr.setScalarColumn( i );	// Create SQL_TOKEN nodes for the columns.
			}
		}
	}
	
	private void initAliases(SelectExpression[] selectExpressions) {
		if ( aggregatedSelectExpression == null ) {
			aliases = new String[selectExpressions.length];
			for ( int i=0; i<selectExpressions.length; i++ ) {
				String alias = selectExpressions[i].getAlias();
				aliases[i] = alias==null ? Integer.toString(i) : alias;
			}
		}
		else {
			aliases = aggregatedSelectExpression.getAggregatedAliases();
		}
	}

	private void renderNonScalarSelects(SelectExpression[] selectExpressions, FromClause currentFromClause) 
	throws SemanticException {
		ASTAppender appender = new ASTAppender( getASTFactory(), this );
		final int size = selectExpressions.length;
		int nonscalarSize = 0;
		for ( int i = 0; i < size; i++ ) {
			if ( !selectExpressions[i].isScalar() ) nonscalarSize++;
		}

		int j = 0;
		for ( int i = 0; i < size; i++ ) {
			if ( !selectExpressions[i].isScalar() ) {
				SelectExpression expr = selectExpressions[i];
				FromElement fromElement = expr.getFromElement();
				if ( fromElement != null ) {
					renderNonScalarIdentifiers( fromElement, nonscalarSize, j, expr, appender );
					j++;
				}
			}
		}

		if ( !currentFromClause.isSubQuery() ) {
			// Generate the property select tokens.
			int k = 0;
			for ( int i = 0; i < size; i++ ) {
				if ( !selectExpressions[i].isScalar() ) {
					FromElement fromElement = selectExpressions[i].getFromElement();
					if ( fromElement != null ) {
						renderNonScalarProperties( appender, fromElement, nonscalarSize, k );
						k++;
					}
				}
			}
		}
	}

	private void renderNonScalarIdentifiers(FromElement fromElement, int nonscalarSize, int j, SelectExpression expr, ASTAppender appender) {
		String text = fromElement.renderIdentifierSelect( nonscalarSize, j );
		if ( !fromElement.getFromClause().isSubQuery() ) {
			if ( !scalarSelect && !getWalker().isShallowQuery() ) {
				//TODO: is this a bit ugly?
				expr.setText( text );
			}
			else {
				appender.append( SqlTokenTypes.SQL_TOKEN, text, false );
			}
		}
	}

	private void renderNonScalarProperties(ASTAppender appender, FromElement fromElement, int nonscalarSize, int k) {
		String text = fromElement.renderPropertySelect( nonscalarSize, k );
		appender.append( SqlTokenTypes.SQL_TOKEN, text, false );
		if ( fromElement.getQueryableCollection() != null && fromElement.isFetch() ) {
			text = fromElement.renderCollectionSelectFragment( nonscalarSize, k );
			appender.append( SqlTokenTypes.SQL_TOKEN, text, false );
		}
		// Look through the FromElement's children to find any collections of values that should be fetched...
		ASTIterator iter = new ASTIterator( fromElement );
		while ( iter.hasNext() ) {
			FromElement child = ( FromElement ) iter.next();
			if ( child.isCollectionOfValuesOrComponents() && child.isFetch() ) {
				// Need a better way to define the suffixes here...
				text = child.renderValueCollectionSelectFragment( nonscalarSize, nonscalarSize + k );
				appender.append( SqlTokenTypes.SQL_TOKEN, text, false );
			}
		}
	}

	public List getCollectionFromElements() {
		return collectionFromElements;
	}
}
