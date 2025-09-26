/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.spi;

import java.util.List;

public record DatabaseObjectRegistration(String create, String drop, String definition, List<DialectScopeRegistration> dialectScopes ) {
}
