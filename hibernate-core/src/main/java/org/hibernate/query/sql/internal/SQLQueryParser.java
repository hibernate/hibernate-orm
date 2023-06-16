/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sql.internal;

import java.util.Map;
import java.util.regex.Pattern;

import org.hibernate.QueryException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.SQLLoadableCollection;
import org.hibernate.persister.entity.SQLLoadable;

/**
 * @author Gavin King
 * @author Max Andersen
 * @author Steve Ebersole
 * @author Paul Benedict
 */
public class SQLQueryParser {
	private static final Pattern PREPARED_STATEMENT_PATTERN = Pattern.compile( "^\\{.*?\\}$" );
	private static final String HIBERNATE_PLACEHOLDER_PREFIX = "h-";
	private static final String DOMAIN_PLACEHOLDER = "h-domain";
	private static final String CATALOG_PLACEHOLDER = "h-catalog";
	private static final String SCHEMA_PLACEHOLDER = "h-schema";

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
		if ( PREPARED_STATEMENT_PATTERN.matcher( sqlQuery.trim() ).matches() ) {
			return sqlQuery;
		}

		final SqlStringGenerationContext sqlStringGenerationContext = factory.getSqlStringGenerationContext();
		final Identifier defaultCatalog = sqlStringGenerationContext.getDefaultCatalog();
		final Identifier defaultSchema = sqlStringGenerationContext.getDefaultSchema();
		final Dialect dialect = sqlStringGenerationContext.getDialect();

		final StringBuilder result = new StringBuilder( sqlQuery.length() + 20 );

		int left, right;
		// replace {....} with corresponding column aliases
		for ( int curr = 0; curr < sqlQuery.length(); curr = right + 1 ) {
			if ( ( left = sqlQuery.indexOf( '{', curr ) ) < 0 ) {
				// No additional open braces found in the string, append the
				// rest of the string in its entirety and quit this loop
				result.append( sqlQuery.substring( curr ) );
				break;
			}

			// append everything up until the next encountered open brace
			result.append( sqlQuery, curr, left );

			if ( ( right = sqlQuery.indexOf( '}', left + 1 ) ) < 0 ) {
				throw new QueryException( "Unmatched braces for alias path", sqlQuery );
			}

			final String aliasPath = sqlQuery.substring( left + 1, right );
			boolean isPlaceholder = aliasPath.startsWith( HIBERNATE_PLACEHOLDER_PREFIX );

			if ( isPlaceholder ) {
				// Domain replacement
				switch ( aliasPath ) {
					case DOMAIN_PLACEHOLDER: {
						if ( defaultCatalog != null ) {
							result.append( defaultCatalog.render(dialect) );
							result.append( "." );
						}
						if ( defaultSchema != null ) {
							result.append( defaultSchema.render(dialect) );
							result.append( "." );
						}
						break;
					}
					// Schema replacement
					case SCHEMA_PLACEHOLDER: {
						if ( defaultSchema != null ) {
							result.append( defaultSchema.render(dialect) );
							result.append( "." );
						}
						break;
					}
					// Catalog replacement
					case CATALOG_PLACEHOLDER: {
						if ( defaultCatalog != null ) {
							result.append( defaultCatalog.render(dialect) );
							result.append( "." );
						}
						break;
					}
					default:
						throw new QueryException( "Unknown placeholder ", aliasPath );
				}
			}
			else if ( context != null ) {
				int firstDot = aliasPath.indexOf( '.' );
				if ( firstDot == -1 ) {
					if ( context.isEntityAlias( aliasPath ) ) {
						// it is a simple table alias {foo}
						result.append( aliasPath );
						aliasesFound++;
					}
					else {
						// passing through anything we do not know : to support jdbc escape sequences HB-898
						result.append( '{' ).append( aliasPath ).append( '}' );
					}
				}
				else {
					final String aliasName = aliasPath.substring( 0, firstDot );
					if ( context.isCollectionAlias( aliasName ) ) {
						// The current alias is referencing the collection to be eagerly fetched
						String propertyName = aliasPath.substring( firstDot + 1 );
						result.append( resolveCollectionProperties( aliasName, propertyName ) );
						aliasesFound++;
					}
					else if ( context.isEntityAlias( aliasName ) ) {
						// it is a property reference {foo.bar}
						String propertyName = aliasPath.substring( firstDot + 1 );
						result.append( resolveProperties( aliasName, propertyName ) );
						aliasesFound++;
					}
					else {
						// passing through anything we do not know : to support jdbc escape sequences HB-898
						result.append( '{' ).append( aliasPath ).append( '}' );
					}
				}
			}
			else {
				result.append( '{' ).append( aliasPath ).append( '}' );
			}
		}
		return result.toString();
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

				String selectFragment = collectionPersister.selectFragment( aliasName, collectionSuffix );
				aliasesFound++;
				return selectFragment
						+ ", "
						+ resolveProperties( aliasName, propertyName );
			case "element.*":
				return resolveProperties( aliasName, "*" );
			default: {
				String[] columnAliases;

				// Let return-properties override whatever the persister has for aliases.
				columnAliases = fieldResults.get( propertyName );
				if ( columnAliases == null ) {
					columnAliases = collectionPersister.getCollectionPropertyColumnAliases(
							propertyName,
							collectionSuffix
					);
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
	}
	private String resolveProperties(String aliasName, String propertyName) {
		final Map<String, String[]> fieldResults = context.getPropertyResultsMap( aliasName );
		final SQLLoadable persister = context.getEntityPersister( aliasName );
		final String suffix = context.getEntitySuffix( aliasName );

		if ( "*".equals( propertyName ) ) {
			if( !fieldResults.isEmpty() ) {
				throw new QueryException( "Using return-property together with * syntax is not supported" );
			}
			aliasesFound++;
			return persister.selectFragment( aliasName, suffix ) ;
		}
		else {
			String[] columnAliases;

			// Let return-propertiess override whatever the persister has for aliases.
			columnAliases = fieldResults.get( propertyName );
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

}
