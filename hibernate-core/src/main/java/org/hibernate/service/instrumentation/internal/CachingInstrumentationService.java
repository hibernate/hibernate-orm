package org.hibernate.service.instrumentation.internal;

import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.bytecode.instrumentation.internal.FieldInterceptionHelper;
import org.hibernate.service.instrumentation.spi.InstrumentationService;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class CachingInstrumentationService implements InstrumentationService {

	private final ConcurrentHashMap<Class<?>, Boolean> isInstrumentedCache = new ConcurrentHashMap<Class<?>, Boolean>(  );

	@Override
	public boolean isInstrumented(Class<?> entityType) {
		Boolean isInstrumented = isInstrumentedCache.get( entityType );
		if ( isInstrumented == null ) {
			isInstrumented = FieldInterceptionHelper.isInstrumented( entityType );
			isInstrumentedCache.put( entityType, isInstrumented );
		}
		//noinspection ConstantConditions
		return isInstrumented;
	}

	@Override
	public boolean isInstrumented(Object entity) {
		return entity != null && isInstrumented( entity.getClass() );
	}
}
