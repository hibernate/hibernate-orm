/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.id.enhanced;

import java.io.Serializable;
import java.lang.reflect.Constructor;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;

/**
 * Factory for {@link Optimizer} instances.
 *
 * @author Steve Ebersole
 */
public class OptimizerFactory {
    private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			OptimizerFactory.class.getName()
	);

	public static enum StandardOptimizerDescriptor {
		NONE( "none", NoopOptimizer.class ),
		HILO( "hilo", HiLoOptimizer.class ),
		LEGACY_HILO( "legacy-hilo", LegacyHiLoAlgorithmOptimizer.class ),
		POOLED( "pooled", PooledOptimizer.class, true ),
		POOLED_LO( "pooled-lo", PooledLoOptimizer.class, true );

		private final String externalName;
		private final Class<? extends Optimizer> optimizerClass;
		private final boolean isPooled;

		StandardOptimizerDescriptor(String externalName, Class<? extends Optimizer> optimizerClass) {
			this( externalName, optimizerClass, false );
		}

		StandardOptimizerDescriptor(String externalName, Class<? extends Optimizer> optimizerClass, boolean pooled) {
			this.externalName = externalName;
			this.optimizerClass = optimizerClass;
			this.isPooled = pooled;
		}

		public String getExternalName() {
			return externalName;
		}

		public Class<? extends Optimizer> getOptimizerClass() {
			return optimizerClass;
		}

		public boolean isPooled() {
			return isPooled;
		}

		public static StandardOptimizerDescriptor fromExternalName(String externalName) {
			if ( StringHelper.isEmpty( externalName ) ) {
				LOG.debug( "No optimizer specified, using NONE as default" );
				return NONE;
			}
			else if ( NONE.externalName.equals( externalName ) ) {
				return NONE;
			}
			else if ( HILO.externalName.equals( externalName ) ) {
				return HILO;
			}
			else if ( LEGACY_HILO.externalName.equals( externalName ) ) {
				return LEGACY_HILO;
			}
			else if ( POOLED.externalName.equals( externalName ) ) {
				return POOLED;
			}
			else if ( POOLED_LO.externalName.equals( externalName ) ) {
				return POOLED_LO;
			}
			else {
				LOG.debugf( "Unknown optimizer key [%s]; returning null assuming Optimizer impl class name", externalName );
				return null;
			}
		}
	}

	/**
	 * Marker interface for optimizer which wish to know the user-specified initial value.
	 * <p/>
	 * Used instead of constructor injection since that is already a public understanding and
	 * because not all optimizers care.
	 */
	public static interface InitialValueAwareOptimizer {
		/**
		 * Reports the user specified initial value to the optimizer.
		 * <p/>
		 * <tt>-1</tt> is used to indicate that the user did not specify.
		 *
		 * @param initialValue The initial value specified by the user, or <tt>-1</tt> to indicate that the
		 * user did not specify.
		 */
		public void injectInitialValue(long initialValue);
	}

	public static boolean isPooledOptimizer(String type) {
		final StandardOptimizerDescriptor standardDescriptor = StandardOptimizerDescriptor.fromExternalName( type );
		return standardDescriptor != null && standardDescriptor.isPooled();
	}

	private static Class[] CTOR_SIG = new Class[] { Class.class, int.class };

	/**
	 * Builds an optimizer
	 *
	 * @param type The optimizer type, either a short-hand name or the {@link Optimizer} class name.
	 * @param returnClass The generated value java type
	 * @param incrementSize The increment size.
	 *
	 * @return The built optimizer
	 *
	 * @deprecated Use {@link #buildOptimizer(String, Class, int, long)} instead
	 */
	@Deprecated
	@SuppressWarnings( {"UnnecessaryBoxing", "unchecked"})
	public static Optimizer buildOptimizer(String type, Class returnClass, int incrementSize) {
		final Class<? extends Optimizer> optimizerClass;

		final StandardOptimizerDescriptor standardDescriptor = StandardOptimizerDescriptor.fromExternalName( type );
		if ( standardDescriptor != null ) {
			optimizerClass = standardDescriptor.getOptimizerClass();
		}
		else {
			try {
				optimizerClass = ReflectHelper.classForName( type );
			}
			catch( Throwable ignore ) {
				LOG.unableToLocateCustomOptimizerClass( type );
				return buildFallbackOptimizer( returnClass, incrementSize );
			}
		}

		try {
			Constructor ctor = optimizerClass.getConstructor( CTOR_SIG );
			return ( Optimizer ) ctor.newInstance( returnClass, Integer.valueOf( incrementSize ) );
		}
		catch( Throwable ignore ) {
            LOG.unableToInstantiateOptimizer( type );
		}

		return buildFallbackOptimizer( returnClass, incrementSize );
	}

	private static Optimizer buildFallbackOptimizer(Class returnClass, int incrementSize) {
		return new NoopOptimizer( returnClass, incrementSize );
	}

	/**
	 * Builds an optimizer
	 *
	 * @param type The optimizer type, either a short-hand name or the {@link Optimizer} class name.
	 * @param returnClass The generated value java type
	 * @param incrementSize The increment size.
	 * @param explicitInitialValue The user supplied initial-value (-1 indicates the user did not specify).
	 *
	 * @return The built optimizer
	 */
	@SuppressWarnings({ "UnnecessaryBoxing", "deprecation" })
	public static Optimizer buildOptimizer(String type, Class returnClass, int incrementSize, long explicitInitialValue) {
		final Optimizer optimizer = buildOptimizer( type, returnClass, incrementSize );
		if ( InitialValueAwareOptimizer.class.isInstance( optimizer ) ) {
			( (InitialValueAwareOptimizer) optimizer ).injectInitialValue( explicitInitialValue );
		}
		return optimizer;
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
		 * Getter for property 'returnClass'.  This is the Java
		 * class which is used to represent the id (e.g. {@link java.lang.Long}).
		 *
		 * @return Value for property 'returnClass'.
		 */
		@SuppressWarnings( {"UnusedDeclaration"})
		public final Class getReturnClass() {
			return returnClass;
		}

		@Override
		public final int getIncrementSize() {
			return incrementSize;
		}
	}

	/**
	 * An optimizer that performs no optimization.  The database is hit for
	 * every request.
	 */
	public static class NoopOptimizer extends OptimizerSupport {
		private IntegralDataTypeHolder lastSourceValue;

		public NoopOptimizer(Class returnClass, int incrementSize) {
			super( returnClass, incrementSize );
		}

		@Override
		public Serializable generate(AccessCallback callback) {
			// IMPL NOTE : it is incredibly important that the method-local variable be used here to
			//		avoid concurrency issues.
			IntegralDataTypeHolder value = null;
			while ( value == null || value.lt( 1 ) ) {
				value = callback.getNextValue();
			}
			lastSourceValue = value;
			return value.makeValue();
		}

		@Override
		public IntegralDataTypeHolder getLastSourceValue() {
			return lastSourceValue;
		}

		@Override
		public boolean applyIncrementSizeToSourceValues() {
			return false;
		}
	}

	/**
	 * Optimizer which applies a 'hilo' algorithm in memory to achieve
	 * optimization.
	 * <p/>
	 * A 'hilo' algorithm is simply a means for a single value stored in the
	 * database to represent a "bucket" of possible, contiguous values.  The
	 * database value identifies which particular bucket we are on.
	 * <p/>
	 * This database value must be paired with another value that defines the
	 * size of the bucket; the number of possible values available.
	 * The {@link #getIncrementSize() incrementSize} serves this purpose.  The
	 * naming here is meant more for consistency in that this value serves the
	 * same purpose as the increment supplied to the {@link PooledOptimizer}.
	 * <p/>
	 * The general algorithms used to determine the bucket are:<ol>
	 * <li>{@code upperLimit = (databaseValue * incrementSize) + 1}</li>
	 * <li>{@code lowerLimit = upperLimit - incrementSize}</li>
	 * </ol>
	 * As an example, consider a case with incrementSize of 20.  Initially the
	 * database holds 1:<ol>
	 * <li>{@code upperLimit = (1 * 20) + 1 = 21}</li>
	 * <li>{@code lowerLimit = 21 - 20 = 1}</li>
	 * </ol>
	 * From there we increment the value from lowerLimit until we reach the
	 * upperLimit, at which point we would define a new bucket.  The database
	 * now contains 2, though incrementSize remains unchanged:<ol>
	 * <li>{@code upperLimit = (2 * 20) + 1 = 41}</li>
	 * <li>{@code lowerLimit = 41 - 20 = 21}</li>
	 * </ol>
	 * And so on...
	 * <p/>
	 * Note, 'value' always (after init) holds the next value to return
	 */
	public static class HiLoOptimizer extends OptimizerSupport {
		private IntegralDataTypeHolder lastSourceValue;
		private IntegralDataTypeHolder upperLimit;
		private IntegralDataTypeHolder value;

		public HiLoOptimizer(Class returnClass, int incrementSize) {
			super( returnClass, incrementSize );
			if ( incrementSize < 1 )
				throw new HibernateException( "increment size cannot be less than 1" );
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev( "Creating hilo optimizer with [incrementSize={0}; returnClass={1}]", incrementSize, returnClass.getName() );
			}
		}

		@Override
		public synchronized Serializable generate(AccessCallback callback) {
			if ( lastSourceValue == null ) {
				// first call, so initialize ourselves.  we need to read the database
				// value and set up the 'bucket' boundaries
				lastSourceValue = callback.getNextValue();
				while ( lastSourceValue.lt( 1 ) ) {
					lastSourceValue = callback.getNextValue();
				}
				// upperLimit defines the upper end of the bucket values
				upperLimit = lastSourceValue.copy().multiplyBy( incrementSize ).increment();
				// initialize value to the low end of the bucket
				value = upperLimit.copy().subtract( incrementSize );
			}
			else if ( ! upperLimit.gt( value ) ) {
				lastSourceValue = callback.getNextValue();
				upperLimit = lastSourceValue.copy().multiplyBy( incrementSize ).increment();
			}
			return value.makeValueThenIncrement();
		}

		@Override
		public IntegralDataTypeHolder getLastSourceValue() {
			return lastSourceValue;
		}

		@Override
		public boolean applyIncrementSizeToSourceValues() {
			return false;
		}

		/**
		 * Getter for property 'lastValue'.
		 * <p/>
		 * Exposure intended for testing purposes.
		 *
		 * @return Value for property 'lastValue'.
		 */
		public IntegralDataTypeHolder getLastValue() {
			return value.copy().decrement();
		}

		/**
		 * Getter for property 'upperLimit'.
		 * <p/>
		 * Exposure intended for testing purposes.
		 *
		 * @return Value for property 'upperLimit'.
		 */
		public IntegralDataTypeHolder getHiValue() {
			return upperLimit;
		}
	}

	public static class LegacyHiLoAlgorithmOptimizer extends OptimizerSupport {
		private long maxLo;
		private long lo;
		private IntegralDataTypeHolder hi;

		private IntegralDataTypeHolder lastSourceValue;
		private IntegralDataTypeHolder value;


		public LegacyHiLoAlgorithmOptimizer(Class returnClass, int incrementSize) {
			super( returnClass, incrementSize );
			if ( incrementSize < 1 )
				throw new HibernateException( "increment size cannot be less than 1" );
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev( "Creating hilo optimizer (legacy) with [incrementSize={0}; returnClass={1}]", incrementSize, returnClass.getName() );
			}
			maxLo = incrementSize;
			lo = maxLo+1;
		}

		@Override
		public synchronized Serializable generate(AccessCallback callback) {
			if ( lo > maxLo ) {
				lastSourceValue = callback.getNextValue();
				lo = lastSourceValue.eq( 0 ) ? 1 : 0;
				hi = lastSourceValue.copy().multiplyBy( maxLo+1 );
			}
			value = hi.copy().add( lo++ );
			return value.makeValue();
		}

		@Override
		public IntegralDataTypeHolder getLastSourceValue() {
			return lastSourceValue.copy();
		}

		@Override
		public boolean applyIncrementSizeToSourceValues() {
			return false;
		}

		/**
		 * Getter for property 'lastValue'.
		 * <p/>
		 * Exposure intended for testing purposes.
		 *
		 * @return Value for property 'lastValue'.
		 */
		@SuppressWarnings( {"UnusedDeclaration"})
		public IntegralDataTypeHolder getLastValue() {
			return value;
		}
	}

	/**
	 * Optimizer which uses a pool of values, storing the next low value of the
	 * range in the database.
	 * <p/>
	 * Note that this optimizer works essentially the same as the
	 * {@link HiLoOptimizer} except that here the bucket ranges are actually
	 * encoded into the database structures.
	 * <p/>
	 * Note if you prefer that the database value be interpreted as the bottom end of our current range,
	 * then use the {@link PooledLoOptimizer} strategy
	 */
	public static class PooledOptimizer extends OptimizerSupport implements InitialValueAwareOptimizer {
		private IntegralDataTypeHolder hiValue;
		private IntegralDataTypeHolder value;
		private long initialValue = -1;

		public PooledOptimizer(Class returnClass, int incrementSize) {
			super( returnClass, incrementSize );
			if ( incrementSize < 1 ) {
				throw new HibernateException( "increment size cannot be less than 1" );
			}
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev( "Creating pooled optimizer with [incrementSize={0}; returnClass={1}]", incrementSize, returnClass.getName() );
			}
		}

		@Override
		public synchronized Serializable generate(AccessCallback callback) {
			if ( hiValue == null ) {
				value = callback.getNextValue();
                // unfortunately not really safe to normalize this
                // to 1 as an initial value like we do the others
                // because we would not be able to control this if
                // we are using a sequence...
                if (value.lt(1)) LOG.pooledOptimizerReportedInitialValue(value);
                // the call to obtain next-value just gave us the initialValue
				if ( ( initialValue == -1 && value.lt( incrementSize ) ) || value.eq( initialValue ) ) {
					hiValue = callback.getNextValue();
				}
				else {
					hiValue = value;
					value = hiValue.copy().subtract( incrementSize );
				}
			}
			else if ( ! hiValue.gt( value ) ) {
				hiValue = callback.getNextValue();
				value = hiValue.copy().subtract( incrementSize );
			}
			return value.makeValueThenIncrement();
		}

		@Override
		public IntegralDataTypeHolder getLastSourceValue() {
			return hiValue;
		}

		@Override
		public boolean applyIncrementSizeToSourceValues() {
			return true;
		}

		/**
		 * Getter for property 'lastValue'.
		 * <p/>
		 * Exposure intended for testing purposes.
		 *
		 * @return Value for property 'lastValue'.
		 */
		public IntegralDataTypeHolder getLastValue() {
			return value.copy().decrement();
		}

		@Override
		public void injectInitialValue(long initialValue) {
			this.initialValue = initialValue;
		}
	}

	public static class PooledLoOptimizer extends OptimizerSupport {
		private IntegralDataTypeHolder lastSourceValue; // last value read from db source
		private IntegralDataTypeHolder value; // the current generator value

		public PooledLoOptimizer(Class returnClass, int incrementSize) {
			super( returnClass, incrementSize );
			if ( incrementSize < 1 ) {
				throw new HibernateException( "increment size cannot be less than 1" );
			}
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev( "Creating pooled optimizer (lo) with [incrementSize={0}; returnClass=]", incrementSize, returnClass.getName() );
			}
		}

		@Override
		public synchronized Serializable generate(AccessCallback callback) {
			if ( lastSourceValue == null || ! value.lt( lastSourceValue.copy().add( incrementSize ) ) ) {
				lastSourceValue = callback.getNextValue();
				value = lastSourceValue.copy();
				// handle cases where initial-value is less that one (hsqldb for instance).
				while ( value.lt( 1 ) ) {
					value.increment();
				}
			}
			return value.makeValueThenIncrement();
		}

		@Override
		public IntegralDataTypeHolder getLastSourceValue() {
			return lastSourceValue;
		}

		@Override
		public boolean applyIncrementSizeToSourceValues() {
			return true;
		}
	}

	/**
	 * @deprecated Use {@link StandardOptimizerDescriptor#getExternalName()} via {@link StandardOptimizerDescriptor#NONE}
	 */
	@Deprecated
	@SuppressWarnings( {"UnusedDeclaration"})
	public static final String NONE = StandardOptimizerDescriptor.NONE.getExternalName();

	/**
	 * @deprecated Use {@link StandardOptimizerDescriptor#getExternalName()} via {@link StandardOptimizerDescriptor#HILO}
	 */
	@Deprecated
	@SuppressWarnings( {"UnusedDeclaration"})
	public static final String HILO = StandardOptimizerDescriptor.HILO.getExternalName();

	/**
	 * @deprecated Use {@link StandardOptimizerDescriptor#getExternalName()} via {@link StandardOptimizerDescriptor#LEGACY_HILO}
	 */
	@Deprecated
	@SuppressWarnings( {"UnusedDeclaration"})
	public static final String LEGACY_HILO = "legacy-hilo";

	/**
	 * @deprecated Use {@link StandardOptimizerDescriptor#getExternalName()} via {@link StandardOptimizerDescriptor#POOLED}
	 */
	@Deprecated
	@SuppressWarnings( {"UnusedDeclaration"})
	public static final String POOL = "pooled";

	/**
	 * @deprecated Use {@link StandardOptimizerDescriptor#getExternalName()} via {@link StandardOptimizerDescriptor#POOLED_LO}
	 */
	@Deprecated
	@SuppressWarnings( {"UnusedDeclaration"})
	public static final String POOL_LO = "pooled-lo";
}
