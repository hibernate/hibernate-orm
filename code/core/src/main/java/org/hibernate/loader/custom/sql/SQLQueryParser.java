//$Id: SQLQueryParser.java 10067 2006-06-28 13:24:59Z steve.ebersole@jboss.com $
package org.hibernate.loader.custom.sql;

import org.hibernate.QueryException;
import org.hibernate.engine.query.ParameterParser;
import org.hibernate.persister.collection.SQLLoadableCollection;
import org.hibernate.persister.entity.SQLLoadable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Gavin King
 * @author Max Andersen
 * @author Steve Ebersole
 */
public class SQLQueryParser {

	private final String originalQueryString;
	private final ParserContext context;

	private final Map namedParameters = new HashMap();
	private long aliasesFound = 0;

	static interface ParserContext {
		boolean isEntityAlias(String aliasName);
		SQLLoadable getEntityPersisterByAlias(String alias);
		String getEntitySuffixByAlias(String alias);
		boolean isCollectionAlias(String aliasName);
		SQLLoadableCollection getCollectionPersisterByAlias(String alias);
		String getCollectionSuffixByAlias(String alias);
		Map getPropertyResultsMapByAlias(String alias);
	}

	public SQLQueryParser(String queryString, ParserContext context) {
		this.originalQueryString = queryString;
		this.context = context;
	}

	public Map getNamedParameters() {
		return namedParameters;
	}

	public boolean queryHasAliases() {
		return aliasesFound>0;
	}

	public String process() {
		return substituteParams( substituteBrackets( originalQueryString ) );
	}

	// TODO: should "record" how many properties we have reffered to - and if we 
	//       don't get'em'all we throw an exception! Way better than trial and error ;)
	private String substituteBrackets(String sqlQuery) throws QueryException {

		StringBuffer result = new StringBuffer( sqlQuery.length() + 20 );
		int left, right;

		// replace {....} with corresponding column aliases
		for ( int curr = 0; curr < sqlQuery.length(); curr = right + 1 ) {
			if ( ( left = sqlQuery.indexOf( '{', curr ) ) < 0 ) {
				// No additional open braces found in the string, append the
				// rest of the string in its entirty and quit this loop
				result.append( sqlQuery.substring( curr ) );
				break;
			}

			// apend everything up until the next encountered open brace
			result.append( sqlQuery.substring( curr, left ) );

			if ( ( right = sqlQuery.indexOf( '}', left + 1 ) ) < 0 ) {
				throw new QueryException( "Unmatched braces for alias path", sqlQuery );
			}

			String aliasPath = sqlQuery.substring( left + 1, right );
			int firstDot = aliasPath.indexOf( '.' );
			if ( firstDot == -1 ) {
				if ( context.isEntityAlias( aliasPath ) ) {
					// it is a simple table alias {foo}
					result.append( aliasPath );
					aliasesFound++;
				} 
				else {
					// passing through anything we do not know : to support jdbc escape sequences HB-898
					result.append( '{' ).append(aliasPath).append( '}' );					
				}
			}
			else {
				String aliasName = aliasPath.substring(0, firstDot);
				boolean isCollection = context.isCollectionAlias( aliasName );
				boolean isEntity = context.isEntityAlias( aliasName );
				
				if ( isCollection ) {
					// The current alias is referencing the collection to be eagerly fetched
					String propertyName = aliasPath.substring( firstDot + 1 );
					result.append( resolveCollectionProperties( aliasName, propertyName ) );
					aliasesFound++;
				} 
				else if ( isEntity ) {
					// it is a property reference {foo.bar}
					String propertyName = aliasPath.substring( firstDot + 1 );
					result.append( resolveProperties( aliasName, propertyName ) );
					aliasesFound++;
				}
				else {
					// passing through anything we do not know : to support jdbc escape sequences HB-898
					result.append( '{' ).append(aliasPath).append( '}' );
				}
	
			}
		}

		// Possibly handle :something parameters for the query ?

		return result.toString();
	}	

