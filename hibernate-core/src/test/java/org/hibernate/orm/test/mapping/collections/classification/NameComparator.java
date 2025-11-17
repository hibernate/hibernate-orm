/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.classification;

import java.util.Comparator;

/**
 * @author Steve Ebersole
 */
//tag::collections-name-comparator-ex[]
public class NameComparator implements Comparator<Name> {
	static final Comparator<Name> comparator = Comparator.comparing( Name::getLast ).thenComparing( Name::getFirst );

	@Override
	public int compare(Name o1, Name o2) {
		return comparator.compare( o1, o2 );
	}
}
//end::collections-name-comparator-ex[]
