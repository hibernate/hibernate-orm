/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.jakarta.data.tck.runner.test;

import ee.jakarta.tck.data.framework.read.only.Country;
import ee.jakarta.tck.data.framework.read.only._Countries;
import ee.jakarta.tck.data.standalone.entity.FirstTests;

import org.hibernate.orm.jakarta.data.tck.runner.DataTck;

@DataTck(
		domainClasses = {Country.class},
		repositoryClasses = {_Countries.class}
)
public class StandaloneFirstTests extends FirstTests {
}
