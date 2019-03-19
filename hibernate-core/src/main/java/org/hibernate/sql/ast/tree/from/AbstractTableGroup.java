/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.AbstractColumnReferenceQualifier;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractTableGroup
		extends AbstractColumnReferenceQualifier
		implements TableGroup {

	private final NavigablePath navigablePath;
	private final Navigable<?> navigable;
	private final LockMode lockMode;

	private Set<TableGroupJoin> tableGroupJoins;

	public AbstractTableGroup(
			String uid,
			NavigablePath navigablePath,
			Navigable<?> navigable,
			LockMode lockMode) {
		super( uid );
		this.navigablePath = navigablePath;
		this.navigable = navigable;
		this.lockMode = lockMode;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public Navigable<?> getNavigable() {
		return navigable;
	}

	@Override
	public LockMode getLockMode() {
		return lockMode;
	}

	@Override
	public Set<TableGroupJoin> getTableGroupJoins() {
		return tableGroupJoins == null ? Collections.emptySet() : Collections.unmodifiableSet( tableGroupJoins );
	}

	@Override
	public boolean hasTableGroupJoins() {
		return tableGroupJoins != null && !tableGroupJoins.isEmpty();
	}

	@Override
	public void setTableGroupJoins(Set<TableGroupJoin> joins) {
		tableGroupJoins.addAll( joins );
	}

	@Override
	public void addTableGroupJoin(TableGroupJoin join) {
		if ( tableGroupJoins == null ) {
			tableGroupJoins = new HashSet<>();
		}
		tableGroupJoins.add( join );
	}

	@Override
	public void visitTableGroupJoins(Consumer<TableGroupJoin> consumer) {
		if ( tableGroupJoins != null ) {
			tableGroupJoins.forEach( consumer );
		}
	}

	protected void renderTableReference(TableReference tableBinding, SqlAppender sqlAppender, SqlAstWalker walker) {
		final String identificationVariable = tableBinding.getIdentificationVariable();
		String aliasString = "";
		if ( identificationVariable != null ) {
			aliasString = " as " + identificationVariable;
		}
		sqlAppender.appendSql(
				tableBinding.getTable().render(
						walker.getSessionFactory().getDialect(),
						walker.getSessionFactory().getJdbcServices().getJdbcEnvironment()
				) + aliasString
		);
	}

	@Override
	public String toLoggableFragment() {
		final StringBuilder buffer = new StringBuilder( "(" );

		buffer.append( "path=(" ).append( getNavigablePath() ).append( "), " );
		buffer.append( "root=(" ).append( getPrimaryTableReference().toLoggableFragment() ).append( "), " );
		buffer.append( "joins=[" );
		if ( getTableReferenceJoins() != null ) {
			boolean firstPass = true;
			for ( TableReferenceJoin tableReferenceJoin : getTableReferenceJoins() ) {
				if ( firstPass ) {
					firstPass = false;
				}
				else {
					buffer.append( ',' );
				}

				buffer.append( tableReferenceJoin.toLoggableFragment() );
			}
		}

		return buffer.append( "])" ).toString();
	}
}
