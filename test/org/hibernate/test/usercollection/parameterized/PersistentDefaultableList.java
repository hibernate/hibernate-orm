package org.hibernate.test.usercollection.parameterized;

import java.util.List;

import org.hibernate.collection.PersistentList;
import org.hibernate.engine.SessionImplementor;

/**
 * The "persistent wrapper" around our specialized collection contract
 *
 * @author Holger Brands
 * @author Steve Ebersole
 */
public class PersistentDefaultableList extends PersistentList implements DefaultableList {
	public PersistentDefaultableList(SessionImplementor session) {
		super( session );
	}

	public PersistentDefaultableList(SessionImplementor session, List list) {
		super( session, list );
	}

	public PersistentDefaultableList() {
	}

	public String getDefaultValue() {
		return ( ( DefaultableList ) this.list ).getDefaultValue();
	}
}
