/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.fetch.depth.form;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

/**
 * @author Marco Belladelli
 */
@ServiceRegistry( settings = @Setting( name = AvailableSettings.MAX_FETCH_DEPTH, value = "2" ) )
public class FormFetchDepth2Test extends AbstractFormFetchDepthTest {
}
