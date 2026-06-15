/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.jakarta.data.tck.runner.test;

import ee.jakarta.tck.data.framework.read.only.Fruit;
import ee.jakarta.tck.data.framework.read.only._FruitRepository;
import ee.jakarta.tck.data.standalone.entity.JakartaQueryTests;

import org.hibernate.orm.jakarta.data.tck.runner.DataTck;

@DataTck(
		domainClasses = {Fruit.class},
		repositoryClasses = {_FruitRepository.class}
)
public class StandaloneJakartaQueryTests extends JakartaQueryTests {
}
