/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sql.internal;

import java.util.Map;

import org.hibernate.QueryException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.SQLLoadableCollection;
import org.hibernate.persister.entity.SQLLoadable;

/**
 * Substitutes escape sequences of form {@code {alias}},
 * {@code {alias.field}}, and {@code {alias.*}} in a
 * native SQL query.
 *
 * @author Gavin King
 * @author Max Andersen
 * @author Steve Ebersole
 * @author Paul Benedict
 */
public class SQLQueryParser {

	private final SessionFactoryImplementor factory;
	private final String originalQueryString;
	private final ParserContext context;

	private long aliasesFound;

	public interface ParserContext {
		boolean isEntityAlias(String aliasName);
		SQLLoadable getEntityPersister(String alias);
		String getEntitySuffix(String alias);
		boolean isCollectionAlias(String aliasName);
		SQLLoadableCollection getCollectionPersister(String alias);
		String getCollectionSuffix(String alias);
		Map<String, String[]> getPropertyResultsMap(String alias);
	}

	public SQLQueryParser(String queryString, ParserContext context, SessionFactoryImplementor factory) {
		this.originalQueryString = queryString;
		this.context = context;
		this.factory = factory;
	}

	public boolean queryHasAliases() {
		return aliasesFound>0;
	}

	protected String getOriginalQueryString() {
		return originalQueryString;
	}

	public String process() {
		return substituteBrackets( originalQueryString );
	}

	// TODO: should "record" how many properties we have referred to - and if we
	//       don't get them all we throw an exception! Way better than trial and error ;)
	protected String substituteBrackets(String sqlQuery) throws QueryException {
		final String trimmed = sqlQuery.trim();
		if ( trimmed.startsWith("{") && trimmed.endsWith("}") ) {
			return sqlQuery;
		}

		final StringBuilder result = new StringBuilder( sqlQuery.length() + 20 );

		// replace {....} with corresponding column aliases
		final StringBuilder token = new StringBuilder();
		boolean singleQuoted = false;
		boolean doubleQuoted = false;
		boolean escaped = false;
		for ( int index = 0; index < sqlQuery.length(); index++ ) {
			final char ch = sqlQuery.charAt( index );
			switch (ch) {
				case '\'':
					if (escaped) {
						token.append(ch);
					}
					else {
						if (!doubleQuoted) {
							singleQuoted = !singleQuoted;
						}
						result.append(ch);
					}
					break;
				case '"':
					if (!singleQuoted && !escaped) {
						doubleQuoted = !doubleQuoted;
					}
					result.append(ch);
					break;
				case '{':
					if (!singleQuoted && !doubleQuoted) {
						escaped = true;
					}
					else {
						result.append(ch);
					}
					break;
				case '}':
					if (!singleQuoted && !doubleQuoted) {
						escaped = false;
						interpretToken( token.toString(), result );
						token.setLength(0);
					}
					else {
						result.append(ch);
					}
					break;
				default:
					if ( !escaped ) {
						result.append(ch);
					}
					else {
						token.append(ch);
					}
			}
		}
		return result.toString();
	}

	private void interpretToken(String token, StringBuilder result) {
		if ( token.startsWith("h-") ) {
			handlePlaceholder( token, result );
		}
		else if ( context != null ) {
			handleAliases( token, result );
		}
		else {
			result.append( '{' ).append( token ).append( '}' );
		}
	}

	private void handleAliases(String token, StringBuilder result) {
		final int firstDot = token.indexOf( '.' );
		if ( firstDot == -1 ) {
			if ( context.isEntityAlias(token) ) {
				// it is a simple table alias {foo}
				result.append(token);
				aliasesFound++;
			}
			else {
				// passing through anything we do not know
				// to support jdbc escape sequences HB-898
				result.append( '{' ).append(token).append( '}' );
			}
		}
		else {
			final String aliasName = token.substring( 0, firstDot );
			if ( context.isCollectionAlias( aliasName ) ) {
				// The current alias is referencing the collection to be eagerly fetched
				String propertyName = token.substring( firstDot + 1 );
				result.append( resolveCollectionProperties( aliasName, propertyName ) );
				aliasesFound++;
			}
			else if ( context.isEntityAlias( aliasName ) ) {
				// it is a property reference {foo.bar}
				String propertyName = token.substring( firstDot + 1 );
				result.append( resolveProperties( aliasName, propertyName ) );
				aliasesFound++;
			}
			else {
				// passing through anything we do not know
				// to support jdbc escape sequences HB-898
				result.append( '{' ).append(token).append( '}' );
			}
		}
	}

