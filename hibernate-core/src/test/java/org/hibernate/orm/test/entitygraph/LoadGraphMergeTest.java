/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.hibernate.jpa.SpecHints.HINT_SPEC_LOAD_GRAPH;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(
		annotatedClasses = {
				LoadGraphMergeTest.Information.class,
				LoadGraphMergeTest.File.class,
				LoadGraphMergeTest.Content.class,
		}
)
@JiraKey( "HHH-15271" )
public class LoadGraphMergeTest {

	private static final Long INFORMATION_ID_1 = 1L;
	private static final Long INFORMATION_ID_2 = 2L;

	@BeforeAll
	public static void init(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Content content = new Content( 1L, "test content" );
					File file = new File( 1L, content );
					Information information = new Information( INFORMATION_ID_1, file );
					entityManager.persist( information );

					Content content2 = new Content( 2L, "test content" );
					File file2 = new File( 2L, content2 );
					Information information2 = new Information( INFORMATION_ID_2, file2 );
					entityManager.persist( information2 );
				}
		);
	}

	@Test
	public void testContentHasNotBeenInitializedByMerge(EntityManagerFactoryScope scope) {
		Information information1 = scope.fromTransaction( entityManager ->
				entityManager.find(
						Information.class,
						INFORMATION_ID_1,
						Collections.singletonMap(
								HINT_SPEC_LOAD_GRAPH,
								entityManager.getEntityGraph( "information.file" ) ) )
		);

		Information information2 = scope.fromTransaction( entityManager ->
				entityManager.find(
						Information.class,
						INFORMATION_ID_2,
						Collections.singletonMap(
								HINT_SPEC_LOAD_GRAPH,
								entityManager.getEntityGraph( "information.file" ) ) )
		);

		scope.inTransaction( entityManager -> {
			assertTrue( Hibernate.isInitialized( information1.getFile() ) );
			assertFalse( Hibernate.isInitialized( information1.getFile().getContent() ) );

			Session session = entityManager.unwrap( Session.class );

			Information mergedInformation = session.merge( information1,
					entityManager.getEntityGraph( "information.file" ) );

			File file = mergedInformation.getFile();
			assertTrue( Hibernate.isInitialized( file ) );
			assertFalse( Hibernate.isInitialized( file.getContent() ),
					"Merge has initialized `file.content` lazy association" );

			assertTrue( Hibernate.isInitialized( information2.getFile() ) );
			assertFalse( Hibernate.isInitialized( information2.getFile().getContent() ) );

			Information mergedInformation2 = session.merge( information2 );

			File file2 = mergedInformation2.getFile();
			assertTrue( Hibernate.isInitialized( file2 ) );
			assertTrue( Hibernate.isInitialized( file2.getContent() ) );
		} );
	}

	@Test
	public void testFileHasNotBeenInitializedByMerge(EntityManagerFactoryScope scope) {
		Information information = scope.fromTransaction( entityManager ->
				entityManager.find(
						Information.class,
						INFORMATION_ID_1 )
		);

		scope.inTransaction( entityManager -> {
			File file1 = information.getFile();
			assertFalse( Hibernate.isInitialized( file1 ) );

			Session session = entityManager.unwrap( Session.class );
			Information mergedInformation = session.merge( information, session.createEntityGraph( "information" ) );

			File file = mergedInformation.getFile();
			assertFalse( Hibernate.isInitialized( file ),
					"Merge has initialized `information.file` lazy association" );
		} );
	}

	@Entity(name = "Information")
	@NamedEntityGraph(
			name = "information.file",
			attributeNodes = @NamedAttributeNode("file")
	)
	@NamedEntityGraph(
			name = "information"
	)
	@Table(name = "INFORMATION_TABLE")
	public static class Information {

		@Id
		private Long id;

		private String info;

		@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		private File file;

		public Information() {
		}

		public Information(Long id, File file) {
			this.id = id;
			this.file = file;
		}

		public File getFile() {
			return file;
		}

		public Long getId() {
			return id;
		}
	}

	@Entity(name = "File")
	@Table(name = "FILE_TABLE")
	public static class File {

		@Id
		private Long id;

		private String name;

		@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		private Content content;

		public File() {
		}

		public File(Long id, Content content) {
			this.id = id;
			this.content = content;
		}

		public Content getContent() {
			return content;
		}

		public Long getId() {
			return id;
		}
	}

	@Entity(name = "Content")
	@Table(name = "CONTENT_TABLE")
	public static class Content {

		@Id
		private Long id;

		private String content;

		public Content() {
		}

		public Content(Long id, String content) {
			this.id = id;
			this.content = content;
		}

		public Long getId() {
			return id;
		}

		public String getContent() {
			return content;
		}
	}

}
