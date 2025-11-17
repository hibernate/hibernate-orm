/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.hhh12973;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.id.SequenceMismatchStrategy;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-12973")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSequences.class)
public class SequenceMismatchStrategyExceptionEnumTest extends SequenceMismatchStrategyDefaultExceptionTest {

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( AvailableSettings.SEQUENCE_INCREMENT_SIZE_MISMATCH_STRATEGY, SequenceMismatchStrategy.EXCEPTION );
	}

}
