/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.insert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.tree.AbstractSqmDmlStatement;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * Convenience base class for InsertSqmStatement implementations.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSqmInsertStatement<T> extends AbstractSqmDmlStatement<T> implements SqmInsertStatement<T> {
	private List<SqmPath<?>> insertionTargetPaths;

	protected AbstractSqmInsertStatement(SqmQuerySource querySource, NodeBuilder nodeBuilder) {
		super( querySource, nodeBuilder );
	}

	protected AbstractSqmInsertStatement(SqmRoot<T> targetRoot, SqmQuerySource querySource, NodeBuilder nodeBuilder) {
		super( targetRoot, querySource, nodeBuilder );
	}

	@Override
	public List<SqmPath<?>> getInsertionTargetPaths() {
		return insertionTargetPaths == null
				? Collections.emptyList()
				: Collections.unmodifiableList( insertionTargetPaths );
	}

	public void setInsertionTargetPaths(List<SqmPath<?>> insertionTargetPaths) {
		this.insertionTargetPaths = insertionTargetPaths;
	}

	public void addInsertTargetStateField(SqmPath<?> stateField) {
		if ( insertionTargetPaths == null ) {
			insertionTargetPaths = new ArrayList<>();
		}
		insertionTargetPaths.add( stateField );
	}

	@Override
	public void visitInsertionTargetPaths(Consumer<SqmPath<?>> consumer) {
		if ( insertionTargetPaths != null ) {
			insertionTargetPaths.forEach( consumer );
		}
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( "insert into " );
		sb.append( getTarget().getEntityName() );
		if ( insertionTargetPaths != null && !insertionTargetPaths.isEmpty() ) {
			sb.append( '(' );
			insertionTargetPaths.get( 0 ).appendHqlString( sb );
			for ( int i = 1; i < insertionTargetPaths.size(); i++ ) {
				sb.append( ", " );
				insertionTargetPaths.get( i ).appendHqlString( sb );
			}
			sb.append( ')' );
		}
	}
}
