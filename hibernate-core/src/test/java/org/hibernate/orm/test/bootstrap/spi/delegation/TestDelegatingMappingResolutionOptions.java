/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.spi.delegation;

import org.hibernate.boot.pipeline.internal.AbstractDelegatingMappingResolutionOptions;
import org.hibernate.boot.pipeline.internal.MappingResolutionOptions;

/**
 * If this class does not compile anymore due to unimplemented methods, you should probably add the corresponding
 * methods to the parent class.
 *
 * @author Guillaume Smet
 */
public class TestDelegatingMappingResolutionOptions extends AbstractDelegatingMappingResolutionOptions {

	public TestDelegatingMappingResolutionOptions(MappingResolutionOptions delegate) {
		super( delegate );
	}
}
