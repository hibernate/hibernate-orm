/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.internal.exporter.query.QueryExporter;
import org.hibernate.tool.reveng.util.StringUtil;

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
		}
		else {
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
