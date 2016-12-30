/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;
import java.util.List;

import org.hibernate.query.criteria.JpaTupleElementImplementor;
import org.hibernate.query.criteria.internal.ValueHandlerFactory;
import org.hibernate.sqm.parser.criteria.tree.select.JpaSelection;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public interface JpaSelectionImplementor<X> extends JpaTupleElementImplementor<X>, JpaSelection<X> {
	public List<ValueHandlerFactory.ValueHandler> getValueHandlers();
}
