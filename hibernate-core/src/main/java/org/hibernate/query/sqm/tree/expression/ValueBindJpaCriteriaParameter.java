/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.BindableType;
import org.hibernate.query.sqm.NodeBuilder;

/**
 * It is a JpaCriteriaParameter created from a value when ValueHandlingMode is equal to BIND
 *
 * @see org.hibernate.query.criteria.ValueHandlingMode
 */
public class ValueBindJpaCriteriaParameter<T> extends JpaCriteriaParameter<T>{
	public ValueBindJpaCriteriaParameter(
			BindableType<T> type,
			T value,
			NodeBuilder nodeBuilder) {
		super( type, value, nodeBuilder );
	}
}
