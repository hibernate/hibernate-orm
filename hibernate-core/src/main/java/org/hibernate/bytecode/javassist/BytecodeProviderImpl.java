/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.bytecode.javassist;

import java.lang.reflect.Modifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.bytecode.BytecodeProvider;
import org.hibernate.bytecode.ClassTransformer;
import org.hibernate.bytecode.ProxyFactoryFactory;
import org.hibernate.bytecode.ReflectionOptimizer;
import org.hibernate.bytecode.util.ClassFilter;
import org.hibernate.bytecode.util.FieldFilter;
import org.hibernate.util.StringHelper;

/**
 * Bytecode provider implementation for Javassist.
 *
 * @author Steve Ebersole
 */
public class BytecodeProviderImpl implements BytecodeProvider {

	private static final Logger log = LoggerFactory.getLogger( BytecodeProviderImpl.class );

	public ProxyFactoryFactory getProxyFactoryFactory() {
		return new ProxyFactoryFactoryImpl();
	}

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
					Object instance = fastClass.newInstance();
					bulkAccessor.setPropertyValues( instance, bulkAccessor.getPropertyValues( instance ) );
				}
			}
		}
		catch ( Throwable t ) {
			fastClass = null;
			bulkAccessor = null;
			String message = "reflection optimizer disabled for: " +
			                 clazz.getName() +
			                 " [" +
			                 StringHelper.unqualify( t.getClass().getName() ) +
			                 ": " +
			                 t.getMessage();

			if ( t instanceof BulkAccessorException ) {
				int index = ( ( BulkAccessorException ) t ).getIndex();
				if ( index >= 0 ) {
					message += " (property " + setterNames[index] + ")";
				}
			}

			log.debug( message );
		}

		if ( fastClass != null && bulkAccessor != null ) {
			return new ReflectionOptimizerImpl(
					new InstantiationOptimizerAdapter( fastClass ),
			        new AccessOptimizerAdapter( bulkAccessor, clazz )
			);
		}
		else {
			return null;
		}
	}

	public ClassTransformer getTransformer(ClassFilter classFilter, FieldFilter fieldFilter) {
		return new JavassistClassTransformer( classFilter, fieldFilter );
	}

}
