/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.lob;

import org.hibernate.bugs.TestEntity;
import org.hibernate.engine.jdbc.env.internal.NonContextualLobCreator;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = TestEntity.class)
@SessionFactory
@JiraKey( "HHH-19464" )
class JarFileEntryBlobTest {

	@Test
	void hibernate_blob_streaming(SessionFactoryScope scope) throws Exception {
		final var zipFilePath = getClass().getClassLoader().getResource( "org/hibernate/orm/test/lob/JarFileEntryBlobTest.zip" );
		File file = new File( zipFilePath.toURI() );

		try (JarFile jarFile = new JarFile( file )) {
			JarEntry entry = jarFile.getJarEntry( "pizza.png" );
			long size = entry.getSize();
			scope.inTransaction( entityManager -> {
						try {
							InputStream is = jarFile.getInputStream( entry );
							Blob blob = NonContextualLobCreator.INSTANCE.wrap(
									NonContextualLobCreator.INSTANCE.createBlob( is, size )
							);
							TestEntity e = new TestEntity();
							e.setId( 1L );
							e.setData( blob );

							entityManager.persist( e );
						}
						catch (IOException e) {
							throw new RuntimeException( e );
						}
					}
			);

			scope.inStatelessSession( session -> {
				final var entity = session.get( TestEntity.class, 1L );
				try {
					assertEquals( size, entity.getData().length() );
				}
				catch (SQLException e) {
					throw new RuntimeException( e );
				}
			} );
		}
	}
}
