/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.javassist;

import java.lang.reflect.Modifier;

import org.hibernate.bytecode.enhance.internal.javassist.EnhancerImpl;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;

import org.jboss.logging.Logger;

/**
 * Bytecode provider implementation for Javassist.
 *
 * @author Steve Ebersole
 */
public class BytecodeProviderImpl implements BytecodeProvider {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			BytecodeProviderImpl.class.getName()
	);

	@Override
	public ProxyFactoryFactory getProxyFactoryFactory() {
		return new ProxyFactoryFactoryImpl();
	}

	@Override
	public ReflectionOptimizer getReflectionOptimizer(
			Class clazz,
			String[] getterNames,
			String[] setterNames,
			Class[] types) {
		FastClass fastClass;
		BulkAccessor bulkAccessor;
		try {
			fastClass = FastClass.create( clazz );
			bulkAccessor = BulkAccessor.create( clazz, getterNames, setterNames, types );
			if ( !clazz.isInterface() && !Modifier.isAbstract( clazz.getModifiers() ) ) {
				if ( fastClass == null ) {
					bulkAccessor = null;
				}
				else {
					//test out the optimizer:
					final Object instance = fastClass.newInstance();
					bulkAccessor.setPropertyValues( instance, bulkAccessor.getPropertyValues( instance ) );
				}
			}
		}
		catch ( Throwable t ) {
			fastClass = null;
			bulkAccessor = null;
			if ( LOG.isDebugEnabled() ) {
				int index = 0;
				if (t instanceof BulkAccessorException) {
					index = ( (BulkAccessorException) t ).getIndex();
				}
				if ( index >= 0 ) {
					LOG.debugf(
							"Reflection optimizer disabled for %s [%s: %s (property %s)]",
							clazz.getName(),
							StringHelper.unqualify( t.getClass().getName() ),
							t.getMessage(),
							setterNames[index]
					);
				}
				else {
					LOG.debugf(
							"Reflection optimizer disabled for %s [%s: %s]",
							clazz.getName(),
							StringHelper.unqualify( t.getClass().getName() ),
							t.getMessage()
					);
				}
			}
		}

		if ( fastClass != null && bulkAccessor != null ) {
			return new ReflectionOptimizerImpl(
					new InstantiationOptimizerAdapter( fastClass ),
					new AccessOptimizerAdapter( bulkAccessor, clazz )
			);
		}

		return null;
	}

	@Override
	public Enhancer getEnhancer(EnhancementContext enhancementContext) {
		return new EnhancerImpl( enhancementContext );
	}
}
