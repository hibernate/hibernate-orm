/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.lob;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;

import org.junit.jupiter.api.Test;

import static org.hibernate.Hibernate.getLobHelper;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Emmanuel Bernard
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsExpectedLobUsagePattern.class)
@Jpa(annotatedClasses = {
		ImageReader.class
})
public class BlobTest {
	@Test
	public void testBlobSerialization(EntityManagerFactoryScope scope) {
		Long readerId = scope.fromTransaction(
				entityManager -> {
					try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
						ObjectOutputStream oos = new ObjectOutputStream( baos )) {
						Map<String, String> image = new HashMap<>();
						image.put( "meta", "metadata" );
						image.put( "data", "imagedata" );
						ImageReader reader = new ImageReader();
						oos.writeObject( image );
						reader.setImage( getLobHelper().createBlob( baos.toByteArray() ) );
						entityManager.persist( reader );
						return reader.getId();
					}
					catch (IOException e) {
						throw new RuntimeException( e );
					}
				}
		);

		scope.inTransaction(
				entityManager -> {
					ImageReader reader = entityManager.find( ImageReader.class, readerId );
					try (ObjectInputStream ois = new ObjectInputStream( reader.getImage().getBinaryStream() )) {
						Map<String, String> image = (HashMap<String, String>) ois.readObject();
						assertTrue( image.containsKey( "meta" ) );
					}
					catch (Exception e) {
						throw new RuntimeException( e );
					}
				}
		);
	}
}
