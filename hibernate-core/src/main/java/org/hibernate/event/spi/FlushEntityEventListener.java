package org.hibernate.event.spi;

import org.hibernate.HibernateException;

/**
 * @author Gavin King
 */
public interface FlushEntityEventListener {
	void onFlushEntity(FlushEntityEvent event) throws HibernateException;
}
