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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl.MetadataBuildingOptionsImpl;
import org.hibernate.boot.internal.MetadataBuildingContextRootImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.api.reveng.RevengDialect;
import org.hibernate.tool.reveng.api.reveng.RevengDialectFactory;
import org.hibernate.tool.reveng.api.reveng.RevengStrategy;
import org.hibernate.tool.reveng.internal.reveng.RevengMetadataCollector;
import org.hibernate.tool.reveng.internal.reveng.reader.DatabaseReader;
import org.hibernate.tool.orm.jbt.api.wrp.DatabaseReaderWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.RevengStrategyWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.TableWrapper;
import org.hibernate.tool.orm.jbt.internal.wrp.AbstractWrapper;

public class DatabaseReaderWrapperFactory {
		
	public static DatabaseReaderWrapper createDatabaseReaderWrapper(
			Properties properties, 
			RevengStrategyWrapper revengStrategy) {
		return new DatabaseReaderWrapperImpl(properties, (RevengStrategy)revengStrategy.getWrappedObject());
	}
	
	static class DatabaseReaderWrapperImpl 
			extends AbstractWrapper
			implements DatabaseReaderWrapper {
		
		DatabaseReader databaseReader = null;
		RevengMetadataCollector revengMetadataCollector = null;
		
		public DatabaseReaderWrapperImpl(
				Properties properties, 
				RevengStrategy revengStrategy) {
			StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
					.applySettings(properties)
					.build();
			MetadataBuildingOptionsImpl metadataBuildingOptions = 
					new MetadataBuildingOptionsImpl(serviceRegistry);	
			BootstrapContextImpl bootstrapContext = new BootstrapContextImpl(
					serviceRegistry, 
					metadataBuildingOptions);
			metadataBuildingOptions.setBootstrapContext(bootstrapContext);
			InFlightMetadataCollectorImpl metadataCollector = new InFlightMetadataCollectorImpl(
					bootstrapContext,
					metadataBuildingOptions);
			RevengDialect mdd = RevengDialectFactory
					.createMetaDataDialect(
							serviceRegistry.getService(JdbcServices.class).getDialect(), 
							properties );
		    databaseReader = DatabaseReader.create(properties,revengStrategy,mdd, serviceRegistry);
		    MetadataBuildingContext metadataBuildingContext = new MetadataBuildingContextRootImpl(
		    		"JBoss Tools", 
		    		bootstrapContext, 
		    		metadataBuildingOptions, 
		    		metadataCollector,
		    		null);
		    revengMetadataCollector = new RevengMetadataCollector(metadataBuildingContext);
		}
		
		public Map<String, List<TableWrapper>> collectDatabaseTables() {
			databaseReader.readDatabaseSchema(revengMetadataCollector);
			Map<String, List<TableWrapper>> result = new HashMap<String, List<TableWrapper>>();
			for (Table table : revengMetadataCollector.getTables()) {
				String qualifier = "";
				if (table.getCatalog() != null) {
					qualifier += table.getCatalog();
				}
				if (table.getSchema() != null) {
					if (!"".equals(qualifier)) {
						qualifier += ".";
					}
					qualifier += table.getSchema();
				}
				List<TableWrapper> list = result.get(qualifier);
				if (list == null) {
					list = new ArrayList<TableWrapper>();
					result.put(qualifier, list);
				}
				list.add(TableWrapperFactory.createTableWrapper(table));
			}			
			return result;
		}
		
	}
	
	

}
