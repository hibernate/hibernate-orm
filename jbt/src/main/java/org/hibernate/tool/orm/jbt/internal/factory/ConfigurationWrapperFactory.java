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
package org.hibernate.tool.orm.jbt.internal.factory;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.api.reveng.RevengStrategy;
import org.hibernate.tool.orm.jbt.api.wrp.ConfigurationWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.NamingStrategyWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.PersistentClassWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.RevengStrategyWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.SessionFactoryWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.TableWrapper;
import org.hibernate.tool.orm.jbt.internal.util.ExtendedConfiguration;
import org.hibernate.tool.orm.jbt.internal.util.JpaConfiguration;
import org.hibernate.tool.orm.jbt.internal.util.NativeConfiguration;
import org.hibernate.tool.orm.jbt.internal.util.RevengConfiguration;
import org.hibernate.tool.orm.jbt.internal.wrp.AbstractWrapper;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;

public class ConfigurationWrapperFactory {

	public static ConfigurationWrapper createNativeConfigurationWrapper() {
		return createConfigurationWrapper(new NativeConfiguration());
	}
	
	public static ConfigurationWrapper createRevengConfigurationWrapper() {
		return createConfigurationWrapper(new RevengConfiguration());
	}
	
	public static ConfigurationWrapper createJpaConfigurationWrapper(String persistenceUnit, Map<?,?> properties) {
		return new ConfigurationWrapperImpl(new JpaConfiguration(persistenceUnit, properties));
	}
	
	private static ConfigurationWrapper createConfigurationWrapper(final Configuration wrappedConfiguration) {
		return new ConfigurationWrapperImpl(wrappedConfiguration);
	}
	
