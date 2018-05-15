/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.consume.spi;

import java.util.Collections;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.exec.spi.ParameterBindingContext;

/**
 * @author Steve Ebersole
 */
public class StandardParameterBindingContext implements ParameterBindingContext {
	private final SessionFactoryImplementor sessionFactory;
	private final QueryParameterBindings parameterBindings;
	private final List loadIdentifiers;

	public StandardParameterBindingContext(
			SessionFactoryImplementor sessionFactory,
			QueryParameterBindings parameterBindings,
			Object loadIdentifier) {
		this( sessionFactory, parameterBindings, Collections.singletonList( loadIdentifier ) );
	}

	public StandardParameterBindingContext(
			SessionFactoryImplementor sessionFactory,
			QueryParameterBindings parameterBindings,
			List loadIdentifiers) {
		this.sessionFactory = sessionFactory;
		this.parameterBindings = parameterBindings;
		this.loadIdentifiers = loadIdentifiers;
	}

	@Override
	public List getLoadIdentifiers() {
		return loadIdentifiers;
	}

	@Override
	public QueryParameterBindings getQueryParameterBindings() {
		return parameterBindings;
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}
}
