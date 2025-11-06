/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.util;

import org.hibernate.mapping.KeyValue;

import java.util.Properties;

public interface EnhancedValue extends KeyValue {

	void setIdentifierGeneratorProperties(Properties suggestedProperties);

	Properties getIdentifierGeneratorProperties();

	void setIdentifierGeneratorStrategy(String s);

	String getIdentifierGeneratorStrategy();

}
