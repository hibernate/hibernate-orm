/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.insert;

import org.hibernate.query.sqm.tree.expression.SqmExpression;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gavin King
 */
public class SqmValues implements Serializable {
	private final List<SqmExpression<?>> expressions = new ArrayList<>();

	public List<SqmExpression<?>> getExpressions() {
		return expressions;
	}
}
