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
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.spi.ClassDetails;

/**
 * Executes HQL/JPQL queries against a database and writes the
 * results to a file or writer. Replaces the old
 * {@code QueryExporter} by working with {@link ClassDetails}
 * instead of {@code Metadata}.
 * <p>
 * Builds a {@link SessionFactory} from the given entities and
 * connection properties, executes each query, and writes the
 * {@code toString()} of each result row.
 *
 * @author Koen Aers
 */
public class QueryExporter {

	private final List<ClassDetails> entities;
	private final Properties properties;
	private final List<String> queries;

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
	 * Executes all queries and writes the results to the given file.
	 * Results are appended per query; each result row is written
	 * on its own line.
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
			Metadata metadata = buildMetadata();
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

	private Metadata buildMetadata() {
		StandardServiceRegistry serviceRegistry =
				new StandardServiceRegistryBuilder()
						.applySettings(properties)
						.build();
		MetadataSources sources = new MetadataSources(serviceRegistry);
		for (ClassDetails entity : entities) {
			sources.addAnnotatedClassName(entity.getClassName());
		}
		return sources.buildMetadata();
	}
}