	private String resolveCollectionProperties(
			String aliasName,
			String propertyName) {

		Map fieldResults = context.getPropertyResultsMapByAlias( aliasName );
		SQLLoadableCollection collectionPersister = context.getCollectionPersisterByAlias( aliasName );
		String collectionSuffix = context.getCollectionSuffixByAlias( aliasName );

		if ( "*".equals( propertyName ) ) {
			if( !fieldResults.isEmpty() ) {
				throw new QueryException("Using return-propertys together with * syntax is not supported.");
			}
			
			String selectFragment = collectionPersister.selectFragment( aliasName, collectionSuffix );
			aliasesFound++;
			return selectFragment 
						+ ", " 
						+ resolveProperties( aliasName, propertyName );
		}
		else if ( "element.*".equals( propertyName ) ) {
			return resolveProperties( aliasName, "*" );
		}
		else {
			String[] columnAliases;

			// Let return-propertys override whatever the persister has for aliases.
			columnAliases = ( String[] ) fieldResults.get(propertyName);
			if ( columnAliases==null ) {
				columnAliases = collectionPersister.getCollectionPropertyColumnAliases( propertyName, collectionSuffix );
			}
			
			if ( columnAliases == null || columnAliases.length == 0 ) {
				throw new QueryException(
						"No column name found for property [" + propertyName + "] for alias [" + aliasName + "]",
						originalQueryString
				);
			}
			if ( columnAliases.length != 1 ) {
				// TODO: better error message since we actually support composites if names are explicitly listed.
				throw new QueryException(
						"SQL queries only support properties mapped to a single column - property [" +
						propertyName + "] is mapped to " + columnAliases.length + " columns.",
						originalQueryString
				);
			}
			aliasesFound++;
			return columnAliases[0];
		
		}
	}
	private String resolveProperties(
			String aliasName,
	        String propertyName) {
		Map fieldResults = context.getPropertyResultsMapByAlias( aliasName );
		SQLLoadable persister = context.getEntityPersisterByAlias( aliasName );
		String suffix = context.getEntitySuffixByAlias( aliasName );

		if ( "*".equals( propertyName ) ) {
			if( !fieldResults.isEmpty() ) {
				throw new QueryException("Using return-propertys together with * syntax is not supported.");
			}			
			aliasesFound++;
			return persister.selectFragment( aliasName, suffix ) ;
		}
		else {

			String[] columnAliases;

			// Let return-propertys override whatever the persister has for aliases.
			columnAliases = (String[]) fieldResults.get( propertyName );
			if ( columnAliases == null ) {
				columnAliases = persister.getSubclassPropertyColumnAliases( propertyName, suffix );
			}

			if ( columnAliases == null || columnAliases.length == 0 ) {
				throw new QueryException(
						"No column name found for property [" + propertyName + "] for alias [" + aliasName + "]",
						originalQueryString
				);
			}
			if ( columnAliases.length != 1 ) {
				// TODO: better error message since we actually support composites if names are explicitly listed.
				throw new QueryException(
						"SQL queries only support properties mapped to a single column - property [" + propertyName + "] is mapped to " + columnAliases.length + " columns.",
						originalQueryString
				);
			}			
			aliasesFound++;
			return columnAliases[0];
		}
	}

	/**
	 * Substitues JDBC parameter placeholders (?) for all encountered
	 * parameter specifications.  It also tracks the positions of these
	 * parameter specifications within the query string.  This accounts for
	 * ordinal-params, named-params, and ejb3-positional-params.
	 *
	 * @param sqlString The query string.
	 * @return The SQL query with parameter substitution complete.
	 */
	private String substituteParams(String sqlString) {
		ParameterSubstitutionRecognizer recognizer = new ParameterSubstitutionRecognizer();
		ParameterParser.parse( sqlString, recognizer );

		namedParameters.clear();
		namedParameters.putAll( recognizer.namedParameterBindPoints );

		return recognizer.result.toString();
	}

	public static class ParameterSubstitutionRecognizer implements ParameterParser.Recognizer {
		StringBuffer result = new StringBuffer();
		Map namedParameterBindPoints = new HashMap();
		int parameterCount = 0;

		public void outParameter(int position) {
			result.append( '?' );
		}

		public void ordinalParameter(int position) {
			result.append( '?' );
		}

		public void namedParameter(String name, int position) {
			addNamedParameter( name );
			result.append( '?' );
		}

		public void jpaPositionalParameter(String name, int position) {
			namedParameter( name, position );
		}

		public void other(char character) {
			result.append( character );
		}

		private void addNamedParameter(String name) {
			Integer loc = new Integer( parameterCount++ );
			Object o = namedParameterBindPoints.get( name );
			if ( o == null ) {
				namedParameterBindPoints.put( name, loc );
			}
			else if ( o instanceof Integer ) {
				ArrayList list = new ArrayList( 4 );
				list.add( o );
				list.add( loc );
				namedParameterBindPoints.put( name, list );
			}
			else {
				( ( List ) o ).add( loc );
			}
		}
	}
}
