package org.hibernate.bytecode.cglib;

import java.lang.reflect.Modifier;

import net.sf.cglib.beans.BulkBean;
import net.sf.cglib.beans.BulkBeanException;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.transform.ClassFilter;
import net.sf.cglib.transform.ClassTransformer;
import net.sf.cglib.transform.ClassTransformerFactory;
import net.sf.cglib.transform.TransformingClassLoader;
import net.sf.cglib.transform.impl.InterceptFieldFilter;
import net.sf.cglib.transform.impl.InterceptFieldTransformer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.bytecode.BytecodeProvider;
import org.hibernate.bytecode.ProxyFactoryFactory;
import org.hibernate.bytecode.ReflectionOptimizer;
import org.hibernate.bytecode.util.FieldFilter;
import org.hibernate.util.StringHelper;
import org.objectweb.asm.Type;

/**
 * Bytecode provider implementation for CGLIB.
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
		BulkBean bulkBean;
		try {
			fastClass = FastClass.create( clazz );
			bulkBean = BulkBean.create( clazz, getterNames, setterNames, types );
			if ( !clazz.isInterface() && !Modifier.isAbstract( clazz.getModifiers() ) ) {
				if ( fastClass == null ) {
					bulkBean = null;
				}
				else {
					//test out the optimizer:
					Object instance = fastClass.newInstance();
					bulkBean.setPropertyValues( instance, bulkBean.getPropertyValues( instance ) );
				}
			}
		}
		catch( Throwable t ) {
			fastClass = null;
			bulkBean = null;
			String message = "reflection optimizer disabled for: " +
			                 clazz.getName() +
			                 " [" +
			                 StringHelper.unqualify( t.getClass().getName() ) +
			                 ": " +
			                 t.getMessage();

			if (t instanceof BulkBeanException ) {
				int index = ( (BulkBeanException) t ).getIndex();
				if (index >= 0) {
					message += " (property " + setterNames[index] + ")";
				}
			}

			log.debug( message );
		}

		if ( fastClass != null && bulkBean != null ) {
			return new ReflectionOptimizerImpl(
					new InstantiationOptimizerAdapter( fastClass ),
			        new AccessOptimizerAdapter( bulkBean, clazz )
			);
		}
		else {
			return null;
		}
	}

	public org.hibernate.bytecode.ClassTransformer getTransformer(org.hibernate.bytecode.util.ClassFilter classFilter, FieldFilter fieldFilter) {
		return new CglibClassTransformer( classFilter, fieldFilter );
	}

}
