package org.hibernate.test.collection.custom.basic;
import org.hibernate.collection.internal.PersistentList;
import org.hibernate.engine.spi.SessionImplementor;

public class PersistentMyList extends PersistentList implements IMyList {

	public PersistentMyList(SessionImplementor session) {
		super(session);
	}

	public PersistentMyList(SessionImplementor session, IMyList list) {
		super(session, list);
	}

	
	
}
