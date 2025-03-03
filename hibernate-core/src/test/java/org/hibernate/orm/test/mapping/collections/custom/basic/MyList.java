/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.custom.basic;

import java.util.ArrayList;

/**
 * A custom collection class. We extend a java.util.Collection class, but that is not required.
 * It could be totally non-java-collection type, but then we would need to implement all the PersistentCollection methods.
 *
 * @author max
 */
public class MyList<X> extends ArrayList<X> implements IMyList<X> {
}
