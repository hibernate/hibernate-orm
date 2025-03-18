/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.basic;

import java.util.Map;

import org.hibernate.cfg.Environment;

import org.hibernate.testing.orm.junit.JiraKey;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-8497")
public class ClassicQueryTranslatorFactoryTest extends Simple {
	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( Environment.QUERY_TRANSLATOR, ClassicQueryTranslatorFactory.class.getName() );
	}
}
