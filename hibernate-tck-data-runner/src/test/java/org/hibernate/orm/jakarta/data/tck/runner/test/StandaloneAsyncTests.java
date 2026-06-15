/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.jakarta.data.tck.runner.test;

import ee.jakarta.tck.data.web.async.Account;
import ee.jakarta.tck.data.web.async.AsyncTests;
import ee.jakarta.tck.data.web.async._Accounts;

import org.hibernate.orm.jakarta.data.tck.runner.DataTck;

@DataTck(
		domainClasses = {Account.class},
		repositoryClasses = {_Accounts.class}
)
public class StandaloneAsyncTests extends AsyncTests {
}
