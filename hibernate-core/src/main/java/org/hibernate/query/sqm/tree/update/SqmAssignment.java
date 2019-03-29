/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.update;

import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class SqmAssignment {
	private final SqmPath targetPath;
	private final SqmExpression value;

	public SqmAssignment(SqmPath targetPath, SqmExpression value) {
		this.targetPath = targetPath;
		this.value = value;
	}

	/**
	 * The attribute/path to be updated
	 */
	public SqmPath getTargetPath() {
		return targetPath;
	}

	/**
	 * The new value
	 */
	public SqmExpression getValue() {
		return value;
	}
}
