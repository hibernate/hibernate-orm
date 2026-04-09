/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2017-2025 Red Hat, Inc.
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
package org.hibernate.tool.internal.metadata;

import java.util.Properties;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.tool.api.metadata.MetadataDescriptor;

public class JpaMetadataDescriptor implements MetadataDescriptor {

	private Properties properties = new Properties();
	private Metadata metadata = null;
	
	public JpaMetadataDescriptor(
			final String persistenceUnit, 
			final Properties properties) {
		EntityManagerFactoryBuilderImpl entityManagerFactoryBuilder = 
				createEntityManagerFactoryBuilder(persistenceUnit, properties);
		EntityManagerFactory entityManagerFactory = 
				entityManagerFactoryBuilder.build();
		metadata = entityManagerFactoryBuilder.getMetadata();
		properties.putAll(entityManagerFactory.getProperties());
	}
	
	public Metadata createMetadata() {
		return metadata;
	}
	
	public Properties getProperties() {
		return properties;
	}
	
	private static class PersistenceProvider extends HibernatePersistenceProvider {
		public EntityManagerFactoryBuilderImpl getEntityManagerFactoryBuilder(
				String persistenceUnit, 
				Properties properties) {
			EntityManagerFactoryBuilderImpl result = (EntityManagerFactoryBuilderImpl)getEntityManagerFactoryBuilderOrNull(
					persistenceUnit, 
					properties);
			if (result == null) {
				throw new HibernateException(
						"Persistence unit not found: '" + persistenceUnit + "'."
					);
			}
			return result;
		}
	}

	private static EntityManagerFactoryBuilderImpl createEntityManagerFactoryBuilder(
			final String persistenceUnit, 
			final Properties properties) {
		return new PersistenceProvider().getEntityManagerFactoryBuilder(
				persistenceUnit, 
				properties);
	}
	
}
