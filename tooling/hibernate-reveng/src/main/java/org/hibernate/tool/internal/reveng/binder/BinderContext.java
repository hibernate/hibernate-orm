/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
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
package org.hibernate.tool.internal.reveng.binder;

import java.util.Properties;

import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.tool.api.reveng.RevengStrategy;

public class BinderContext {
	
	public static BinderContext create(
			MetadataBuildingContext metadataBuildingContext,
			InFlightMetadataCollector metadataCollector,
			RevengStrategy revengStrategy,
			Properties properties) {
		return new BinderContext(
				metadataBuildingContext, 
				metadataCollector, 
				revengStrategy, 
				properties);
	}
	
	public final MetadataBuildingContext metadataBuildingContext;
	public final InFlightMetadataCollector metadataCollector;
	public final RevengStrategy revengStrategy;
	public final Properties properties;
	
	private BinderContext(
			MetadataBuildingContext metadataBuildingContext,
			InFlightMetadataCollector metadataCollector,
			RevengStrategy revengStrategy,
			Properties properties) {
		this.metadataBuildingContext = metadataBuildingContext;
		this.metadataCollector = metadataCollector;
		this.revengStrategy = revengStrategy;
		this.properties = properties;
	}

}
