/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.spi;

/**
 * SPI for providing the type names used by the Quarkus Data / Panache framework.
 * <p>
 * Implementations are discovered via {@link java.util.ServiceLoader}. When no
 * implementation is found, {@link DefaultQuarkusDataTypeNames} is used, which
 * returns the original Panache Next type names.
 */
public interface QuarkusDataTypeNames {

	String packageName();

	String entityMarker();

	String managedBlockingRepositoryBase();

	String statelessBlockingRepositoryBase();

	String managedReactiveRepositoryBase();

	String statelessReactiveRepositoryBase();
}
