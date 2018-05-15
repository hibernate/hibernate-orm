/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.mode;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.metamodel.model.domain.spi.Instantiator;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractPojoInstantiator implements Instantiator {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( AbstractPojoInstantiator.class );

	private final Class mappedPojoClass;
	private final boolean isAbstract;

	public AbstractPojoInstantiator(Class mappedPojoClass) {
		this.mappedPojoClass = mappedPojoClass;
		this.isAbstract = ReflectHelper.isAbstractClass( mappedPojoClass );
	}

	public Class getMappedPojoClass() {
		return mappedPojoClass;
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	@Override
	public boolean isInstance(Object object, SessionFactoryImplementor sessionFactory) {
		return mappedPojoClass.isInstance( object );
	}
}
