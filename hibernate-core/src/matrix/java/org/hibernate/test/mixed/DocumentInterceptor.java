//$Id: DocumentInterceptor.java 8670 2005-11-25 17:36:29Z epbernard $
package org.hibernate.test.mixed;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Iterator;

import org.hibernate.CallbackException;
import org.hibernate.EntityMode;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;

/**
 * @author Gavin King
 */
public class DocumentInterceptor implements Interceptor {


	public boolean onLoad(
			Object entity, Serializable id, Object[] state,
			String[] propertyNames, Type[] types
	) throws CallbackException {
		return false;
	}

	public boolean onFlushDirty(
			Object entity, Serializable id,
			Object[] currentState, Object[] previousState,
			String[] propertyNames, Type[] types
	) throws CallbackException {
		if ( entity instanceof Document ) {
			currentState[3] = Calendar.getInstance();
			return true;
		}
		else {
			return false;
		}
	}

	public boolean onSave(
			Object entity, Serializable id, Object[] state,
			String[] propertyNames, Type[] types
	) throws CallbackException {
		if ( entity instanceof Document ) {
			state[4] = state[3] = Calendar.getInstance();
			return true;
		}
		else {
			return false;
		}
	}

	public void onDelete(
			Object entity, Serializable id, Object[] state,
			String[] propertyNames, Type[] types
	) throws CallbackException {

	}

	public void preFlush(Iterator entities) throws CallbackException {

	}

	public void postFlush(Iterator entities) throws CallbackException {

	}

	public Boolean isTransient(Object entity) {
		return null;
	}

	public int[] findDirty(
			Object entity, Serializable id,
			Object[] currentState, Object[] previousState,
			String[] propertyNames, Type[] types
	) {
		return null;
	}

	public Object instantiate(String entityName, EntityMode entityMode, Serializable id) throws CallbackException {
		return null;
	}

	public String getEntityName(Object object) throws CallbackException {
		return null;
	}

	public Object getEntity(String entityName, Serializable id)
			throws CallbackException {
		return null;
	}

	public void afterTransactionBegin(Transaction tx) {
	}

	public void afterTransactionCompletion(Transaction tx) {
	}

	public void beforeTransactionCompletion(Transaction tx) {
	}

	public String onPrepareStatement(String sql) {
		return sql;
	}

	public void onCollectionRecreate(Object collection, Serializable key) throws CallbackException {
	}

	public void onCollectionRemove(Object collection, Serializable key) throws CallbackException {
	}

	public void onCollectionUpdate(Object collection, Serializable key) throws CallbackException {
	}

}