	private void handlePlaceholder(String token, StringBuilder result) {
		final SqlStringGenerationContext context = factory.getSqlStringGenerationContext();
		final Identifier defaultCatalog = context.getDefaultCatalog();
		final Identifier defaultSchema = context.getDefaultSchema();
		final Dialect dialect = context.getDialect();
		switch (token) {
			case "h-domain":
				if ( defaultCatalog != null ) {
					result.append( defaultCatalog.render(dialect) );
					result.append( "." );
				}
				if ( defaultSchema != null ) {
					result.append( defaultSchema.render(dialect) );
					result.append( "." );
				}
				break;
			case "h-schema":
				if ( defaultSchema != null ) {
					result.append( defaultSchema.render(dialect) );
					result.append( "." );
				}
				break;
			case "h-catalog":
				if ( defaultCatalog != null ) {
					result.append( defaultCatalog.render(dialect) );
					result.append( "." );
				}
				break;
			default:
				throw new QueryException( "Unknown placeholder ", token);
		}
	}

	private String resolveCollectionProperties(
			String aliasName,
			String propertyName) {
		final Map<String, String[]> fieldResults = context.getPropertyResultsMap( aliasName );
		final SQLLoadableCollection collectionPersister = context.getCollectionPersister( aliasName );
		final String collectionSuffix = context.getCollectionSuffix( aliasName );
		switch ( propertyName ) {
			case "*":
				if ( !fieldResults.isEmpty() ) {
					throw new QueryException( "Using return-property together with * syntax is not supported" );
				}
				aliasesFound++;
				return collectionPersister.selectFragment( aliasName, collectionSuffix )
					+ ", " + resolveProperties( aliasName, propertyName );
			case "element.*":
				return resolveProperties( aliasName, "*" );
			default:
				// Let return-properties override whatever the persister has for aliases.
				String[] columnAliases = fieldResults.get( propertyName );
				if ( columnAliases == null ) {
					columnAliases =
							collectionPersister.getCollectionPropertyColumnAliases( propertyName, collectionSuffix );
				}
				validate( aliasName, propertyName, columnAliases );
				aliasesFound++;
				return columnAliases[0];
		}
	}

	private String resolveProperties(String aliasName, String propertyName) {
		final Map<String, String[]> fieldResults = context.getPropertyResultsMap( aliasName );
		final SQLLoadable persister = context.getEntityPersister( aliasName );
		final String suffix = context.getEntitySuffix( aliasName );
		if ( "*".equals( propertyName ) ) {
			if ( !fieldResults.isEmpty() ) {
				throw new QueryException( "Using return-property together with * syntax is not supported" );
			}
			aliasesFound++;
			return persister.selectFragment( aliasName, suffix ) ;
		}
		else {
			// Let return-properties override whatever the persister has for aliases.
			String[] columnAliases = fieldResults.get( propertyName );
			if ( columnAliases == null ) {
				columnAliases = persister.getSubclassPropertyColumnAliases( propertyName, suffix );
			}
			validate( aliasName, propertyName, columnAliases );
			aliasesFound++;
			return columnAliases[0];
		}
	}

	private void validate(String aliasName, String propertyName, String[] columnAliases) {
		if ( columnAliases == null || columnAliases.length == 0 ) {
			throw new QueryException(
					"No column name found for property [" + propertyName + "] for alias [" + aliasName + "]",
					originalQueryString
			);
		}
		if ( columnAliases.length != 1 ) {
			// TODO: better error message since we actually support composites if names are explicitly listed
			throw new QueryException(
					"SQL queries only support properties mapped to a single column - property [" +
					propertyName + "] is mapped to " + columnAliases.length + " columns.",
					originalQueryString
			);
		}
	}
}
