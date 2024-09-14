/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.embeddable.strategy.instantiator.embeddable;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.ValueAccess;

/**
 * @author Steve Ebersole
 */
public class NameInstantiator implements EmbeddableInstantiator {
	@Override
	public Object instantiate(ValueAccess valueAccess, SessionFactoryImplementor sessionFactory) {
		// alphabetical
		final String first = valueAccess.getValue( 0, String.class );
		final String last = valueAccess.getValue( 1, String.class );
		return new Name( first, last );
	}

	@Override
	public boolean isInstance(Object object, SessionFactoryImplementor sessionFactory) {
		return object instanceof Name;
	}

	@Override
	public boolean isSameClass(Object object, SessionFactoryImplementor sessionFactory) {
		return object.getClass().equals( Name.class );
	}
}
