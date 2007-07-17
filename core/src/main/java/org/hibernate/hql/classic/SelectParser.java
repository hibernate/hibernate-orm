//$Id: SelectParser.java 9915 2006-05-09 09:38:15Z max.andersen@jboss.com $
package org.hibernate.hql.classic;

import org.hibernate.Hibernate;
import org.hibernate.QueryException;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.hql.QuerySplitter;
import org.hibernate.type.Type;
import org.hibernate.util.ReflectHelper;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Parsers the select clause of a Hibernate query.
 *
 * @author Gavin King, David Channon
 */
public class SelectParser implements Parser {

	//TODO: arithmetic expressions, multiple new Foo(...)

	private static final Set COUNT_MODIFIERS = new HashSet();

	static {
		COUNT_MODIFIERS.add( "distinct" );
		COUNT_MODIFIERS.add( "all" );
		COUNT_MODIFIERS.add( "*" );
	}

	private LinkedList aggregateFuncTokenList = new LinkedList();

	private boolean ready;
	private boolean aggregate;
	private boolean first;
	private boolean afterNew;
	private boolean insideNew;
	private boolean aggregateAddSelectScalar;
	private Class holderClass;

	private final SelectPathExpressionParser pathExpressionParser;
	private final PathExpressionParser aggregatePathExpressionParser;

	{
		pathExpressionParser = new SelectPathExpressionParser();
		aggregatePathExpressionParser = new PathExpressionParser();
		//TODO: would be nice to use false, but issues with MS SQL
		pathExpressionParser.setUseThetaStyleJoin( true );
		aggregatePathExpressionParser.setUseThetaStyleJoin( true );
	}

