/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.from;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.spi.SqlAstNode;

import org.jboss.logging.Logger;

/**
 * Represents a groups of joined tables.  Roughly equivalent to what ANSI SQL
 * calls a {@code <table reference>}.
 * <p/>
 * We further group the individual TableSpecification references into groups to be able to
 * more easily refer to all the tables from a single entity/collection persister as a
 * single group.
 *
 * @author Steve Ebersole
 */
public class TableSpace implements SqlAstNode {
	private static final Logger log = Logger.getLogger( TableSpace.class );

	private final FromClause fromClause;

	private TableGroup rootTableGroup;
	private List<TableGroupJoin> joinedTableGroups;

	public TableSpace(FromClause fromClause) {
		if ( fromClause == null ) {
			throw new AssertionFailure( "FromClause cannot be null" );
		}
		this.fromClause = fromClause;
	}

	public FromClause getFromClause() {
		return fromClause;
	}

	public TableGroup getRootTableGroup() {
		return rootTableGroup;
	}

	public void setRootTableGroup(TableGroup rootTableGroup) {
		log.tracef(
				"Setting root TableGroup [%s] for space [%s] - was %s",
				rootTableGroup,
				this,
				this.rootTableGroup == null ? "<null>" : this.rootTableGroup
		);
		this.rootTableGroup = rootTableGroup;
	}

	public List<TableGroupJoin> getJoinedTableGroups() {
		if ( joinedTableGroups == null ) {
			return Collections.emptyList();
		}
		else {
			return Collections.unmodifiableList( joinedTableGroups );
		}
	}

	public void addJoinedTableGroup(TableGroupJoin join) {
		log.tracef( "Adding TableGroupJoin [%s] to space [%s]", join, this );
		if ( joinedTableGroups == null ) {
			joinedTableGroups = new ArrayList<>();
		}
		joinedTableGroups.add( join );
	}

	@Override
	public void accept(SqlAstWalker  sqlTreeWalker) {
		sqlTreeWalker.visitTableSpace( this );
	}
}
