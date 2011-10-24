//$Id: Vetoer.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;
import java.io.Serializable;

import org.hibernate.CallbackException;
import org.hibernate.Session;
import org.hibernate.classic.Lifecycle;

public class Vetoer implements Lifecycle {
	private String id;
	private String name;
	private String[] strings;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String[] getStrings() {
		return strings;
	}

	public void setStrings(String[] strings) {
		this.strings = strings;
	}

	boolean onSaveCalled;
	boolean onUpdateCalled;
	boolean onDeleteCalled;

	public boolean onSave(Session s) throws CallbackException {
		boolean result = !onSaveCalled;
		onSaveCalled = true;
		return result;
	}

	public boolean onUpdate(Session s) throws CallbackException {
		boolean result = !onUpdateCalled;
		onUpdateCalled = true;
		return result;
	}

	public boolean onDelete(Session s) throws CallbackException {
		boolean result = !onDeleteCalled;
		onDeleteCalled = true;
		return result;
	}

	public void onLoad(Session s, Serializable id) {}
}






