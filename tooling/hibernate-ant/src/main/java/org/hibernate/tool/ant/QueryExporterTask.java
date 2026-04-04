/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
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
package org.hibernate.tool.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.internal.reveng.models.exporter.query.QueryExporter;
import org.hibernate.tool.util.StringUtil;

public class QueryExporterTask extends ExporterTask {

	private String query = "";
	private String filename;
	List<HQL> queries = new ArrayList<HQL>();

	public QueryExporterTask(HibernateToolTask parent) {
		super( parent );
	}

	public void validateParameters() {
		super.validateParameters();
		if(StringUtil.isEmptyOrNull(query) && queries.isEmpty()) {
			throw new BuildException("Need to specify at least one query.");
		}

		for (Iterator<HQL> iter = queries.iterator(); iter.hasNext();) {
			HQL hql = iter.next();
			if(StringUtil.isEmptyOrNull(hql.query)) {
				throw new BuildException("Query must not be empty");
			}
		}
	}

	@Override
	public void execute() {
		parent.log("Executing: [" + query + "]");
		MetadataDescriptor md = parent.getMetadataDescriptor();
		List<String> queryStrings = new ArrayList<>();
		if (!StringUtil.isEmptyOrNull(query)) {
			queryStrings.add(query);
		}
		for (HQL hql : queries) {
			if (!StringUtil.isEmptyOrNull(hql.query)) {
				queryStrings.add(hql.query);
			}
		}
		QueryExporter exporter = QueryExporter.create(md, queryStrings);
		if (filename != null) {
			exporter.export(new File(getDestdir(), filename));
		} else {
			exporter.export(new File(getDestdir(), "query-output.txt"));
		}
	}

	protected Exporter createExporter() {
		return null;
	}

	public void addText(String text) {
		if(!StringUtil.isEmptyOrNull(text)) {
		  query += trim(text);
		}
	}

	static private String trim(String text) {
		return text.trim();
	}

	public static class HQL {
		String query = "";
		public void addText(String text) {
			if(!StringUtil.isEmptyOrNull(text)) {
				query += trim(text);
			}
		}
	}

	public HQL createHql() {
		HQL hql = new HQL();
		queries.add(hql);
		return hql;
	}

	public void setDestFile(String filename) {
		this.filename = filename;
	}

	public String getName() {
		return "query (Executes queries)";
	}

    boolean isNotEmpty(String string) {
		return string != null && string.length() > 0;
	}

}
