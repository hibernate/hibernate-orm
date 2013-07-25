/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.loader.plan2.exec.query.internal;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.sql.SelectFragment;

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

	public void appendSelectClauseFragment(SelectFragment selectFragment) {
		appendSelectClauseFragment( selectFragment.toFragmentString().substring( 2 ) );
	}

	public void appendFromClauseFragment(String fragment) {
		if ( this.fromClause.length() > 0 ) {
			this.fromClause.append( ", " );
			this.guesstimatedBufferSize += 2;
		}
		this.fromClause.append( fragment );
		this.guesstimatedBufferSize += fragment.length();
	}

	public void appendFromClauseFragment(String tableName, String alias) {
		appendFromClauseFragment( tableName + ' ' + alias );
	}

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
		if ( restrictions.startsWith( "and" ) ) {
			restrictions = restrictions.substring( 4 );
		}
		if ( restrictions.endsWith( "and" ) ) {
			restrictions = restrictions.substring( 0, restrictions.length()-4 );
		}

		return restrictions;
	}

//	public void appendOuterJoins(String outerJoinsAfterFrom, String outerJoinsAfterWhere) {
//		appendOuterJoinsAfterFrom( outerJoinsAfterFrom );
//		appendOuterJoinsAfterWhere( outerJoinsAfterWhere );
//	}
//
//	private void appendOuterJoinsAfterFrom(String outerJoinsAfterFrom) {
//		if ( this.outerJoinsAfterFrom == null ) {
//			this.outerJoinsAfterFrom = new StringBuilder( outerJoinsAfterFrom );
//		}
//		else {
//			this.outerJoinsAfterFrom.append( ' ' ).append( outerJoinsAfterFrom );
//		}
//	}
//
//	private void appendOuterJoinsAfterWhere(String outerJoinsAfterWhere) {
//		final String cleaned = cleanRestrictions( outerJoinsAfterWhere );
//
//		if ( this.outerJoinsAfterWhere == null ) {
//			this.outerJoinsAfterWhere = new StringBuilder( cleaned );
//		}
//		else {
//			this.outerJoinsAfterWhere.append( " and " ).append( cleaned );
//			this.guesstimatedBufferSize += 5;
//		}
//
//		this.guesstimatedBufferSize += cleaned.length();
//	}

	public void setOuterJoins(String outerJoinsAfterFrom, String outerJoinsAfterWhere) {
		this.outerJoinsAfterFrom = outerJoinsAfterFrom;

		final String cleanRestrictions = cleanRestrictions( outerJoinsAfterWhere );
		this.outerJoinsAfterWhere = cleanRestrictions;

		this.guesstimatedBufferSize += outerJoinsAfterFrom.length() + cleanRestrictions.length();
	}

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

	public void setComment(String comment) {
		this.comment = comment;
		this.guesstimatedBufferSize += comment.length();
	}

	public void setLockMode(LockMode lockMode) {
		this.lockOptions.setLockMode( lockMode );
	}

	public void setLockOptions(LockOptions lockOptions) {
		LockOptions.copy( lockOptions, this.lockOptions );
	}

	/**
	 * Construct an SQL <tt>SELECT</tt> statement from the given clauses
	 */
	public String toStatementString() {
		final StringBuilder buf = new StringBuilder( guesstimatedBufferSize );

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
			buf.append( dialect.getForUpdateString( lockOptions ) );
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
