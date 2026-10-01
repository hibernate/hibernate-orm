/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.spi;

/** Not a repository as far as the processor is concerned, unless the extension says so. */
@ExtensionMarker
public interface SpiRepository {
}
