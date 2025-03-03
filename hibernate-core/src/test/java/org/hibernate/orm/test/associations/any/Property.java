/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations.any;

//tag::associations-any-property-example[]
public interface Property<T> {

	String getName();

	T getValue();
}
//end::associations-any-property-example[]
