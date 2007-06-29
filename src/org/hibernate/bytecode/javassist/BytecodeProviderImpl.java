package org.hibernate.bytecode.javassist;

import java.lang.reflect.Modifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

	private static final Log log = LogFactory.getLog( BytecodeProviderImpl.class );

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
