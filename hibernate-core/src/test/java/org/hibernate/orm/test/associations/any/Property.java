/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations.any;

//tag::associations-any-property-example[]
public interface Property<T> {

	String getName();

	T getValue();
}
//end::associations-any-property-example[]
