package org.hibernate.event.spi;

import org.hibernate.HibernateException;
import jakarta.annotation.Nonnull;

/**
 * Defines the contract for handling of session flush events.
 *
 * @author Steve Ebersole
 */
public interface FlushEventListener {
	/**
	 * Handle the given flush event.
	 *
	 * @param event The flush event to be handled.
	 */
	void onFlush(@Nonnull FlushEvent event) throws HibernateException;
}
