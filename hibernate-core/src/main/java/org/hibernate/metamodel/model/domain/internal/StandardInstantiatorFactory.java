/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.domain.EmbeddedValueMapping;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.metamodel.model.domain.spi.Instantiator;
import org.hibernate.metamodel.model.domain.spi.InstantiatorFactory;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;

/**
 * @author Steve Ebersole
 */
public class StandardInstantiatorFactory implements InstantiatorFactory {
	/**
	 * Singleton access
	 */
	public static final StandardInstantiatorFactory INSTANCE = new StandardInstantiatorFactory();

	@Override
	public Instantiator createEmbeddableInstantiator(
			EmbeddedValueMapping bootMapping,
			EmbeddedTypeDescriptor runtimeDescriptor,
			ReflectionOptimizer.InstantiationOptimizer optimizer) {
		switch ( bootMapping.getExplicitRepresentation() ) {
			case MAP: {
				return new EmbeddableMapInstantiator( runtimeDescriptor );
			}
			case POJO: {
				return new EmbeddablePojoInstantiatorImpl( runtimeDescriptor, optimizer );
			}
		}

		throw new HibernateException( "Unexpected Representation [" + bootMapping.getExplicitRepresentation() + "] for embeddable Instantiator" );
	}

	@Override
	public Instantiator createEntityInstantiator(
			EntityMapping bootMapping,
			EntityDescriptor runtimeDescriptor,
			ReflectionOptimizer.InstantiationOptimizer optimizer) {
		switch ( bootMapping.getExplicitRepresentation() ) {
			case MAP: {
				return new EntityMapInstantiator( bootMapping, runtimeDescriptor );
			}
			case POJO: {
				return new EntityPojoInstantiator( bootMapping, runtimeDescriptor, optimizer );
			}
		}

		throw new HibernateException( "Unexpected Representation [" + bootMapping.getExplicitRepresentation() + "] for entity Instantiator" );
	}
}
