/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import jakarta.persistence.PostLoad;
import org.hibernate.HibernateException;
import org.hibernate.Session;

public class Qux {

	boolean created;
	boolean deleted;
	boolean loaded;
	boolean store;  // this should more logically be named "stored" but that's a reserved keyword on MySQL 5.7
	private Long key;
	private String stuff;
	private Set fums;
	private List moreFums;
	private Qux child;
	private Session session;
	private Long childKey;
	private Holder holder;

	private FooProxy foo;

	public Qux() { }

	public Qux(String s) {
		stuff=s;
	}

	@PostLoad
	public void onLoad(Session session, Object id) {
		loaded=true;
		this.session=session;
	}

	public void store() {
	}

	public FooProxy getFoo() {
		return foo;
	}
	public void setFoo(FooProxy foo) {
		this.foo = foo;
	}

	public boolean getCreated() {
		return created;
	}
	private void setCreated(boolean created) {
		this.created = created;
	}

	public boolean getDeleted() {
		return deleted;
	}

	private void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public boolean getLoaded() {
		return loaded;
	}
	private void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}

	public boolean getStore() {
		return store;
	}
	private void setStore(boolean store) {
		this.store = store;
	}

	public Long getKey() {
		return key;
	}

	private void setKey(long key) {
		this.key = new Long(key);
	}

	public void setTheKey(long key) {
		this.key = new Long(key);
	}

	public String getStuff() {
		return stuff;
	}
	public void setStuff(String stuff) {
		this.stuff = stuff;
	}

	public Set getFums() {
		return fums;
	}

	public void setFums(Set fums) {
		this.fums = fums;
	}

	public List getMoreFums() {
		return moreFums;
	}
	public void setMoreFums(List moreFums) {
		this.moreFums = moreFums;
	}

	public Qux getChild() throws HibernateException, SQLException {
		store =true;
		this.childKey = child==null ? null : child.getKey();
		if (childKey!=null && child==null) child = (Qux) session.getReference(Qux.class, childKey);
		return child;
	}

	public void setChild(Qux child) {
		this.child = child;
	}

	private Long getChildKey() {
		return childKey;
	}

	private void setChildKey(Long childKey) {
		this.childKey = childKey;
	}

	protected void finalize() { }

	public Holder getHolder() {
		return holder;
	}

	public void setHolder(Holder holder) {
		this.holder = holder;
	}

}
