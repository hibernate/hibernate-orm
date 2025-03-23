/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.xml.common;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Steve Ebersole
 */
public class CallbackTarget {
	private List<String> prePersistCallbacks = new ArrayList<>();
	private List<String> postPersistCallbacks = new ArrayList<>();

	private List<String> preUpdateCallbacks = new ArrayList<>();
	private List<String> postUpdateCallbacks = new ArrayList<>();

	private List<String> preRemoveCallbacks = new ArrayList<>();
	private List<String> postRemoveCallbacks = new ArrayList<>();

	private List<String> postLoadCallbacks = new ArrayList<>();

	public void prePersistCalled(String listenerName) {
		prePersistCallbacks.add( listenerName );
	}

	public void postPersistCalled(String listenerName) {
		postPersistCallbacks.add( listenerName );
	}

	public void preUpdateCalled(String listenerName) {
		preUpdateCallbacks.add( listenerName );
	}

	public void postUpdateCalled(String listenerName) {
		postUpdateCallbacks.add( listenerName );
	}

	public void preRemoveCalled(String listenerName) {
		preRemoveCallbacks.add( listenerName );
	}

	public void postRemoveCalled(String listenerName) {
		postRemoveCallbacks.add( listenerName );
	}

	public void postLoadCalled(String listenerName) {
		postLoadCallbacks.add( listenerName );
	}


	public List<String> getPrePersistCallbacks() {
		return prePersistCallbacks;
	}

	public List<String> getPostPersistCallbacks() {
		return postPersistCallbacks;
	}

	public List<String> getPreUpdateCallbacks() {
		return preUpdateCallbacks;
	}

	public List<String> getPostUpdateCallbacks() {
		return postUpdateCallbacks;
	}

	public List<String> getPreRemoveCallbacks() {
		return preRemoveCallbacks;
	}

	public List<String> getPostRemoveCallbacks() {
		return postRemoveCallbacks;
	}

	public List<String> getPostLoadCallbacks() {
		return postLoadCallbacks;
	}
}
