package org.hibernate.metamodel.source.annotations.attribute.state.relational;

import org.hibernate.metamodel.relational.state.ManyToOneRelationalState;

/**
 * @author Hardy Ferentschik
 */
public class ManyToOneRelationalStateImpl extends TupleRelationalStateImpl implements ManyToOneRelationalState {
	@Override
	public boolean isLogicalOneToOne() {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public String getForeignKeyName() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
}


