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
package org.hibernate.id.enhanced;

import java.io.Serializable;
import java.lang.reflect.Constructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.HibernateException;
import org.hibernate.util.ReflectHelper;
import org.hibernate.id.IdentifierGeneratorFactory;

/**
 * Factory for {@link Optimizer} instances.
 *
 * @author Steve Ebersole
 */
public class OptimizerFactory {
	private static final Logger log = LoggerFactory.getLogger( OptimizerFactory.class );

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

	/**
	 * Common support for optimizer implementations.
	 */
	public static abstract class OptimizerSupport implements Optimizer {
		protected final Class returnClass;
		protected final int incrementSize;

		/**
		 * Construct an optimizer
		 *
		 * @param returnClass The expected id class.
		 * @param incrementSize The increment size
		 */
		protected OptimizerSupport(Class returnClass, int incrementSize) {
			if ( returnClass == null ) {
				throw new HibernateException( "return class is required" );
			}
			this.returnClass = returnClass;
			this.incrementSize = incrementSize;
		}

		/**
		 * Take the primitive long value and "make" (or wrap) it into the
		 * {@link #getReturnClass id type}.
		 *
		 * @param value The primitive value to make/wrap.
		 * @return The wrapped value.
		 */
		protected final Serializable make(long value) {
			return IdentifierGeneratorFactory.createNumber( value, returnClass );
		}

		/**
		 * Getter for property 'returnClass'.  This is the Java
		 * class which is used to represent the id (e.g. {@link java.lang.Long}).
		 *
		 * @return Value for property 'returnClass'.
		 */
		public final Class getReturnClass() {
			return returnClass;
		}

		/**
		 * {@inheritDoc}
		 */
		public final int getIncrementSize() {
			return incrementSize;
		}
	}

	/**
	 * An optimizer that performs no optimization.  The database is hit for
	 * every request.
	 */
	public static class NoopOptimizer extends OptimizerSupport {
		private long lastSourceValue = -1;

		public NoopOptimizer(Class returnClass, int incrementSize) {
			super( returnClass, incrementSize );
		}

		/**
		 * {@inheritDoc}
		 */
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

		/**
		 * {@inheritDoc}
		 */
		public long getLastSourceValue() {
			return lastSourceValue;
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean applyIncrementSizeToSourceValues() {
			return false;
		}
	}

	/**
	 * Optimizer which applies a 'hilo' algorithm in memory to achieve
	 * optimization.
	 */
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

		/**
		 * {@inheritDoc}
		 */
		public synchronized Serializable generate(AccessCallback callback) {
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


		/**
		 * {@inheritDoc}
		 */
		public long getLastSourceValue() {
			return lastSourceValue;
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean applyIncrementSizeToSourceValues() {
			return false;
		}

		/**
		 * Getter for property 'lastValue'.
		 *
		 * @return Value for property 'lastValue'.
		 */
		public long getLastValue() {
			return value - 1;
		}

		/**
		 * Getter for property 'hiValue'.
		 *
		 * @return Value for property 'hiValue'.
		 */
		public long getHiValue() {
			return hiValue;
		}
	}

	/**
	 * Optimizer which uses a pool of values, storing the next low value of the
	 * range in the database.
	 */
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

		/**
		 * {@inheritDoc}
		 */
		public synchronized Serializable generate(AccessCallback callback) {
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

		/**
		 * {@inheritDoc}
		 */
		public long getLastSourceValue() {
			return hiValue;
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean applyIncrementSizeToSourceValues() {
			return true;
		}

		/**
		 * Getter for property 'lastValue'.
		 *
		 * @return Value for property 'lastValue'.
		 */
		public long getLastValue() {
			return value - 1;
		}
	}
}
