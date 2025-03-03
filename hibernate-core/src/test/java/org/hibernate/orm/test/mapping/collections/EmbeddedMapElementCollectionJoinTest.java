/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		EmbeddedMapElementCollectionJoinTest.ImageFile.class,
		EmbeddedMapElementCollectionJoinTest.ImageKey.class,
		EmbeddedMapElementCollectionJoinTest.ImagesEmbeddable.class,
		EmbeddedMapElementCollectionJoinTest.TestEntity.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16540" )
public class EmbeddedMapElementCollectionJoinTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ImagesEmbeddable images = new ImagesEmbeddable();
			images.getImagesMap().put( new ImageKey( 1 ), new ImageFile( "file_1" ) );
			session.persist( new TestEntity( images ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from TestEntity" ).executeUpdate() );
	}

	@Test
	public void testMapJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<ImageFile> cq = cb.createQuery( ImageFile.class );
			final Root<TestEntity> root = cq.from( TestEntity.class );
			final ImageFile result = session.createQuery(
					cq.select( root.join( "images" ).joinMap( "imagesMap" ) )
			).getSingleResult();
			assertThat( result.getFileName() ).isEqualTo( "file_1" );
		} );
	}

	@Embeddable
	public static class ImageFile {
		private String fileName;

		public ImageFile() {
		}

		public ImageFile(String fileName) {
			this.fileName = fileName;
		}

		public String getFileName() {
			return fileName;
		}
	}

	@Embeddable
	public static class ImageKey {
		private int imageKey;

		public ImageKey() {
		}

		public ImageKey(int imageKey) {
			this.imageKey = imageKey;
		}

		public int getImageKey() {
			return imageKey;
		}
	}

	@Embeddable
	public static class ImagesEmbeddable {
		@ElementCollection( fetch = FetchType.LAZY )
		private Map<ImageKey, ImageFile> imagesMap = new HashMap<>();

		public Map<ImageKey, ImageFile> getImagesMap() {
			return imagesMap;
		}
	}

	@Entity( name = "TestEntity" )
	public static class TestEntity {
		@Id
		@GeneratedValue
		private int id;
		@Embedded
		private ImagesEmbeddable images = new ImagesEmbeddable();

		public TestEntity() {
		}

		public TestEntity(ImagesEmbeddable images) {
			this.images = images;
		}

		public int getId() {
			return id;
		}

		public ImagesEmbeddable getImages() {
			return images;
		}
	}
}
