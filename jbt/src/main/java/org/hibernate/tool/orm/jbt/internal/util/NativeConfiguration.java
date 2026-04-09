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
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.api.reveng.RevengStrategy;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;

public class NativeConfiguration extends Configuration implements ExtendedConfiguration {
	
	private EntityResolver entityResolver = null;
	
	private ImplicitNamingStrategy namingStrategy = null;
	
	private Metadata metadata = null;
	
	public void setEntityResolver(EntityResolver entityResolver) {
		// This method is not supported anymore in class Configuration from Hibernate 5+
		// Only caching the EntityResolver for bookkeeping purposes
		this.entityResolver = entityResolver;
	}
	
	public EntityResolver getEntityResolver() {
		// This method is not supported anymore in class Configuration from Hibernate 5+
		// Returning the cached EntityResolver for bookkeeping purposes
		return entityResolver;
	}
	
	public void setNamingStrategy(ImplicitNamingStrategy namingStrategy) {
		// The method Configuration.setNamingStrategy() is not supported 
		// anymore from Hibernate 5+.
		// Naming strategies can be configured using the 
		// AvailableSettings.IMPLICIT_NAMING_STRATEGY property.
		// Only caching the NamingStrategy for bookkeeping purposes
		this.namingStrategy = namingStrategy;
	}
	
	public ImplicitNamingStrategy getNamingStrategy() {
		// This method is not supported anymore from Hibernate 5+
		// Returning the cached NamingStrategy for bookkeeping purposes
		return namingStrategy;
	}
	
	public Configuration configure(Document document) {
		File tempFile = null;
		Configuration result = null;
		metadata = null;
		try {
			tempFile = File.createTempFile(document.toString(), "cfg.xml");
			DOMSource domSource = new DOMSource(document);
			StringWriter stringWriter = new StringWriter();
			StreamResult stream = new StreamResult(stringWriter);
		    TransformerFactory tf = TransformerFactory.newInstance();
		    Transformer transformer = tf.newTransformer();
		    transformer.transform(domSource, stream);
		    FileWriter fileWriter = new FileWriter(tempFile);
		    fileWriter.write(stringWriter.toString());
		    fileWriter.close();
			result = configure(tempFile);
		} catch(IOException | TransformerException e) {
			throw new RuntimeException("Problem while configuring", e);
		} finally {
			tempFile.delete();
		}
		return result;
	}
	
	public void buildMappings() {
		buildMetadata();
	}
	
	public Iterator<PersistentClass> getClassMappings() {
		return getMetadata().getEntityBindings().iterator();
	}
	
	public PersistentClass getClassMapping(String name) {
		return getMetadata().getEntityBinding(name);
	}
	
	public Iterator<Table> getTableMappings() {
		return getMetadata().collectTableMappings().iterator();
	}
	
	public void setPreferBasicCompositeIds(boolean b) {
		throw new RuntimeException(
				"Method 'setPreferBasicCompositeIds' should not be called on instances of " +
				this.getClass().getName());
	}
		
	public void setReverseEngineeringStrategy(RevengStrategy strategy) {
		throw new RuntimeException(
				"Method 'setReverseEngineeringStrategy' should not be called on instances of " +
				this.getClass().getName());
	}
	
	public void readFromJDBC() {
		throw new RuntimeException(
				"Method 'readFromJDBC' should not be called on instances of " +
				this.getClass().getName());
	}
	
	public Metadata getMetadata() {
		if (metadata == null) {
			buildMetadata();
		}
		return metadata;
	}
	
	private void buildMetadata() {
		MetadataSources metadataSources = MetadataHelper.getMetadataSources(this);
		getStandardServiceRegistryBuilder().applySettings(getProperties());
		metadata = metadataSources.buildMetadata(getStandardServiceRegistryBuilder().build());
	}
	
}
