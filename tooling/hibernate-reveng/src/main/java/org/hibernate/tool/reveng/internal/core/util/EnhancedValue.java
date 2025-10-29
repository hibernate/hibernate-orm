/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2024-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
