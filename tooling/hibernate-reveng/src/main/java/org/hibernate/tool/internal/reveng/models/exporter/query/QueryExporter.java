/*
 * Copyright 2010 - 2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.models.exporter.query;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;

import java.util.Properties;

/**
 * Executes HQL/JPQL queries against a database and writes the
 * results to a file or writer. Uses {@link Metadata} directly
 * to build a {@link SessionFactory}.
 *
 * @author Koen Aers
 */
public class QueryExporter implements Exporter {

	private Metadata metadata;
	private List<String> queries;
	private Properties exporterProperties = new Properties();

	public QueryExporter() {}

	@Override
	public Properties getProperties() {
		return exporterProperties;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void start() {
		MetadataDescriptor md = (MetadataDescriptor)
				exporterProperties.get(ExporterConstants.METADATA_DESCRIPTOR);
		File destDir = (File)
				exporterProperties.get(ExporterConstants.DESTINATION_FOLDER);
		List<String> queryList = (List<String>)
				exporterProperties.get(ExporterConstants.QUERY_LIST);
		String outputFileName = (String)
				exporterProperties.get(ExporterConstants.OUTPUT_FILE_NAME);
		QueryExporter configured = create(md, queryList);
		File outputFile = new File(destDir, outputFileName != null ? outputFileName : "query-output.txt");
		configured.export(outputFile);
	}

	private QueryExporter(Metadata metadata, List<String> queries) {
		this.metadata = metadata;
		this.queries = queries;
	}

	public static QueryExporter create(Metadata metadata,
										List<String> queries) {
		return new QueryExporter(metadata, queries);
	}

	public static QueryExporter create(MetadataDescriptor md,
										List<String> queries) {
		return new QueryExporter(md.createMetadata(), queries);
	}

	/**
	 * Executes all queries and writes the results to the given file.
	 */
	public void export(File outputFile) {
		outputFile.getParentFile().mkdirs();
		try (Writer writer = new FileWriter(outputFile)) {
			export(writer);
		}
		catch (IOException e) {
			throw new RuntimeException(
					"Failed to write query output to "
					+ outputFile, e);
		}
	}

	/**
	 * Executes all queries and writes the results to the given
	 * writer. Each result row is written on its own line.
	 */
	public void export(Writer output) {
		SessionFactory sessionFactory = null;
		Session session = null;
		Transaction transaction = null;
		try {
			sessionFactory = metadata.buildSessionFactory();
			session = sessionFactory.openSession();
			transaction = session.beginTransaction();
			PrintWriter pw = new PrintWriter(output);
			for (String query : queries) {
				List<?> results = session
						.createQuery(query, (Class<?>) null)
						.getResultList();
				for (Object row : results) {
					pw.println(row);
				}
			}
			pw.flush();
			transaction.commit();
		}
		catch (HibernateException e) {
			if (transaction != null) {
				transaction.rollback();
			}
			throw new RuntimeException(
					"Error occurred while executing query", e);
		}
		finally {
			if (session != null) {
				session.close();
			}
			if (sessionFactory != null) {
				sessionFactory.close();
			}
		}
	}
}
