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

import org.hibernate.query.sqm.tree.AbstractSqmDmlStatement;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReference;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * Convenience base class for InsertSqmStatement implementations.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSqmInsertStatement extends AbstractSqmDmlStatement implements SqmInsertStatement {
	private List<SqmSingularAttributeReference> insertionTargetPaths;

	@SuppressWarnings("WeakerAccess")
	protected AbstractSqmInsertStatement() {
		super();
	}

	@SuppressWarnings("WeakerAccess")
	protected AbstractSqmInsertStatement(SqmRoot targetRoot) {
		super( targetRoot );
	}

	@Override
	public List<SqmSingularAttributeReference> getInsertionTargetPaths() {
		return insertionTargetPaths == null ? Collections.emptyList() : Collections.unmodifiableList(
				insertionTargetPaths );
	}

	public void setInsertionTargetPaths(List<SqmSingularAttributeReference> insertionTargetPaths) {
		this.insertionTargetPaths = insertionTargetPaths;
	}

	public void addInsertTargetStateField(SqmSingularAttributeReference stateField) {
		if ( insertionTargetPaths == null ) {
			insertionTargetPaths = new ArrayList<>();
		}
		insertionTargetPaths.add( stateField );
	}

	@Override
	public void visitInsertionTargetPaths(Consumer<SqmSingularAttributeReference> consumer) {
		if ( insertionTargetPaths != null ) {
			insertionTargetPaths.forEach( consumer );
		}
	}
}
