package org.hibernate.test.usercollection.basic;

import org.hibernate.collection.PersistentList;
import org.hibernate.engine.SessionImplementor;

public class PersistentMyList extends PersistentList implements IMyList {

	public PersistentMyList(SessionImplementor session) {
		super(session);
	}

	public PersistentMyList(SessionImplementor session, IMyList list) {
		super(session, list);
	}

	
	
}
