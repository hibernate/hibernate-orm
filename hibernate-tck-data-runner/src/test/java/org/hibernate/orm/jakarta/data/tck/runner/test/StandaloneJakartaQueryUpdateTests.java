/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.jakarta.data.tck.runner.test;

import ee.jakarta.tck.data.standalone.entity.JakartaQueryUpdateTests;
import ee.jakarta.tck.data.standalone.entity.Vegetable;
import ee.jakarta.tck.data.standalone.entity._VegetableRepository;

import org.hibernate.orm.jakarta.data.tck.runner.DataTck;

@DataTck(
		domainClasses = {Vegetable.class},
		repositoryClasses = {_VegetableRepository.class}
)
public class StandaloneJakartaQueryUpdateTests extends JakartaQueryUpdateTests {
}
