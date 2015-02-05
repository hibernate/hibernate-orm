package org.hibernate.test.collection.custom.declaredtype;
import org.hibernate.collection.internal.PersistentList;
import org.hibernate.engine.spi.SessionImplementor;

public class PersistentHeadList extends PersistentList implements IHeadList {

	public PersistentHeadList(SessionImplementor session) {
		super(session);
	}

	public PersistentHeadList(SessionImplementor session, IHeadList list) {
		super(session, list);
	}


    @Override
    public Object head() {
        return ((IHeadList) list).head();
    }
}
