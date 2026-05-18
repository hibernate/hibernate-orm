/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import java.util.List;

record ResultSelection(String resultTypeName, List<String> paths, boolean recordProjection) {
}
