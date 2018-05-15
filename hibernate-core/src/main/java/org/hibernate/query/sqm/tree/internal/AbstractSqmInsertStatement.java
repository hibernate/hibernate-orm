/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.query.sqm.tree.SqmInsertStatement;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReference;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * Convenience base class for InsertSqmStatement implementations.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSqmInsertStatement extends AbstractSqmStatement implements SqmInsertStatement {
	private final SqmRoot insertTarget;
	private List<SqmSingularAttributeReference> stateFields;

	public AbstractSqmInsertStatement(SqmRoot insertTarget) {
		this.insertTarget = insertTarget;
	}

	@Override
	public SqmRoot getInsertTarget() {
		return insertTarget;
	}

	@Override
	public List<SqmSingularAttributeReference> getStateFields() {
		if ( stateFields == null ) {
			return Collections.emptyList();
		}
		else {
			return Collections.unmodifiableList( stateFields );
		}
	}

	public void addInsertTargetStateField(SqmSingularAttributeReference stateField) {
		if ( stateFields == null ) {
			stateFields = new ArrayList<>();
		}
		stateFields.add( stateField );
	}
}
