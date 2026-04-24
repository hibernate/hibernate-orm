/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.hbm;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.models.spi.FieldDetails;

final class FieldMetaUtil {

	private FieldMetaUtil() {
	}

	static Map<String, List<String>> forField(
			Map<String, Map<String, List<String>>> allFieldMeta, FieldDetails field) {
		return allFieldMeta.getOrDefault(field.getName(), Collections.emptyMap());
	}
}
