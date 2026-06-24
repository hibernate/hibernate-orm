/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.spi;

/**
 * Default implementation returning the original Panache Next type names.
 */
public class DefaultQuarkusDataTypeNames implements QuarkusDataTypeNames {

	@Override
	public String packageName() {
		return "io.quarkus.hibernate.panache";
	}

	@Override
	public String entityMarker() {
		return "io.quarkus.hibernate.panache.PanacheEntityMarker";
	}

	@Override
	public String managedBlockingRepositoryBase() {
		return "io.quarkus.hibernate.panache.managed.blocking.PanacheManagedBlockingRepositoryBase";
	}

	@Override
	public String statelessBlockingRepositoryBase() {
		return "io.quarkus.hibernate.panache.stateless.blocking.PanacheStatelessBlockingRepositoryBase";
	}

	@Override
	public String managedReactiveRepositoryBase() {
		return "io.quarkus.hibernate.panache.managed.reactive.PanacheManagedReactiveRepositoryBase";
	}

	@Override
	public String statelessReactiveRepositoryBase() {
		return "io.quarkus.hibernate.panache.stateless.reactive.PanacheStatelessReactiveRepositoryBase";
	}
}
