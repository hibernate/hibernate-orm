/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.jakarta.data.tck.runner.test;

import ee.jakarta.tck.data.standalone.persistence.Product;
import ee.jakarta.tck.data.standalone.persistence.stateful.StatefulPersistenceEntityTests;
import ee.jakarta.tck.data.standalone.persistence.stateful._Inventory;
import ee.jakarta.tck.data.standalone.persistence.stateful._Products;

import org.hibernate.orm.jakarta.data.tck.runner.DataTck;
import org.hibernate.orm.jakarta.data.tck.runner.util.TransactionalInterceptor;
import org.hibernate.orm.jakarta.data.tck.runner.util.UserTransactionProducer;

@DataTck(
		domainClasses = {Product.class},
		repositoryClasses = {_Inventory.class, _Products.class,
				UserTransactionProducer.class, TransactionalInterceptor.class},
		sharedEntityManager = true
)
public class StandaloneStatefulPersistenceEntityTests extends StatefulPersistenceEntityTests {
}
