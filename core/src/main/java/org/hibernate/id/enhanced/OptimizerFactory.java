package org.hibernate.id.enhanced;

import java.io.Serializable;
import java.lang.reflect.Constructor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.HibernateException;
import org.hibernate.util.ReflectHelper;
import org.hibernate.id.IdentifierGeneratorFactory;

/**
 * Factory for {@link Optimizer} instances.
 *
 * @author Steve Ebersole
 */
public class OptimizerFactory {
	private static final Log log = LogFactory.getLog( OptimizerFactory.class );

	public static final String NONE = "none";
	public static final String HILO = "hilo";
	public static final String POOL = "pooled";

	private static Class[] CTOR_SIG = new Class[] { Class.class, int.class };

	public static Optimizer buildOptimizer(String type, Class returnClass, int incrementSize) {
		String optimizerClassName;
		if ( NONE.equals( type ) ) {
			optimizerClassName = NoopOptimizer.class.getName();
		}
		else if ( HILO.equals( type ) ) {
			optimizerClassName = HiLoOptimizer.class.getName();
		}
		else if ( POOL.equals( type ) ) {
			optimizerClassName = PooledOptimizer.class.getName();
		}
		else {
			optimizerClassName = type;
		}

		try {
			Class optimizerClass = ReflectHelper.classForName( optimizerClassName );
			Constructor ctor = optimizerClass.getConstructor( CTOR_SIG );
			return ( Optimizer ) ctor.newInstance( new Object[] { returnClass, new Integer( incrementSize ) } );
		}
		catch( Throwable ignore ) {
			// intentionally empty
		}

		// the default...
		return new NoopOptimizer( returnClass, incrementSize );
	}

	public static abstract class OptimizerSupport implements Optimizer {
		protected final Class returnClass;
		protected final int incrementSize;

		protected OptimizerSupport(Class returnClass, int incrementSize) {
			if ( returnClass == null ) {
				throw new HibernateException( "return class is required" );
			}
			this.returnClass = returnClass;
			this.incrementSize = incrementSize;
		}

		protected Serializable make(long value) {
			return IdentifierGeneratorFactory.createNumber( value, returnClass );
		}

		public Class getReturnClass() {
			return returnClass;
		}

		public int getIncrementSize() {
			return incrementSize;
		}
	}

	public static class NoopOptimizer extends OptimizerSupport {
		private long lastSourceValue = -1;

		public NoopOptimizer(Class returnClass, int incrementSize) {
			super( returnClass, incrementSize );
		}

		public Serializable generate(AccessCallback callback) {
			if ( lastSourceValue == -1 ) {
				while( lastSourceValue <= 0 ) {
					lastSourceValue = callback.getNextValue();
				}
			}
			else {
				lastSourceValue = callback.getNextValue();
			}
			return make( lastSourceValue );
		}

		public long getLastSourceValue() {
			return lastSourceValue;
		}

		public boolean applyIncrementSizeToSourceValues() {
			return false;
		}
	}

	public static class HiLoOptimizer extends OptimizerSupport {
		private long lastSourceValue = -1;
		private long value;
		private long hiValue;

		public HiLoOptimizer(Class returnClass, int incrementSize) {
			super( returnClass, incrementSize );
			if ( incrementSize < 1 ) {
				throw new HibernateException( "increment size cannot be less than 1" );
			}
			if ( log.isTraceEnabled() ) {
				log.trace( "creating hilo optimizer with [incrementSize=" + incrementSize + "; returnClass="  + returnClass.getName() + "]" );
			}
		}

		public Serializable generate(AccessCallback callback) {
			if ( lastSourceValue < 0 ) {
				lastSourceValue = callback.getNextValue();
				while ( lastSourceValue <= 0 ) {
					lastSourceValue = callback.getNextValue();
				}
				hiValue = ( lastSourceValue * incrementSize ) + 1;
				value = hiValue - incrementSize;
			}
			else if ( value >= hiValue ) {
				lastSourceValue = callback.getNextValue();
				hiValue = ( lastSourceValue * incrementSize ) + 1;
			}
			return make( value++ );
		}


		public long getLastSourceValue() {
			return lastSourceValue;
		}

		public boolean applyIncrementSizeToSourceValues() {
			return false;
		}

		public long getLastValue() {
			return value - 1;
		}

		public long getHiValue() {
			return hiValue;
		}
	}

	public static class PooledOptimizer extends OptimizerSupport {
		private long value;
		private long hiValue = -1;

		public PooledOptimizer(Class returnClass, int incrementSize) {
			super( returnClass, incrementSize );
			if ( incrementSize < 1 ) {
				throw new HibernateException( "increment size cannot be less than 1" );
			}
			if ( log.isTraceEnabled() ) {
				log.trace( "creating pooled optimizer with [incrementSize=" + incrementSize + "; returnClass="  + returnClass.getName() + "]" );
			}
		}

		public Serializable generate(AccessCallback callback) {
			if ( hiValue < 0 ) {
				value = callback.getNextValue();
				if ( value < 1 ) {
					// unfortunately not really safe to normalize this
					// to 1 as an initial value like we do the others
					// because we would not be able to control this if
					// we are using a sequence...
					log.info( "pooled optimizer source reported [" + value + "] as the initial value; use of 1 or greater highly recommended" );
				}
				hiValue = callback.getNextValue();
			}
			else if ( value >= hiValue ) {
				hiValue = callback.getNextValue();
				value = hiValue - incrementSize;
			}
			return make( value++ );
		}

		public long getLastSourceValue() {
			return hiValue;
		}

		public boolean applyIncrementSizeToSourceValues() {
			return true;
		}

		public long getLastValue() {
			return value - 1;
		}
	}
}
