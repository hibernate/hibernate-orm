/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.jboss.as.jpa.hibernate5.management;

/**
 * Represents the Hibernate query name which is passed in as a parameter.  The displayQuery can be obtained which
 * has spaces and other symbols replaced with a textual description (which shouldn't be changed or localized.
 * The localization rule is so that one set of admin scripts will work against any back end system.  If it becomes
 * more important to localize the textual descriptions, care should be taken to avoid duplicate values when doing so.
 *
 * @author Scott Marlow
 */
public class QueryName {

	// query name as returned from hibernate Statistics.getQueries()
	private final String hibernateQuery;

	// query name transformed for display use (allowed to be ugly but should be unique)
	private final String displayQuery;

	// HQL symbol or operators
	private static final String SQL_NE = "<>";
	private static final String NE_BANG = "!=";
	private static final String NE_HAT = "^=";
	private static final String LE = "<=";
	private static final String GE = ">=";
	private static final String CONCAT = "||";
	private static final String LT = "<";
	private static final String EQ = "=";
	private static final String GT = ">";
	private static final String OPEN = "(";
	private static final String CLOSE = ")";
	private static final String OPEN_BRACKET = "[";
	private static final String CLOSE_BRACKET = "]";
	private static final String PLUS = "+";
	private static final String MINUS = "-";
	private static final String STAR = "*";
	private static final String DIV = "/";
	private static final String MOD = "%";
	private static final String COLON = ":";
	private static final String PARAM = "?";
	private static final String COMMA = ",";
	private static final String SPACE = " ";
	private static final String TAB = "\t";
	private static final String NEWLINE = "\n";
	private static final String LINEFEED = "\r";
	private static final String QUOTE = "'";
	private static final String DQUOTE = "\"";
	private static final String TICK = "`";
	private static final String OPEN_BRACE = "{";
	private static final String CLOSE_BRACE = "}";
	private static final String HAT = "^";
	private static final String AMPERSAND = "&";

	// textual representation (not to be localized as we don't won't duplication between any of the values)
	private static final String NOT_EQUAL__ = "_not_equal_";
	private static final String BANG_NOT_EQUAL__ = "_bang_not_equal_";
	private static final String HAT_NOT_EQUAL__ = "_hat_not_equal_";
	private static final String LESS_THAN_EQUAL__ = "_less_than_equal_";
	private static final String GREATER_THAN_EQUAL__ = "_greater_than_equal_";
	private static final String CONCAT__ = "_concat_";
	private static final String LESS_THAN__ = "_less_than_";
	private static final String EQUAL__ = "_equal_";
	private static final String GREATER__ = "_greater_";
	private static final String LEFT_PAREN__ = "_left_paren_";
	private static final String RIGHT_PAREN__ = "_right_paren_";
	private static final String LEFT_BRACKET__ = "_left_bracket_";
	private static final String RIGHT_BRACKET__ = "_right_bracket_";
	private static final String PLUS__ = "_plus_";
	private static final String MINUS__ = "_minus_";
	private static final String STAR__ = "_star_";
	private static final String DIVIDE__ = "_divide_";
	private static final String MODULUS__ = "_modulus_";
	private static final String COLON__ = "_colon_";
	private static final String PARAM__ = "_param_";
	private static final String COMMA__ = "_comma_";
	private static final String SPACE__ = "_space_";
	private static final String TAB__ = "_tab_";
	private static final String NEWLINE__ = "_newline_";
	private static final String LINEFEED__ = "_linefeed_";
	private static final String QUOTE__ = "_quote_";
	private static final String DQUOTE__ = "_double_quote_";
	private static final String TICK__ = "_tick_";
	private static final String OPEN_BRACE__ = "_left_brace_";
	private static final String CLOSE_BRACE__ = "_right_brace_";
	private static final String HAT__ = "_hat_";
	private static final String AMPERSAND__ = "_ampersand_";

	public static QueryName queryName(String query) {
		return new QueryName( query );
	}

	/**
	 * Construct
	 */
	public QueryName(String query) {
		hibernateQuery = query;
		displayQuery = displayable( query );

	}

	public String getDisplayName() {
		return displayQuery;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		QueryName queryName = (QueryName) o;

		if ( displayQuery != null ? !displayQuery.equals( queryName.displayQuery ) : queryName.displayQuery != null ) {
			return false;
		}
		if ( hibernateQuery != null ?
				!hibernateQuery.equals( queryName.hibernateQuery ) :
				queryName.hibernateQuery != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = hibernateQuery != null ? hibernateQuery.hashCode() : 0;
		result = 31 * result + ( displayQuery != null ? displayQuery.hashCode() : 0 );
		return result;
	}

	/**
	 * transform a Hibernate HQL query into something that can be displayed/used for management operations
	 */
	private String displayable(String query) {
		if ( query == null ||
				query.length() == 0 ) {
			return query;
		}

		StringBuilder buff = new StringBuilder( query );

		// handle two character transforms first
		subst( buff, SQL_NE, NOT_EQUAL__ );
		subst( buff, NE_BANG, BANG_NOT_EQUAL__ );
		subst( buff, NE_HAT, HAT_NOT_EQUAL__ );
		subst( buff, LE, LESS_THAN_EQUAL__ );
		subst( buff, GE, GREATER_THAN_EQUAL__ );
		subst( buff, CONCAT, CONCAT__ );
		subst( buff, LT, LESS_THAN__ );
		subst( buff, EQ, EQUAL__ );
		subst( buff, GT, GREATER__ );
		subst( buff, OPEN, LEFT_PAREN__ );
		subst( buff, CLOSE, RIGHT_PAREN__ );
		subst( buff, OPEN_BRACKET, LEFT_BRACKET__ );
		subst( buff, CLOSE_BRACKET, RIGHT_BRACKET__ );
		subst( buff, PLUS, PLUS__ );
		subst( buff, MINUS, MINUS__ );
		subst( buff, STAR, STAR__ );
		subst( buff, DIV, DIVIDE__ );
		subst( buff, MOD, MODULUS__ );
		subst( buff, COLON, COLON__ );
		subst( buff, PARAM, PARAM__ );
		subst( buff, COMMA, COMMA__ );
		subst( buff, SPACE, SPACE__ );
		subst( buff, TAB, TAB__ );
		subst( buff, NEWLINE, NEWLINE__ );
		subst( buff, LINEFEED, LINEFEED__ );
		subst( buff, QUOTE, QUOTE__ );
		subst( buff, DQUOTE, DQUOTE__ );
		subst( buff, TICK, TICK__ );
		subst( buff, OPEN_BRACE, OPEN_BRACE__ );
		subst( buff, CLOSE_BRACE, CLOSE_BRACE__ );
		subst( buff, HAT, HAT__ );
		subst( buff, AMPERSAND, AMPERSAND__ );
		return buff.toString();
	}

	/**
	 * Substitute sub-strings inside of a string.
	 *
	 * @param stringBuilder String buffer to use for substitution (buffer is not reset)
	 * @param from String to substitute from
	 * @param to String to substitute to
	 */
	private static void subst(final StringBuilder stringBuilder, final String from, final String to) {
		int begin = 0, end = 0;

		while ( ( end = stringBuilder.indexOf( from, end ) ) != -1 ) {
			stringBuilder.delete( end, end + from.length() );
			stringBuilder.insert( end, to );

			// update positions
			begin = end + to.length();
			end = begin;
		}
	}

}
