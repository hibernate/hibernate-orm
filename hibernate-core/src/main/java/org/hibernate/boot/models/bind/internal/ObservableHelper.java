/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.bind.internal;

import java.util.Iterator;
import java.util.List;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.ModelsException;

import static org.hibernate.boot.models.bind.ModelBindingLogging.MODEL_BINDING_LOGGER;

/**
 * @author Steve Ebersole
 */
public class ObservableHelper {
	public static <T> void processCallbacks(
			T resolvedInstance,
			List<? extends ResolutionCallback<T>> callbacks) {
		processCallbacks( resolvedInstance, callbacks, false );
	}

	public static <T> void processCallbacks(
			T resolvedInstance,
			List<? extends ResolutionCallback<T>> callbacks,
			boolean allowUnresolved) {
		if ( CollectionHelper.isEmpty( callbacks ) ) {
			return;
		}

		int processedCount = 0;
		final Iterator<? extends ResolutionCallback<T>> secondPassItr = callbacks.iterator();
		while ( secondPassItr.hasNext() ) {
			final ResolutionCallback<T> callback = secondPassItr.next();
			try {
				final boolean success = callback.handleResolution( resolvedInstance );
				if ( success ) {
					processedCount++;
					secondPassItr.remove();
				}
			}
			catch (Exception e) {
				// todo : handle cases where the caught exception is a non-transient, invariant condition
				//		indicating to immediately stop the processing and throw an error (either the original
				//		or a new one.
				MODEL_BINDING_LOGGER.debug( "Error processing second pass", e );
			}
		}

		if ( !callbacks.isEmpty() ) {
			if ( processedCount == 0 ) {
				// there are callbacks in the queue, but we were not able to
				// successfully process any of them.  this is a non-changing
				// error condition - throw an exception, unless `allowUnresolved` == true
				if ( !allowUnresolved ) {
					throw new ModelsException( "Unable to process second-pass list" );
				}
			}

			processCallbacks( resolvedInstance, callbacks );
		}
	}
}
