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
package org.hibernate.tool.orm.jbt.internal.util;

import java.util.Iterator;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.api.reveng.RevengStrategy;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;

public interface ExtendedConfiguration {
	
	void setEntityResolver(EntityResolver entityResolver);
	
	void setNamingStrategy(ImplicitNamingStrategy namingStrategy);
	
	Configuration configure(Document document);
	
	void buildMappings();
	
	Iterator<PersistentClass> getClassMappings();
	
	void setPreferBasicCompositeIds(boolean b);
	
	void setReverseEngineeringStrategy(RevengStrategy strategy);
	
	void readFromJDBC();
	
	PersistentClass getClassMapping(String string);
	
	ImplicitNamingStrategy getNamingStrategy();
	
	EntityResolver getEntityResolver();
	
	Iterator<Table> getTableMappings();
	
}
