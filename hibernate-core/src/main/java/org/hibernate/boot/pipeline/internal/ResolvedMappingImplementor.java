/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import org.hibernate.boot.spi.AbstractDelegatingMetadata;

/// Legacy Metadata view over a resolved mapping product.
public class ResolvedMappingImplementor extends AbstractDelegatingMetadata {
	private final ResolvedMapping resolvedMapping;

	public ResolvedMappingImplementor(ResolvedMapping resolvedMapping) {
		super( resolvedMapping.metadata() );
		this.resolvedMapping = resolvedMapping;
	}

	public ResolvedMapping getResolvedMapping() {
		return resolvedMapping;
	}
}
