/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.insert;

import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.tree.from.SqmRoot;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gavin King
 */
public class SqmInsertValuesStatement<T> extends AbstractSqmInsertStatement<T> {
	private List<SqmValues> valuesList = new ArrayList<>();

	public SqmInsertValuesStatement(SqmRoot<T> targetRoot, NodeBuilder nodeBuilder) {
		super( targetRoot, SqmQuerySource.HQL, nodeBuilder );
	}

	public List<SqmValues> getValuesList() {
		return valuesList;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitInsertValuesStatement( this );
	}

	@Override
	public JpaPredicate getRestriction() {
		return null;
	}
}
