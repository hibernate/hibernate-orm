/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.entitygraph.aftermerge;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;


import org.hibernate.Hibernate;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.FailureExpected;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LoadGraphAfterMergeTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {File.class, Content.class, Information.class};
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		doInJPA( this::entityManagerFactory, em -> {
			Information information = new Information();
			information.setId(1L);

			File file = new File();
			file.setId(1L);

			Content content = new Content();
			content.setId(1L);
			content.setContentArray("test".getBytes());

			file.setContent(content);
			information.setFile(file);

			em.persist(information);
		} );
	}

	@Test
	@FailureExpected( jiraKey = "HHH-15271" )
	public void checkIfInitializedAfterMerge() {
		final Information[] result = new Information[1];

		doInJPA( this::entityManagerFactory, em -> {
			result[0] = em.find( Information.class, 1L, createProperties( em ));
		} );

		doInJPA( this::entityManagerFactory, em -> {
			assertTrue(Hibernate.isInitialized(result[0].getFile()));
			assertFalse(Hibernate.isInitialized(result[0].getFile().getContent()));

			result[0] = em.merge(result[0]);

			assertTrue(Hibernate.isInitialized(result[0].getFile()));
			// fails here, expected behaviour?
			assertFalse(Hibernate.isInitialized(result[0].getFile().getContent()));
		} );
	}

	private Map<String, Object> createProperties(EntityManager em) {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(
				"javax.persistence.loadgraph",
				createEntityGraph( em )
		);
		return properties;
	}

	private EntityGraph<Information> createEntityGraph(EntityManager em) {
		EntityGraph<Information> entityGraph = em.createEntityGraph( Information.class );
		entityGraph.addAttributeNodes( "file" );
		return entityGraph;
	}

	@Entity(name = "content")
	@Table(name = "CONTENTS")
	@Access(AccessType.PROPERTY)
	public static class Content {

		private Long id;
		private byte[] contentArray;

		public byte[] getContentArray() {
			return contentArray;
		}

		public void setContentArray(byte[] contentArray) {
			this.contentArray = contentArray;
		}

		@Id
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "file")
	@Table(name = "FILES")
	@Access(AccessType.PROPERTY)
	public static class File {

		private Long id;
		private Content content;

		@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		public Content getContent() {
			return content;
		}

		public void setContent(Content content) {
			this.content = content;
		}

		@Id
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "information")
	@Table(name = "INFORMATIONS")
	@Access(AccessType.PROPERTY)
	public static class Information {

		private Long id;
		private File file;

		@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		public File getFile() {
			return file;
		}

		public void setFile(File file) {
			this.file = file;
		}

		@Id
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}
}
