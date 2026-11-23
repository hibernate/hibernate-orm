package org.hibernate.orm.test.event.collection.association.bidirectional.onetomany;



/**
 *
 * @author Gail Badner
 */
public class ParentWithBidirectionalOneToManySubclass extends ParentWithBidirectionalOneToMany {
	public ParentWithBidirectionalOneToManySubclass() {
	}

	public ParentWithBidirectionalOneToManySubclass(String name) {
		super( name );
	}

}
