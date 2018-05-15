/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.exec.spi.ParameterBindingContext;

/**
 * ParameterBindingContext implementation used in load-like operations
 *
 * @author Steve Ebersole
 */
public class LoadParameterBindingContext implements ParameterBindingContext {
	private final SessionFactoryImplementor sessionFactory;
	private final List<?> loadIdentifiers;

	public LoadParameterBindingContext(
			SessionFactoryImplementor sessionFactory,
			List<Object> loadIdentifiers) {
		this.sessionFactory = sessionFactory;
		this.loadIdentifiers = loadIdentifiers;
	}

	public LoadParameterBindingContext(
			SessionFactoryImplementor sessionFactory,
			Object loadIdentifier) {
		this( sessionFactory, Collections.singletonList( loadIdentifier ) );
	}


	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> List<T> getLoadIdentifiers() {
		return (List<T>) loadIdentifiers;
	}

	@Override
	public QueryParameterBindings getQueryParameterBindings() {
		return QueryParameterBindings.NO_PARAM_BINDINGS;
	}
}
