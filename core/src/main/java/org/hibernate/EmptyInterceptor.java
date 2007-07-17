//$Id: EmptyInterceptor.java 7859 2005-08-11 21:57:33Z oneovthafew $
package org.hibernate;

import java.io.Serializable;
import java.util.Iterator;

import org.hibernate.type.Type;

/**
 * An interceptor that does nothing. May be used as a base class
 * for application-defined custom interceptors.
 * 
 * @author Gavin King
 */
public class EmptyInterceptor implements Interceptor, Serializable {
	
	public static final Interceptor INSTANCE = new EmptyInterceptor();
	
	protected EmptyInterceptor() {}

	public void onDelete(
			Object entity, 
			Serializable id, 
			Object[] state, 
			String[] propertyNames, 
			Type[] types) {}

	public boolean onFlushDirty(
			Object entity, 
			Serializable id, 
			Object[] currentState, 
			Object[] previousState, 
			String[] propertyNames, 
			Type[] types) {
		return false;
	}

	public boolean onLoad(
			Object entity, 
			Serializable id, 
			Object[] state, 
			String[] propertyNames, 
			Type[] types) {
		return false;
	}

	public boolean onSave(
			Object entity, 
			Serializable id, 
			Object[] state, 
			String[] propertyNames, 
			Type[] types) {
		return false;
	}

	public void postFlush(Iterator entities) {}
	public void preFlush(Iterator entities) {}

	public Boolean isTransient(Object entity) {
		return null;
	}

	public Object instantiate(String entityName, EntityMode entityMode, Serializable id) {
		return null;
	}

	public int[] findDirty(Object entity,
			Serializable id,
			Object[] currentState,
			Object[] previousState,
			String[] propertyNames,
			Type[] types) {
		return null;
	}

	public String getEntityName(Object object) {
		return null;
	}

	public Object getEntity(String entityName, Serializable id) {
		return null;
	}

	public void afterTransactionBegin(Transaction tx) {}
	public void afterTransactionCompletion(Transaction tx) {}
	public void beforeTransactionCompletion(Transaction tx) {}

	public String onPrepareStatement(String sql) {
		return sql;
	}

	public void onCollectionRemove(Object collection, Serializable key) throws CallbackException {}

	public void onCollectionRecreate(Object collection, Serializable key) throws CallbackException {}

	public void onCollectionUpdate(Object collection, Serializable key) throws CallbackException {}
	
}