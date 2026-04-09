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

import java.util.List;

import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.internal.export.query.QueryExporter;
import org.hibernate.tool.orm.jbt.api.wrp.QueryExporterWrapper;
import org.hibernate.tool.orm.jbt.internal.wrp.AbstractWrapper;

public class QueryExporterWrapperFactory {

	public static QueryExporterWrapper createQueryExporterWrapper(QueryExporter wrappedQueryExporter) {
		return new QueryExporterWrapperImpl(wrappedQueryExporter);
	}
	
	private static class QueryExporterWrapperImpl 
			extends AbstractWrapper
			implements QueryExporterWrapper {
		
		private QueryExporter queryExporter = null;
		
		private QueryExporterWrapperImpl(QueryExporter queryExporter) {
			this.queryExporter = queryExporter;
		}
		
		@Override 
		public QueryExporter getWrappedObject() { 
			return queryExporter; 
		}
		
		@Override
		public void setQueries(List<String> queries) { 
			queryExporter.setQueries(queries); 
		}	
		
		@Override
		public void setFilename(String fileName) { 
			queryExporter.getProperties().put(ExporterConstants.OUTPUT_FILE_NAME, fileName); 
		}

	}

}
