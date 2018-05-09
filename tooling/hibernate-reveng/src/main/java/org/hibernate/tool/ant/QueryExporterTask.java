package org.hibernate.tool.ant;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.hibernate.tool.hbm2x.Exporter;
import org.hibernate.tool.hbm2x.QueryExporter;
import org.hibernate.internal.util.StringHelper;

public class QueryExporterTask extends ExporterTask {

	private String query = "";
	private String filename;
	List<HQL> queries = new ArrayList<HQL>();

	public QueryExporterTask(HibernateToolTask parent) {
		super( parent );		
	}

	protected Exporter configureExporter(Exporter exp) {
		QueryExporter exporter = (QueryExporter) exp;
		List<String> queryStrings = new ArrayList<String>();
		if(StringHelper.isNotEmpty(query)) {
			queryStrings.add(query);
		}
		for (Iterator<HQL> iter = queries.iterator(); iter.hasNext();) {
			HQL hql = iter.next();
			if(StringHelper.isNotEmpty(hql.query)) {
				queryStrings.add(hql.query);
			}
		}
		exporter.setQueries(queryStrings);
		exporter.setFilename(filename);
		super.configureExporter( exp );		
        return exporter;
	}

	public void validateParameters() {
		super.validateParameters();
		if(StringHelper.isEmpty(query) && queries.isEmpty()) {
			throw new BuildException("Need to specify at least one query.");
		}
		
		for (Iterator<HQL> iter = queries.iterator(); iter.hasNext();) {
			HQL hql = iter.next();
			if(StringHelper.isEmpty(hql.query)) {
				throw new BuildException("Query must not be empty");
			}
		}
	}
	protected Exporter createExporter() {
		QueryExporter exporter = new QueryExporter();
		return exporter;
	}

	public void addText(String text) {
		if(StringHelper.isNotEmpty(text)) {
		  query += trim(text);
		}
	}
	
	static private String trim(String text) {
		return text.trim();
	}

	public static class HQL {
		String query = "";
		public void addText(String text) {
			if(StringHelper.isNotEmpty(text)) {
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
	 
	public void execute() {
		parent.log("Executing: [" + query + "]");
		super.execute();
	}
	public String getName() {
		return "query (Executes queries)";
	}
	
	
}
