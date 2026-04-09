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
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.cfg.Configuration;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.api.reveng.RevengStrategy;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;

import jakarta.persistence.EntityManagerFactory;

public class JpaConfiguration extends Configuration implements ExtendedConfiguration {

	Metadata metadata = null;
	SessionFactory sessionFactory;
	
	String persistenceUnit;
	
	public JpaConfiguration(
			String persistenceUnit, 
			Map<?, ?> properties) {
		this.persistenceUnit = persistenceUnit;
		if (properties != null) {
			getProperties().putAll(properties);
		}
	}
	
	public Metadata getMetadata() {
		if (metadata == null) {
			initialize();
		}
		return metadata;
	}
	
	@Override
	public SessionFactory buildSessionFactory() {
		if (sessionFactory == null) {
			initialize();
		}
		return sessionFactory;
	}
	
	@Override
	public Configuration setProperties(Properties properties) {
		super.setProperties(properties);
		metadata = null;
		sessionFactory = null;
		return this;
	}
	
	@Override
	public Configuration addProperties(Properties properties) {
		super.addProperties(properties);
		metadata = null;
		sessionFactory = null;
		return this;
	}
	
	public void buildMappings() {
		initialize();
	}
	
	@Override
	public Configuration addFile(File file) {
		throw new RuntimeException(
				"Method 'addFile' should not be called on instances of " +
				this.getClass().getName());
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Configuration addClass(Class pc) {
		throw new RuntimeException(
				"Method 'addClass' should not be called on instances of " +
				this.getClass().getName());
	}
	
	@Override
	public Configuration configure() {
		throw new RuntimeException(
				"Method 'configure' should not be called on instances of " +
				this.getClass().getName());
	}
	
	public Configuration configure(Document document) {
		throw new RuntimeException(
				"Method 'configure' should not be called on instances of " +
				this.getClass().getName());
	}
		
	public Configuration configure(File file) {
		throw new RuntimeException(
				"Method 'configure' should not be called on instances of " +
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
		
	public String getPersistenceUnit() {
		return persistenceUnit;
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
	
	public Iterator<PersistentClass> getClassMappings() {
		return getMetadata().getEntityBindings().iterator();
	}
	
	public PersistentClass getClassMapping(String name) {
		return getMetadata().getEntityBinding(name);
	}
	
	public Iterator<Table> getTableMappings() {
		return getMetadata().collectTableMappings().iterator();
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
		
	public void readFromJDBC() {
		throw new RuntimeException(
				"Method 'readFromJDBC' should not be called on instances of " +
				this.getClass().getName());
	}
	
	void initialize() {
		EntityManagerFactoryBuilderImpl entityManagerFactoryBuilder = 
				HibernateToolsPersistenceProvider
					.createEntityManagerFactoryBuilder(
							persistenceUnit, 
							getProperties());
		EntityManagerFactory entityManagerFactory = 
				entityManagerFactoryBuilder.build();
		sessionFactory = (SessionFactory)entityManagerFactory;
		metadata = entityManagerFactoryBuilder.getMetadata();
		getProperties().putAll(entityManagerFactory.getProperties());
	}

}