	public void token(String token, QueryTranslatorImpl q) throws QueryException {

		String lctoken = token.toLowerCase();

		if ( first ) {
			first = false;
			if ( "distinct".equals( lctoken ) ) {
				q.setDistinct( true );
				return;
			}
			else if ( "all".equals( lctoken ) ) {
				q.setDistinct( false );
				return;
			}
		}

		if ( afterNew ) {
			afterNew = false;
			try {
				holderClass = ReflectHelper.classForName( QuerySplitter.getImportedClass( token, q.getFactory() ) );
			}
			catch ( ClassNotFoundException cnfe ) {
				throw new QueryException( cnfe );
			}
			if ( holderClass == null ) throw new QueryException( "class not found: " + token );
			q.setHolderClass( holderClass );
			insideNew = true;
		}
		else if ( token.equals( "," ) ) {
			if ( !aggregate && ready ) throw new QueryException( "alias or expression expected in SELECT" );
			q.appendScalarSelectToken( ", " );
			ready = true;
		}
		else if ( "new".equals( lctoken ) ) {
			afterNew = true;
			ready = false;
		}
		else if ( "(".equals( token ) ) {
			if ( insideNew && !aggregate && !ready ) {
				//opening paren in new Foo ( ... )
				ready = true;
			}
			else if ( aggregate ) {
				q.appendScalarSelectToken( token );
			}
			else {
				throw new QueryException( "aggregate function expected before ( in SELECT" );
			}
			ready = true;
		}
		else if ( ")".equals( token ) ) {
			if ( insideNew && !aggregate && !ready ) {
				//if we are inside a new Result(), but not inside a nested function
				insideNew = false;
			}
			else if ( aggregate && ready ) {
				q.appendScalarSelectToken( token );
				aggregateFuncTokenList.removeLast();
				if ( aggregateFuncTokenList.size() < 1 ) {
					aggregate = false;
					ready = false;
				}
			}
			else {
				throw new QueryException( "( expected before ) in select" );
			}
		}
		else if ( COUNT_MODIFIERS.contains( lctoken ) ) {
			if ( !ready || !aggregate ) throw new QueryException( token + " only allowed inside aggregate function in SELECT" );
			q.appendScalarSelectToken( token );
			if ( "*".equals( token ) ) q.addSelectScalar( getFunction( "count", q ).getReturnType( Hibernate.LONG, q.getFactory() ) ); //special case
		}
		else if ( getFunction( lctoken, q ) != null && token.equals( q.unalias( token ) ) ) {
			// the name of an SQL function
			if ( !ready ) throw new QueryException( ", expected before aggregate function in SELECT: " + token );
			aggregate = true;
			aggregateAddSelectScalar = true;
			aggregateFuncTokenList.add( lctoken );
			ready = false;
			q.appendScalarSelectToken( token );
			if ( !aggregateHasArgs( lctoken, q ) ) {
				q.addSelectScalar( aggregateType( aggregateFuncTokenList, null, q ) );
				if ( !aggregateFuncNoArgsHasParenthesis( lctoken, q ) ) {
					aggregateFuncTokenList.removeLast();
					if ( aggregateFuncTokenList.size() < 1 ) {
						aggregate = false;
						ready = false;
					}
					else {
						ready = true;
					}
				}
			}
		}
		else if ( aggregate ) {
			boolean constantToken = false;
			if ( !ready ) throw new QueryException( "( expected after aggregate function in SELECT" );
			try {
				ParserHelper.parse( aggregatePathExpressionParser, q.unalias( token ), ParserHelper.PATH_SEPARATORS, q );
			}
			catch ( QueryException qex ) {
				constantToken = true;
			}

			if ( constantToken ) {
				q.appendScalarSelectToken( token );
			}
			else {
				if ( aggregatePathExpressionParser.isCollectionValued() ) {
					q.addCollection( aggregatePathExpressionParser.getCollectionName(),
							aggregatePathExpressionParser.getCollectionRole() );
				}
				q.appendScalarSelectToken( aggregatePathExpressionParser.getWhereColumn() );
				if ( aggregateAddSelectScalar ) {
					q.addSelectScalar( aggregateType( aggregateFuncTokenList, aggregatePathExpressionParser.getWhereColumnType(), q ) );
					aggregateAddSelectScalar = false;
				}
				aggregatePathExpressionParser.addAssociation( q );
			}
		}
		else {
			if ( !ready ) throw new QueryException( ", expected in SELECT" );
			ParserHelper.parse( pathExpressionParser, q.unalias( token ), ParserHelper.PATH_SEPARATORS, q );
			if ( pathExpressionParser.isCollectionValued() ) {
				q.addCollection( pathExpressionParser.getCollectionName(),
						pathExpressionParser.getCollectionRole() );
			}
			else if ( pathExpressionParser.getWhereColumnType().isEntityType() ) {
				q.addSelectClass( pathExpressionParser.getSelectName() );
			}
			q.appendScalarSelectTokens( pathExpressionParser.getWhereColumns() );
			q.addSelectScalar( pathExpressionParser.getWhereColumnType() );
			pathExpressionParser.addAssociation( q );

			ready = false;
		}
	}

	public boolean aggregateHasArgs(String funcToken, QueryTranslatorImpl q) {
		return getFunction( funcToken, q ).hasArguments();
	}

	public boolean aggregateFuncNoArgsHasParenthesis(String funcToken, QueryTranslatorImpl q) {
		return getFunction( funcToken, q ).hasParenthesesIfNoArguments();
	}

	public Type aggregateType(List funcTokenList, Type type, QueryTranslatorImpl q) throws QueryException {
		Type retType = type;
		Type argType;
		for ( int i = funcTokenList.size() - 1; i >= 0; i-- ) {
			argType = retType;
			String funcToken = ( String ) funcTokenList.get( i );
			retType = getFunction( funcToken, q ).getReturnType( argType, q.getFactory() );
		}
		return retType;
	}

	private SQLFunction getFunction(String name, QueryTranslatorImpl q) {
		return q.getFactory().getSqlFunctionRegistry().findSQLFunction( name );
	}

	public void start(QueryTranslatorImpl q) {
		ready = true;
		first = true;
		aggregate = false;
		afterNew = false;
		insideNew = false;
		holderClass = null;
		aggregateFuncTokenList.clear();
	}

	public void end(QueryTranslatorImpl q) {
	}

}
