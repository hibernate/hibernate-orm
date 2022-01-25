/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
