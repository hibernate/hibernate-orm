/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.from.TableReference;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class InsertSelectStatement implements MutationStatement {
	private static final Logger log = Logger.getLogger( InsertSelectStatement.class );

	private TableReference targetTable;
	private List<ColumnReference> targetColumnReferences;
	private QuerySpec sourceSelectStatement = new QuerySpec( true );

	public TableReference getTargetTable() {
		return targetTable;
	}

	public void setTargetTable(TableReference targetTable) {
		log.tracef( "Setting INSERT-SELECT target table [%s]", targetTable );
		if ( this.targetTable != null ) {
			log.debugf( "INSERT-SELECT target table has been set multiple times" );
		}
		this.targetTable = targetTable;
	}

	public List<ColumnReference> getTargetColumnReferences() {
		return targetColumnReferences == null ? Collections.emptyList() : targetColumnReferences;
	}

	public void addTargetColumnReferences(ColumnReference... references) {
		if ( targetColumnReferences == null ) {
			targetColumnReferences = new ArrayList<>();
		}

		Collections.addAll( this.targetColumnReferences, references );
	}

	public void addTargetColumnReferences(List<ColumnReference> references) {
		if ( targetColumnReferences == null ) {
			targetColumnReferences = new ArrayList<>();
		}

		this.targetColumnReferences.addAll( references );
	}

	public QuerySpec getSourceSelectStatement() {
		return sourceSelectStatement;
	}

	public void setSourceSelectStatement(QuerySpec sourceSelectStatement) {
		this.sourceSelectStatement = sourceSelectStatement;
	}
}
