//$Id: SQLFunctionTemplate.java 6608 2005-04-29 15:32:30Z oneovthafew $
package org.hibernate.dialect.function;

import org.hibernate.QueryException;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.type.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents HQL functions that can have different representations in different SQL dialects.
 * E.g. in HQL we can define function <code>concat(?1, ?2)</code> to concatenate two strings 
 * p1 and p2. Target SQL function will be dialect-specific, e.g. <code>(?1 || ?2)</code> for 
 * Oracle, <code>concat(?1, ?2)</code> for MySql, <code>(?1 + ?2)</code> for MS SQL.
 * Each dialect will define a template as a string (exactly like above) marking function 
 * parameters with '?' followed by parameter's index (first index is 1).
 *
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 6608 $</tt>
 */
public class SQLFunctionTemplate implements SQLFunction {
	private final Type type;
	private final boolean hasArguments;
	private final boolean hasParenthesesIfNoArgs;

	private final String template;
	private final String[] chunks;
	private final int[] paramIndexes;

	public SQLFunctionTemplate(Type type, String template) {
		this( type, template, true );
	}

	public SQLFunctionTemplate(Type type, String template, boolean hasParenthesesIfNoArgs) {
		this.type = type;
		this.template = template;

		List chunkList = new ArrayList();
		List paramList = new ArrayList();
		StringBuffer chunk = new StringBuffer( 10 );
		StringBuffer index = new StringBuffer( 2 );

		for ( int i = 0; i < template.length(); ++i ) {
			char c = template.charAt( i );
			if ( c == '?' ) {
				chunkList.add( chunk.toString() );
				chunk.delete( 0, chunk.length() );

				while ( ++i < template.length() ) {
					c = template.charAt( i );
					if ( Character.isDigit( c ) ) {
						index.append( c );
					}
					else {
						chunk.append( c );
						break;
					}
				}

				paramList.add( new Integer( Integer.parseInt( index.toString() ) - 1 ) );
				index.delete( 0, index.length() );
			}
			else {
				chunk.append( c );
			}
		}

		if ( chunk.length() > 0 ) {
			chunkList.add( chunk.toString() );
		}

		chunks = ( String[] ) chunkList.toArray( new String[chunkList.size()] );
		paramIndexes = new int[paramList.size()];
		for ( int i = 0; i < paramIndexes.length; ++i ) {
			paramIndexes[i] = ( ( Integer ) paramList.get( i ) ).intValue();
		}

		hasArguments = paramIndexes.length > 0;
		this.hasParenthesesIfNoArgs = hasParenthesesIfNoArgs;
	}

	/**
	 * Applies the template to passed in arguments.
	 * @param args function arguments
	 *
	 * @return generated SQL function call
	 */
	public String render(List args, SessionFactoryImplementor factory) {
		StringBuffer buf = new StringBuffer();
		for ( int i = 0; i < chunks.length; ++i ) {
			if ( i < paramIndexes.length ) {
				Object arg = paramIndexes[i] < args.size() ? args.get( paramIndexes[i] ) : null;
				if ( arg != null ) {
					buf.append( chunks[i] ).append( arg );
				}
			}
			else {
				buf.append( chunks[i] );
			}
		}
		return buf.toString();
	}

	// SQLFunction implementation

	public Type getReturnType(Type columnType, Mapping mapping) throws QueryException {
		return type;
	}

	public boolean hasArguments() {
		return hasArguments;
	}

	public boolean hasParenthesesIfNoArguments() {
		return hasParenthesesIfNoArgs;
	}
	
	public String toString() {
		return template;
	}
}