	private static class ConfigurationWrapperImpl
			extends AbstractWrapper
			implements ConfigurationWrapper {
		
		private Configuration wrappedConfiguration = null;
		
		private NamingStrategyWrapper namingStrategyWrapper = null;
		
		private ConfigurationWrapperImpl(Configuration configuration) {
			wrappedConfiguration = configuration;
		}
		
		@Override 
		public Configuration getWrappedObject() { 
			return wrappedConfiguration; 
		}
		
		@Override
		public String getProperty(String property) { 
			return wrappedConfiguration.getProperty(property); 
		}
		
		@Override
		public ConfigurationWrapper addFile(File file) { 
			wrappedConfiguration.addFile(file); return this; 
		}
		
		@Override
		public void setProperty(String name, String value) { 
			wrappedConfiguration.setProperty(name, value); 
		}
		
		@Override
		public ConfigurationWrapper setProperties(Properties properties) { 
			wrappedConfiguration.setProperties(properties); return this; 
		}
		
		@Override
		public void setEntityResolver(EntityResolver entityResolver) {
			if (wrappedConfiguration instanceof ExtendedConfiguration) {
				((ExtendedConfiguration)wrappedConfiguration).setEntityResolver(entityResolver);
			}
		}
		
		@Override
		public void setNamingStrategy(NamingStrategyWrapper namingStrategyWrapper) {
			if (wrappedConfiguration instanceof ExtendedConfiguration) {
				((ExtendedConfiguration)wrappedConfiguration).setNamingStrategy((ImplicitNamingStrategy)namingStrategyWrapper.getWrappedObject());
			}
		}
		
		@Override
		public Properties getProperties() { 
			return wrappedConfiguration.getProperties(); 
		}
		
		@Override
		public void addProperties(Properties properties) { 
			wrappedConfiguration.addProperties(properties); 
		}
		
		@Override
		public ConfigurationWrapper configure(Document document) {
			if (wrappedConfiguration instanceof ExtendedConfiguration) {
				((ExtendedConfiguration)wrappedConfiguration).configure(document);
			}
			return this;		
		}
		
		@Override
		public ConfigurationWrapper configure(File file) { 
			wrappedConfiguration.configure(file); 
			return this; 
		}
		
		@Override
		public ConfigurationWrapper configure() { 
			wrappedConfiguration.configure(); 
			return this; 
		}
		
		@Override
		public void addClass(Class<?> clazz) { 
			wrappedConfiguration.addClass(clazz); 
		}
		
		@Override
		public void buildMappings() {
			if (wrappedConfiguration instanceof ExtendedConfiguration) {
				((ExtendedConfiguration)wrappedConfiguration).buildMappings();
			}
		}
		
		@Override
		public SessionFactoryWrapper buildSessionFactory() { 
			return SessionFactoryWrapperFactory.createSessionFactoryWrapper(((Configuration)getWrappedObject()).buildSessionFactory()); 
		}
		
		@Override
		public Iterator<PersistentClassWrapper> getClassMappings() { 
			if (wrappedConfiguration instanceof ExtendedConfiguration) {
				Iterator<PersistentClass> classMappings = ((ExtendedConfiguration)wrappedConfiguration).getClassMappings();
				return new Iterator<PersistentClassWrapper>() {
					@Override
					public boolean hasNext() {
						return classMappings.hasNext();
					}
					@Override
					public PersistentClassWrapper next() {
						return PersistentClassWrapperFactory.createPersistentClassWrapper(classMappings.next());
					}				
				};
			}
			return null;
		}
		
		@Override
		public void setPreferBasicCompositeIds(boolean b) {
			if (wrappedConfiguration instanceof ExtendedConfiguration) {
				((ExtendedConfiguration)wrappedConfiguration).setPreferBasicCompositeIds(b);
			}
		}
		
		@Override
		public void setReverseEngineeringStrategy(RevengStrategyWrapper strategy) {
			if (wrappedConfiguration instanceof ExtendedConfiguration) {
				((ExtendedConfiguration)wrappedConfiguration).setReverseEngineeringStrategy((RevengStrategy)strategy.getWrappedObject());
			}
		}
		
		@Override
		public void readFromJDBC() {
			if (wrappedConfiguration instanceof ExtendedConfiguration) {
				((ExtendedConfiguration)wrappedConfiguration).readFromJDBC();
			}
		}
		
		@Override
		public PersistentClassWrapper getClassMapping(String string) { 
			if (wrappedConfiguration instanceof ExtendedConfiguration) {
				PersistentClass classMapping = ((ExtendedConfiguration)wrappedConfiguration).getClassMapping(string);
				if (classMapping != null) {
					return PersistentClassWrapperFactory.createPersistentClassWrapper(classMapping);
				}
			}
			return null;
		}
		
		@Override
		public NamingStrategyWrapper getNamingStrategy() {
			ImplicitNamingStrategy namingStrategy = null;
			if (wrappedConfiguration instanceof ExtendedConfiguration) {
				namingStrategy = ((ExtendedConfiguration)wrappedConfiguration).getNamingStrategy();
			}
			if (namingStrategyWrapper == null || namingStrategyWrapper.getWrappedObject() != namingStrategy) {
				if (namingStrategy == null) {
					namingStrategyWrapper = null;
				} else {
					namingStrategyWrapper = NamingStrategyWrapperFactory.createNamingStrategyWrapper(namingStrategy);
				}
			}
			return namingStrategyWrapper;
		}
		
		@Override
		public EntityResolver getEntityResolver() {
			if (wrappedConfiguration instanceof ExtendedConfiguration) {
				return ((ExtendedConfiguration)wrappedConfiguration).getEntityResolver();
			}
			return null;
		}
		
		@Override
		public Iterator<TableWrapper> getTableMappings() {
			if (wrappedConfiguration instanceof ExtendedConfiguration) {
				Iterator<Table> tableMappings = ((ExtendedConfiguration)wrappedConfiguration).getTableMappings();
				return new Iterator<TableWrapper>() {
					@Override
					public boolean hasNext() {
						return tableMappings.hasNext();
					}
					@Override
					public TableWrapper next() {
						return TableWrapperFactory.createTableWrapper(tableMappings.next());
					}					
				};
			}
			return null;
		}
		
	}
	
}
