//$Id$
package org.hibernate.event;

import java.io.Serializable;

import org.hibernate.HibernateException;

/**
 * @author Gavin King
 */
public interface FlushEntityEventListener extends Serializable {
	public void onFlushEntity(FlushEntityEvent event) throws HibernateException;
}
