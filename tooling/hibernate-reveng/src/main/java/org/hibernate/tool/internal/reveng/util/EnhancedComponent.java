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
package org.hibernate.tool.internal.reveng.util;

import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;

@SuppressWarnings("serial")
public class EnhancedComponent extends Component implements EnhancedValue {

	private Properties idGenProps = new Properties();
	private String genStrategy = null;

	public EnhancedComponent(MetadataBuildingContext metadata, PersistentClass owner) throws MappingException {
		super(metadata, owner);
	}

	@Override
	public void setIdentifierGeneratorProperties(Properties props) {
		idGenProps = props;
		
	}
	
	@Override
	public Properties getIdentifierGeneratorProperties() {
		return idGenProps;
	}

	@Override
	public void setIdentifierGeneratorStrategy(String s) {
		genStrategy = s;
	}

	@Override
	public String getIdentifierGeneratorStrategy() {
		return genStrategy;
	}

	@Override
	public Class<?> getComponentClass() throws MappingException {
		// we prevent ORM from trying to load a component class by name,
		// since at the point when we are building these, a corresponding class is not yet created
		// (so can't even think about it being compiled and able to load via any classloader) ...
		return Object.class;
	}
}
