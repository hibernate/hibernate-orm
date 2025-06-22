/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;


/**
 * @author max
 *
 */
public interface ValueVisitor {

	Object accept(Bag bag);

	Object accept(IdentifierBag bag);

	Object accept(List list);

	Object accept(PrimitiveArray primitiveArray);
	Object accept(Array list);

	Object accept(Map map);

	Object accept(OneToMany many);

	Object accept(Set set);

	Object accept(Any any);

	Object accept(SimpleValue value);

	default Object accept(BasicValue value) {
		return null;
	}

	Object accept(DependantValue value);

	Object accept(Component component);

	Object accept(ManyToOne mto);
	Object accept(OneToOne oto);


}
