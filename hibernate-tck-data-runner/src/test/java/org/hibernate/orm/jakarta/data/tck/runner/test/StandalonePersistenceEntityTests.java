/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.jakarta.data.tck.runner.test;

import ee.jakarta.tck.data.standalone.persistence.Product;
import ee.jakarta.tck.data.standalone.persistence.stateless.PersistenceEntityTests;
import ee.jakarta.tck.data.standalone.persistence.stateless._Catalog;

import org.hibernate.orm.jakarta.data.tck.runner.DataTck;

@DataTck(
		domainClasses = {Product.class},
		repositoryClasses = {_Catalog.class}
)
public class StandalonePersistenceEntityTests extends PersistenceEntityTests {
}
