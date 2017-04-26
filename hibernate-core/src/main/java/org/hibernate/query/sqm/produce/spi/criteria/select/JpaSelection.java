/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi.criteria.select;

import javax.persistence.criteria.Selection;

import org.hibernate.query.sqm.produce.spi.criteria.CriteriaVisitor;
import org.hibernate.query.sqm.produce.spi.criteria.JpaTupleElement;
import org.hibernate.query.sqm.tree.select.SqmAliasedExpressionContainer;

/**
 * @author Steve Ebersole
 */
public interface JpaSelection<T> extends Selection<T>, JpaTupleElement<T> {
	void visitSelections(CriteriaVisitor visitor, SqmAliasedExpressionContainer container);
}
