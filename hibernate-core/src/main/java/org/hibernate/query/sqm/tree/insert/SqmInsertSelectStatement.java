/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.insert;

import org.hibernate.query.sqm.tree.SqmQuerySpec;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * @author Steve Ebersole
 */
public class SqmInsertSelectStatement extends AbstractSqmInsertStatement {
	private SqmQuerySpec selectQuerySpec;

	public SqmInsertSelectStatement() {
		super();
	}

	public SqmInsertSelectStatement(SqmRoot targetRoot) {
		super( targetRoot );
	}

	public SqmQuerySpec getSelectQuerySpec() {
		return selectQuerySpec;
	}

	public void setSelectQuerySpec(SqmQuerySpec selectQuerySpec) {
		this.selectQuerySpec = selectQuerySpec;
	}
}
