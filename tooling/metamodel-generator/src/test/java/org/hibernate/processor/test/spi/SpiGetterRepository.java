/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.spi;

/**
 * A repository for which the extension declares a session getter expression instead of an injected session.
 */
@ExtensionMarker
public interface SpiGetterRepository {
}
