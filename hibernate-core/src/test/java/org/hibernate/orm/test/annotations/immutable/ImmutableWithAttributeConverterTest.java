/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.immutable;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.testing.orm.domain.DomainModelDescriptor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		modelDescriptorClasses = ImmutableWithAttributeConverterTest.ImmutableModel.class
)
@SessionFactory
public class ImmutableWithAttributeConverterTest {

	public static class ImmutableModel implements DomainModelDescriptor {
		@Override
		public Class[] getAnnotatedClasses() {
			return new Class[] {
					Country.class,
					State.class,
					Photo.class
			};
		}


		@Override
		public void applyDomainModel(MetadataSources sources) {
			MetadataBuilder metadataBuilder = sources.getMetadataBuilder();
			metadataBuilder.applyAttributeConverter( ExifConverter.class );
			metadataBuilder.applyAttributeConverter( CaptionConverter.class );
		}
	}

	@AfterEach
	public void afterEach(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testImmutableAttribute(SessionFactoryScope scope) {

		Photo photo = new Photo();
		scope.inTransaction(
				session -> {
					photo.setName( "cat.jpg" );
					photo.setMetadata( new Exif( Collections.singletonMap( "fake", "first value" ) ) );
					photo.setCaption( new Caption( "Cat.jpg caption" ) );
					session.persist( photo );
				}
		);

		// try changing the attribute
		scope.inTransaction(
				session -> {
					Photo cat = session.get( Photo.class, photo.getId() );
					assertThat( cat ).isNotNull();
					cat.getMetadata().getAttributes().put( "fake", "second value" );
					cat.getCaption().setText( "new caption" );
				}
		);

		// retrieving the attribute again - it should be unmodified since object identity is the same
		scope.inTransaction(
				session -> {
					Photo cat = session.get( Photo.class, photo.getId() );
					assertThat( cat ).isNotNull();
					assertThat( cat.getMetadata().getAttribute( "fake" ) )
							.describedAs( "Metadata should not have changed" )
							.isEqualTo( "first value" );
					assertThat( cat.getCaption().getText() )
							.describedAs( "Caption should not have changed" )
							.isEqualTo( "Cat.jpg caption" );
				}
		);
	}

	@Test
	public void testChangeImmutableAttribute(SessionFactoryScope scope) {

		Photo photo = new Photo();
		scope.inTransaction(
				session -> {
					photo.setName( "cat.jpg" );
					photo.setMetadata( new Exif( Collections.singletonMap( "fake", "first value" ) ) );
					photo.setCaption( new Caption( "Cat.jpg caption" ) );
					session.persist( photo );
				}
		);

		// replacing the attribute
		scope.inTransaction(
				session -> {
					Photo cat = session.get( Photo.class, photo.getId() );
					assertThat( cat ).isNotNull();
					cat.setMetadata( new Exif( Collections.singletonMap( "fake", "second value" ) ) );
					cat.setCaption( new Caption( "new caption" ) );
				}
		);

		// retrieving the attribute again - it should be modified since the holder object has changed as well
		scope.inTransaction(
				session -> {
					Photo cat = session.get( Photo.class, photo.getId() );
					assertThat( cat ).isNotNull();

					assertThat( cat.getMetadata().getAttribute( "fake" ) )
							.describedAs( "Metadata should have changed" )
							.isEqualTo( "second value" );
					assertThat( cat.getCaption().getText() )
							.describedAs( "Caption should have changed" )
							.isEqualTo( "new caption" );
				}
		);
	}

}
