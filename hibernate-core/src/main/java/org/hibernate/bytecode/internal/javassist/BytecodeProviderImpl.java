/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.javassist;

import java.lang.reflect.Modifier;
import java.util.Set;

import org.hibernate.bytecode.buildtime.spi.ClassFilter;
import org.hibernate.bytecode.buildtime.spi.FieldFilter;
import org.hibernate.bytecode.instrumentation.internal.javassist.JavassistHelper;
import org.hibernate.bytecode.instrumentation.spi.FieldInterceptor;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ClassTransformer;
import org.hibernate.bytecode.spi.EntityInstrumentationMetadata;
import org.hibernate.bytecode.spi.NotInstrumentedException;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.engine.spi.SessionImplementor;
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
	public ClassTransformer getTransformer(ClassFilter classFilter, FieldFilter fieldFilter) {
		return new JavassistClassTransformer( classFilter, fieldFilter );
	}

	@Override
	public EntityInstrumentationMetadata getEntityInstrumentationMetadata(Class entityClass) {
		return new EntityInstrumentationMetadataImpl( entityClass );
	}

	private class EntityInstrumentationMetadataImpl implements EntityInstrumentationMetadata {
		private final Class entityClass;
		private final boolean isInstrumented;

		private EntityInstrumentationMetadataImpl(Class entityClass) {
			this.entityClass = entityClass;
			this.isInstrumented = FieldHandled.class.isAssignableFrom( entityClass );
		}

		@Override
		public String getEntityName() {
			return entityClass.getName();
		}

		@Override
		public boolean isInstrumented() {
			return isInstrumented;
		}

		@Override
		public FieldInterceptor extractInterceptor(Object entity) throws NotInstrumentedException {
			if ( !entityClass.isInstance( entity ) ) {
				throw new IllegalArgumentException(
						String.format(
								"Passed entity instance [%s] is not of expected type [%s]",
								entity,
								getEntityName()
						)
				);
			}
			if ( ! isInstrumented() ) {
				throw new NotInstrumentedException( String.format( "Entity class [%s] is not instrumented", getEntityName() ) );
			}
			return JavassistHelper.extractFieldInterceptor( entity );
		}

		@Override
		public FieldInterceptor injectInterceptor(
				Object entity,
				String entityName,
				Set uninitializedFieldNames,
				SessionImplementor session) throws NotInstrumentedException {
			if ( !entityClass.isInstance( entity ) ) {
				throw new IllegalArgumentException(
						String.format(
								"Passed entity instance [%s] is not of expected type [%s]",
								entity,
								getEntityName()
						)
				);
			}
			if ( ! isInstrumented() ) {
				throw new NotInstrumentedException( String.format( "Entity class [%s] is not instrumented", getEntityName() ) );
			}
			return JavassistHelper.injectFieldInterceptor( entity, entityName, uninitializedFieldNames, session );
		}
	}
}
