/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

import java.util.Map;
import java.util.Properties;

public class PropertiesHelper {

	/**
	 * Pretend that a {@link Properties} object is really a
	 * {@link Map Map&lt;String,Object&gt;}, which of course it
	 * should be anyway.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static Map<String, Object> map(Properties properties) {
		//yup, I'm really doing this, and yep, I know it's rubbish:
		return (Map) properties;
	}
}
