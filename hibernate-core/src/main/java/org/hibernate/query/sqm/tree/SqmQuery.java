/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree;

import org.hibernate.query.criteria.JpaCriteriaBase;

/**
 * Commonality between a top-level statement and a sub-query
 *
 * @author Steve Ebersole
 */
public interface SqmQuery<T> extends JpaCriteriaBase, SqmNode {
}
