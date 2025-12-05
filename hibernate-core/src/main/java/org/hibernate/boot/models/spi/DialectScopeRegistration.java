/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.spi;

public record DialectScopeRegistration(String name, String content, String minimumVersion, String maximumVersion) {
}
