package org.hibernate.bytecode.cglib;

import org.hibernate.bytecode.ReflectionOptimizer;
import net.sf.cglib.reflect.FastClass;
import org.hibernate.InstantiationException;

import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * The {@link ReflectionOptimizer.InstantiationOptimizer} implementation for CGLIB
 * which simply acts as an adpater to the {@link FastClass} class.
 *
 * @author Steve Ebersole
 */
public class InstantiationOptimizerAdapter implements ReflectionOptimizer.InstantiationOptimizer, Serializable {
	private FastClass fastClass;

	public InstantiationOptimizerAdapter(FastClass fastClass) {
		this.fastClass = fastClass;
	}

	public Object newInstance() {
		try {
			return fastClass.newInstance();
		}
		catch ( Throwable t ) {
			throw new InstantiationException(
					"Could not instantiate entity with CGLIB optimizer: ",
			        fastClass.getJavaClass(),
			        t
			);
		}
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeObject( fastClass.getJavaClass() );
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		Class beanClass = ( Class ) in.readObject();
		fastClass = FastClass.create( beanClass );
	}
}
