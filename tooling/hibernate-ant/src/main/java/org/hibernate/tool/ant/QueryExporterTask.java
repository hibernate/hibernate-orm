package org.hibernate.tool.ant;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.export.ExporterFactory;
import org.hibernate.tool.api.export.ExporterType;
import org.hibernate.tool.util.StringUtil;

public class QueryExporterTask extends ExporterTask {

	private String query = "";
	private String filename;
	List<HQL> queries = new ArrayList<HQL>();

	public QueryExporterTask(HibernateToolTask parent) {
		super( parent );		
	}

	protected Exporter configureExporter(Exporter exp) {
		Exporter exporter = super.configureExporter( exp );		
		List<String> queryStrings = new ArrayList<String>();
		if(!StringUtil.isEmptyOrNull(query)) {
			queryStrings.add(query);
		}
		for (Iterator<HQL> iter = queries.iterator(); iter.hasNext();) {
			HQL hql = iter.next();
			if(!StringUtil.isEmptyOrNull(hql.query)) {
				queryStrings.add(hql.query);
			}
		}
		exporter.getProperties().put(ExporterConstants.QUERY_LIST, queryStrings);
		exporter.getProperties().put(ExporterConstants.OUTPUT_FILE_NAME, filename);
        return exporter;
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
	protected Exporter createExporter() {
		return ExporterFactory.createExporter(ExporterType.QUERY);
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
	 
	public void execute() {
		parent.log("Executing: [" + query + "]");
		super.execute();
	}
	public String getName() {
		return "query (Executes queries)";
	}
	
	
    boolean isNotEmpty(String string) {
		return string != null && string.length() > 0;
	}

}
