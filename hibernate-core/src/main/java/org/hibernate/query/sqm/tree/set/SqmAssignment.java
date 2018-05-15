/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.set;

import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReference;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class SqmAssignment {
	private final SqmSingularAttributeReference stateField;
	private final SqmExpression value;

	public SqmAssignment(SqmSingularAttributeReference stateField, SqmExpression value) {
		this.stateField = stateField;
		this.value = value;
	}

	public SqmSingularAttributeReference getStateField() {
		return stateField;
	}

	public SqmExpression getValue() {
		return value;
	}
}
