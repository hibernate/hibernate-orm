/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.type.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.type.spi.BasicTypeProducer;
import org.hibernate.boot.model.type.spi.BasicTypeProducerRegistry;
import org.hibernate.boot.model.type.spi.Release;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class BasicTypeProducerRegistryImpl implements BasicTypeProducerRegistry {
	private static final Logger log = Logger.getLogger( BasicTypeProducerRegistryImpl.class );

	private final TypeConfiguration typeConfiguration;
	private Map<String,BasicTypeProducer> basicTypeProducerMap = new HashMap<>();

	public BasicTypeProducerRegistryImpl(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
	}

	@Override
	public BasicTypeProducer resolve(String name) {
		return basicTypeProducerMap.get( name );
	}

	@Override
	public BasicTypeProducerRegistry register(TypeDefinition typeDefinition) {
		return register( typeDefinition, DuplicationStrategy.OVERWRITE );
	}

	@Override
	public BasicTypeProducerRegistry register(TypeDefinition typeDefinition, DuplicationStrategy duplicationStrategy) {
		if ( typeDefinition == null ) {
			throw new IllegalArgumentException( "TypeDefinition to register cannot be null" );
		}
		if ( typeDefinition.getTypeImplementorClass() == null ) {
			throw new IllegalArgumentException( "TypeDefinition to register cannot define null #typeImplementorClass" );
		}

		// register under the BasicType-implementor's class name
		register(
				new BasicTypeProducerTypeDefinitionImpl(
						typeDefinition,
						typeConfiguration
				),
				duplicationStrategy
		);

		// register under the TypeDefinition name, if one
		if ( !StringHelper.isEmpty( typeDefinition.getName() ) ) {
			register(
					new BasicTypeProducerTypeDefinitionImpl(
							typeDefinition.getName(),
							typeDefinition,
							typeConfiguration
					),
					duplicationStrategy
			);
		}

		// register under the TypeDefinition defined registration-keys, if any
		if ( typeDefinition.getRegistrationKeys() != null ) {
			for ( String registrationKey : typeDefinition.getRegistrationKeys() ) {
				register(
						new BasicTypeProducerTypeDefinitionImpl( registrationKey, typeDefinition, typeConfiguration ),
						duplicationStrategy
				);
			}
		}

		return this;
	}

	@Override
	public BasicTypeProducerRegistry register(BasicType basicTypeInstance, String... keys) {
		return register( basicTypeInstance, DuplicationStrategy.OVERWRITE, keys );
	}

	@Override
	public BasicTypeProducerRegistry register(
			BasicType basicTypeInstance,
			DuplicationStrategy duplicationStrategy,
			String... keys) {

		register(
				new BasicTypeProducerInstanceImpl( basicTypeInstance.getClass().getName(), basicTypeInstance ),
				duplicationStrategy
		);

		for ( String key : keys ) {
			register(
					new BasicTypeProducerInstanceImpl( key, basicTypeInstance ),
					duplicationStrategy
			);

		}

		return this;
	}

	public BasicTypeProducerRegistry register(
			BasicTypeProducer producer,
			DuplicationStrategy duplicationStrategy) {
		final String registrationKey = producer.getName();

		if ( duplicationStrategy == DuplicationStrategy.KEEP ) {
			// check first...
			if ( !basicTypeProducerMap.containsKey( registrationKey ) ) {
				basicTypeProducerMap.put( registrationKey, producer );
			}
		}
		else {
			final BasicTypeProducer existing = basicTypeProducerMap.put( registrationKey, producer );

			if ( existing != null && existing != this ) {
				if ( duplicationStrategy == DuplicationStrategy.OVERWRITE ) {
					log.debugf( "Incoming BasicTypeProducer [%s] overwrote existing registration [%s]", producer, existing );
				}
				else {
					throw new IllegalArgumentException(
							String.format(
									Locale.ROOT,
									"Incoming BasicTypeProducer [%s] would have overwritten existing registration [%s], " +
											"but DuplicationStrategy.DISALLOW was requested",
									producer,
									existing
							)
					);
				}
			}
		}

		return this;
	}

	/**
	 * Releases this BasicTypeProducerRegistry and its held resources.
	 * </p>
	 * Applications should never call this.  Not overly happy about exposing this.  But
	 * given the way TypeConfiguration and BasicTypeProducerRegistry get instantiated
	 * currently I do not see an alternative.
	 */
	public void release() {
		// Handle any producers using @Release
		for ( BasicTypeProducer basicTypeProducer : basicTypeProducerMap.values() ) {
			releaseProducer( basicTypeProducer );
		}

		// then clear and the map
		basicTypeProducerMap.clear();
	}

	private void releaseProducer(BasicTypeProducer basicTypeProducer) {
		final Class<? extends BasicTypeProducer> producerClass = basicTypeProducer.getClass();

		for ( Method method : producerClass.getMethods() ) {
			if ( method.getAnnotation( Release.class ) != null ) {
				performRelease( basicTypeProducer, method );
			}
		}
	}

	private void performRelease(BasicTypeProducer producer, Method releaseMethod) {
		// make sure there are no arguments to the Method
		if ( releaseMethod.getParameterCount() > 0 ) {
			log.warnf(
					"Registered BasicTypeProducer [%s] contained method [%s] annotated with @Release, but defining method arguments; skipping invocation",
					producer,
					releaseMethod
			);
			return;
		}

		if ( !releaseMethod.isAccessible() ) {
			try {
				releaseMethod.setAccessible( true );
			}
			catch (Exception e) {
				log.warnf(
						"Unable to call Method#setAccessible for @Release method [%s]; skipping invocation",
						releaseMethod
				);
				return;
			}
		}

		try {
			releaseMethod.invoke( producer );
		}
		catch (InvocationTargetException e) {
			log.warnf( "Unable to invoke BasicTypeProducer @Release method [%s] due to exception during invocation", e.getCause(), releaseMethod );
		}
		catch (Exception e) {
			log.warnf( "Unable to invoke BasicTypeProducer @Release method [%s] due to exception during invocation", e, releaseMethod );
		}
	}
}
