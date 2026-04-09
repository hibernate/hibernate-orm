/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2022-2025 Red Hat, Inc.
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

import java.io.File;
import java.util.Collections;
import java.util.Iterator;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.api.metadata.MetadataConstants;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.api.reveng.RevengStrategy;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;

public class RevengConfiguration extends Configuration implements ExtendedConfiguration {

	RevengStrategy revengStrategy;
	Metadata metadata;

	public Object getReverseEngineeringStrategy() {
		return revengStrategy;
	}

	public void setReverseEngineeringStrategy(RevengStrategy strategy) {
		this.revengStrategy = strategy;
	}

	public boolean preferBasicCompositeIds() {
		Boolean preferBasicCompositeIds = (Boolean)getProperties().get(MetadataConstants.PREFER_BASIC_COMPOSITE_IDS);
		return preferBasicCompositeIds == null ? Boolean.TRUE : preferBasicCompositeIds;
	}

	public void setPreferBasicCompositeIds(boolean preferBasicCompositeIds) {
		getProperties().put(
				MetadataConstants.PREFER_BASIC_COMPOSITE_IDS, 
				Boolean.valueOf(preferBasicCompositeIds));
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public void readFromJDBC() {
		metadata = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(revengStrategy, getProperties())
				.createMetadata();
	}
	
	public Iterator<PersistentClass> getClassMappings() {
		if (metadata != null) {
			return metadata.getEntityBindings().iterator();
		} else {
			return Collections.emptyIterator();
		}
	}
	
	public PersistentClass getClassMapping(String name) {
		PersistentClass pc = null;
		if (metadata != null) {
			pc = metadata.getEntityBinding(name);
		}
		return pc;
	}
	
	public Iterator<Table> getTableMappings() {
		if (metadata != null) {
			return metadata.collectTableMappings().iterator();
		} else {
			return Collections.emptyIterator();
		}
	}
	
	public Configuration addFile(File file) {
		throw new RuntimeException(
				"Method 'addFile' should not be called on instances of " +
				this.getClass().getName());
	}
	
	@SuppressWarnings("rawtypes")
	public Configuration addClass(Class file) {
		throw new RuntimeException(
				"Method 'addClass' should not be called on instances of " +
				this.getClass().getName());
	}
	
	public void setEntityResolver(EntityResolver entityResolver) {
		throw new RuntimeException(
				"Method 'setEntityResolver' should not be called on instances of " +
				this.getClass().getName());
	}
	
	public void setNamingStrategy(ImplicitNamingStrategy namingStrategy) {
		throw new RuntimeException(
				"Method 'setNamingStrategy' should not be called on instances of " +
				this.getClass().getName());
	}
		
	public Configuration configure(File object) {
		throw new RuntimeException(
				"Method 'configure' should not be called on instances of " +
				this.getClass().getName());
	}
		
	public Configuration configure(Document document) {
		throw new RuntimeException(
				"Method 'configure' should not be called on instances of " +
				this.getClass().getName());
	}
		
	public Configuration configure() {
		throw new RuntimeException(
				"Method 'configure' should not be called on instances of " +
				this.getClass().getName());
	}
		
	public void buildMappings() {
		if (metadata == null) {
			readFromJDBC();
		}
	}
	
	public SessionFactory buildSessionFactory() {
		throw new RuntimeException(
				"Method 'buildSessionFactory' should not be called on instances of " +
				this.getClass().getName());
	}
		
	public ImplicitNamingStrategy getNamingStrategy() {
		throw new RuntimeException(
				"Method 'getNamingStrategy' should not be called on instances of " +
				this.getClass().getName());
	}
		
	public EntityResolver getEntityResolver() {
		throw new RuntimeException(
				"Method 'getEntityResolver' should not be called on instances of " +
				this.getClass().getName());
	}
		
}
