//$Id: FlushEntityEventListener.java 7785 2005-08-08 23:24:44Z oneovthafew $
package org.hibernate.event;

import java.io.Serializable;

import org.hibernate.HibernateException;

/**
 * @author Gavin King
 */
public interface FlushEntityEventListener extends Serializable {
	public void onFlushEntity(FlushEntityEvent event) throws HibernateException;
}
