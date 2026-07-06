/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain;

import org.hibernate.boot.pipeline.internal.source.MappingSources;

/**
 * Convenience base class test domain models based on annotated classes
 *
 * @author Steve Ebersole
 */
public abstract class AbstractDomainModelDescriptor implements DomainModelDescriptor {
	private final Class[] annotatedClasses;

	protected AbstractDomainModelDescriptor(Class... annotatedClasses) {
		this.annotatedClasses = annotatedClasses;
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return annotatedClasses;
	}

	@Override
	public void applyDomainModel(MappingSources sources) {
		for ( Class annotatedClass : annotatedClasses ) {
			sources.addManagedClass( annotatedClass );
		}
	}
}
