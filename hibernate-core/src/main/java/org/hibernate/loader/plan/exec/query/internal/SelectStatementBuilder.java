/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.query.internal;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;

/**
 * Largely a copy of the {@link org.hibernate.sql.Select} class, but changed up slightly to better meet needs
 * of building a SQL SELECT statement from a LoadPlan
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class SelectStatementBuilder {
	public final Dialect dialect;

	private StringBuilder selectClause = new StringBuilder();
	private StringBuilder fromClause = new StringBuilder();
//	private StringBuilder outerJoinsAfterFrom;
	private String outerJoinsAfterFrom;
	private StringBuilder whereClause;
//	private StringBuilder outerJoinsAfterWhere;
	private String outerJoinsAfterWhere;
	private StringBuilder orderByClause;
	private String comment;
	private LockOptions lockOptions = new LockOptions();

	private int guesstimatedBufferSize = 20;

	/**
	 * Constructs a select statement builder object.
	 *
	 * @param dialect The dialect.
	 */
	public SelectStatementBuilder(Dialect dialect) {
		this.dialect = dialect;
	}

	/**
	 * Appends a select clause fragment
	 *
	 * @param selection The selection fragment
	 */
	public void appendSelectClauseFragment(String selection) {
		if ( this.selectClause.length() > 0 ) {
			this.selectClause.append( ", " );
			this.guesstimatedBufferSize += 2;
		}
		this.selectClause.append( selection );
		this.guesstimatedBufferSize += selection.length();
	}

	/**
	 * Appends the from clause fragment.
	 *
	 * @param fragment The from cause fragment.
	 */
	public void appendFromClauseFragment(String fragment) {
		if ( this.fromClause.length() > 0 ) {
			this.fromClause.append( ", " );
			this.guesstimatedBufferSize += 2;
		}
		this.fromClause.append( fragment );
		this.guesstimatedBufferSize += fragment.length();
	}

	/**
	 * Appends the specified table name and alias as a from clause fragment.
	 *
	 * @param tableName The table name.
	 * @param alias The table alias.
	 */
	public void appendFromClauseFragment(String tableName, String alias) {
		appendFromClauseFragment( tableName + ' ' + alias );
	}

	/**
	 * Appends the specified restrictions after "cleaning" the specified value
	 * (by trimming and removing 'and ' from beginning and ' and' from the end).
	 * If the where clause already exists, this method ensure that ' and '
	 * prefixes the cleaned restrictions.
	 *
	 * @param restrictions The restrictions.
	 */
	public void appendRestrictions(String restrictions) {
		final String cleaned = cleanRestrictions( restrictions );
		if ( StringHelper.isEmpty( cleaned ) ) {
			return;
		}

		this.guesstimatedBufferSize += cleaned.length();

		if ( whereClause == null ) {
			whereClause = new StringBuilder( cleaned );
		}
		else {
			whereClause.append( " and " ).append( cleaned );
			this.guesstimatedBufferSize += 5;
		}
	}

	private String cleanRestrictions(String restrictions) {
		restrictions = restrictions.trim();
		if ( restrictions.startsWith( "and " ) ) {
			restrictions = restrictions.substring( 4 );
		}
		if ( restrictions.endsWith( " and" ) ) {
			restrictions = restrictions.substring( 0, restrictions.length()-4 );
		}

		return restrictions;
	}

	/**
	 * Sets the outer join fragments to be added to the "from" and "where" clauses.
	 *
	 * @param outerJoinsAfterFrom The outer join fragment to be appended to the "from" clause.
	 * @param outerJoinsAfterWhere The outer join fragment to be appended to the "where" clause.
	 */
	public void setOuterJoins(String outerJoinsAfterFrom, String outerJoinsAfterWhere) {
		this.outerJoinsAfterFrom = outerJoinsAfterFrom;

		final String cleanRestrictions = cleanRestrictions( outerJoinsAfterWhere );
		this.outerJoinsAfterWhere = cleanRestrictions;

		this.guesstimatedBufferSize += outerJoinsAfterFrom.length() + cleanRestrictions.length();
	}

	/**
	 * Appends the "order by" fragment, prefixed by a comma if the "order by" fragment already
	 * exists.
	 *
	 * @param ordering The "order by" fragment to append.
	 */
	public void appendOrderByFragment(String ordering) {
		if ( this.orderByClause == null ) {
			this.orderByClause = new StringBuilder();
		}
		else {
			this.orderByClause.append( ", " );
			this.guesstimatedBufferSize += 2;
		}
		this.orderByClause.append( ordering );
	}

	/**
	 * Sets the comment for the select statement.
	 *
	 * @param comment The comment.
	 */
	public void setComment(String comment) {
		this.comment = comment;
		this.guesstimatedBufferSize += comment.length();
	}

	/**
	 * Sets the lock mode for the select statement.
	 *
	 * @param lockMode The lock mode.
	 */
	public void setLockMode(LockMode lockMode) {
		this.lockOptions.setLockMode( lockMode );
	}

	/**
	 * Sets the lock options for the select statement.
	 *
	 * @param lockOptions The lock options.
	 */
	public void setLockOptions(LockOptions lockOptions) {
		LockOptions.copy( lockOptions, this.lockOptions );
	}

	/**
	 * Construct an SQL <tt>SELECT</tt> statement from the given clauses.
	 *
	 * @return the SQL <tt>SELECT</tt> statement.
	 */
	public String toStatementString() {
		StringBuilder buf = new StringBuilder( guesstimatedBufferSize );

		if ( StringHelper.isNotEmpty( comment ) ) {
			buf.append( "/* " ).append( comment ).append( " */ " );
		}

		buf.append( "select " )
				.append( selectClause )
				.append( " from " )
				.append( fromClause );

		if ( StringHelper.isNotEmpty( outerJoinsAfterFrom ) ) {
			buf.append( outerJoinsAfterFrom );
		}

		if ( isNotEmpty( whereClause ) || isNotEmpty( outerJoinsAfterWhere ) ) {
			buf.append( " where " );
			// the outerJoinsAfterWhere needs to come before where clause to properly
			// handle dynamic filters
			if ( StringHelper.isNotEmpty( outerJoinsAfterWhere ) ) {
				buf.append( outerJoinsAfterWhere );
				if ( isNotEmpty( whereClause ) ) {
					buf.append( " and " );
				}
			}
			if ( isNotEmpty( whereClause ) ) {
				buf.append( whereClause );
			}
		}

		if ( orderByClause != null ) {
			buf.append( " order by " ).append( orderByClause );
		}

		if ( lockOptions.getLockMode() != LockMode.NONE ) {
			buf = new StringBuilder(dialect.applyLocksToSql( buf.toString(), lockOptions, null ) );
		}

		return dialect.transformSelectString( buf.toString() );
	}

	private boolean isNotEmpty(String string) {
		return StringHelper.isNotEmpty( string );
	}

	private boolean isNotEmpty(StringBuilder builder) {
		return builder != null && builder.length() > 0;
	}
}
