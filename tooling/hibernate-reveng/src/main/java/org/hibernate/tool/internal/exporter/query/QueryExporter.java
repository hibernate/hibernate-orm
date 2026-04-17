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
package org.hibernate.tool.internal.exporter.query;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;
import java.util.Properties;

import org.hibernate.boot.Metadata;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.internal.metadata.MetadataBootstrapper;

/**
 * Executes HQL/JPQL queries against a database and writes the
 * results to a file or writer. Bootstraps {@link org.hibernate.boot.Metadata}
 * from {@link ClassDetails} via {@link MetadataBootstrapper}, then
 * builds a {@link SessionFactory} to execute the queries.
 * <p>
 * This exporter requires that the entity classes referenced by the
 * {@code ClassDetails} are available on the classpath. When used
 * with reverse-engineered entities (which have no compiled Java
 * classes), a {@link HibernateException} will be thrown with a
 * descriptive error message.
 *
 * @author Koen Aers
 */
public class QueryExporter implements Exporter {

	private Metadata metadata;
	private List<ClassDetails> entities;
	private Properties properties;
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

	private QueryExporter(List<ClassDetails> entities,
						   Properties properties,
						   List<String> queries) {
		this.entities = entities;
		this.properties = properties;
		this.queries = queries;
	}

	public static QueryExporter create(List<ClassDetails> entities,
										Properties properties,
										List<String> queries) {
		return new QueryExporter(entities, properties, queries);
	}

	/**
	 * Creates a QueryExporter from a MetadataDescriptor.
	 * Uses {@link MetadataDescriptor#createMetadata()} directly
	 * to preserve the full Hibernate ORM metadata model (including
	 * polymorphic queries and cfg.xml mappings).
	 */
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
	 *
	 * @throws HibernateException if the entity classes are not
	 *         available on the classpath (e.g. reverse-engineered
	 *         entities that have not been compiled yet)
	 */
	public void export(Writer output) {
		if (metadata != null) {
			executeQueries(metadata, output);
		} else {
			try (MetadataBootstrapper.MetadataContext ctx =
					MetadataBootstrapper.bootstrap(entities, properties)) {
				executeQueries(ctx.metadata(), output);
			}
		}
	}

	private void executeQueries(Metadata md, Writer output) {
		SessionFactory sessionFactory = null;
		Session session = null;
		Transaction transaction = null;
		try {
			sessionFactory = md.buildSessionFactory();
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
				try {
					transaction.rollback();
				} catch (Exception ignored) {}
			}
			throw new HibernateException(
					"Failed to execute queries. If using reverse-engineered "
					+ "entities, ensure the generated Java classes have been "
					+ "compiled and are available on the classpath.", e);
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
