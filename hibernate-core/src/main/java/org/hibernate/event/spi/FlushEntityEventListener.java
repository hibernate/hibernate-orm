package org.hibernate.event.spi;

import org.hibernate.HibernateException;
import jakarta.annotation.Nonnull;

/**
 * @author Gavin King
 */
public interface FlushEntityEventListener {
	void onFlushEntity(@Nonnull FlushEntityEvent event) throws HibernateException;
}
