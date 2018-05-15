/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.exec.spi.ParameterBindingContext;

/**
 * @author Steve Ebersole
 */
public class TemplateParameterBindingContext implements ParameterBindingContext {
	private static final Object ID_VALUE_TOKEN = new Object();

	private final SessionFactoryImplementor sessionFactory;
	private final List idListPrototype;

	public TemplateParameterBindingContext(SessionFactoryImplementor sessionFactory) {
		this( sessionFactory, -1 );
	}

	public TemplateParameterBindingContext(SessionFactoryImplementor sessionFactory, int expectedIdentifierCount) {
		this.sessionFactory = sessionFactory;
		if ( expectedIdentifierCount < 1 ) {
			this.idListPrototype = Collections.emptyList();
		}
		else {
			this.idListPrototype = CollectionHelper.arrayList( expectedIdentifierCount );
			Collections.fill( idListPrototype, ID_VALUE_TOKEN );
		}
	}

	@Override
	public <T> List<T> getLoadIdentifiers() {
		return idListPrototype;
	}

	@Override
	public QueryParameterBindings getQueryParameterBindings() {
		return QueryParameterBindings.NO_PARAM_BINDINGS;
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}
}
