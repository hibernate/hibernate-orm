/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.predicate;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstWalker;

/**
 * @author Steve Ebersole
 */
public class Junction implements Predicate {
	public enum Nature {
		/**
		 * An AND
		 */
		CONJUNCTION,
		/**
		 * An OR
		 */
		DISJUNCTION
	}

	private final Nature nature;
	private final JdbcMappingContainer expressionType;
	private final List<Predicate> predicates;

	public Junction() {
		this( Nature.CONJUNCTION );
	}

	public Junction(Nature nature) {
		this( nature, null );
	}

	public Junction(Nature nature, JdbcMappingContainer expressionType) {
		this.nature = nature;
		this.expressionType = expressionType;
		this.predicates = new ArrayList<>();
	}

	public Junction(
			Nature nature,
			List<Predicate> predicates,
			JdbcMappingContainer expressionType) {
		this.nature = nature;
		this.expressionType = expressionType;
		this.predicates = predicates;
	}

	public void add(Predicate predicate) {
		predicates.add( predicate );
	}

	public Nature getNature() {
		return nature;
	}

	public List<Predicate> getPredicates() {
		return predicates;
	}

	@Override
	public boolean isEmpty() {
		return predicates.isEmpty();
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitJunction( this );
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return expressionType;
	}
}
