/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import org.hibernate.query.QueryParameter;
import org.hibernate.query.named.spi.ParameterMemento;

/**
 * @author Steve Ebersole
 */
public interface QueryParameterImplementor<T> extends QueryParameter<T> {
	void allowMultiValuedBinding();

	ParameterMemento toMemento();
}
