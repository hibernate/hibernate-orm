/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.engine.spi;

import jakarta.persistence.Id;
import org.hibernate.engine.spi.ExtensionStorage;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DomainModel(annotatedClasses = {
		SessionExtensionTest.UselessEntity.class,
})
@SessionFactory
public class SessionExtensionTest {

	@Test
	public void failing(SessionFactoryScope scope) {
		scope.inSession( sessionImplementor -> {
			assertThatThrownBy(
					() -> sessionImplementor.getExtensionStorage( MySometimesFailingExtensionStorage.class, MySometimesFailingExtensionStorage::new ) )
					.isInstanceOf( UnsupportedOperationException.class );
		} );

		scope.inStatelessSession( sessionImplementor -> {
			assertThatThrownBy(
					() -> sessionImplementor.getExtensionStorage( MySometimesFailingExtensionStorage.class, MySometimesFailingExtensionStorage::new ) )
					.isInstanceOf( UnsupportedOperationException.class );
		} );
	}

	@Test
	public void supplier(SessionFactoryScope scope) {
		scope.inSession( sessionImplementor -> {
			sessionImplementor.getExtensionStorage( MySometimesFailingExtensionStorage.class,
							() -> new MySometimesFailingExtensionStorage( new HashMap<>() ) )
					.add( new Extension( 1 ) );

			assertThat( sessionImplementor.getExtensionStorage( MySometimesFailingExtensionStorage.class, MySometimesFailingExtensionStorage::new ).get( 1 ) )
					.isNotNull()
					.isEqualTo( new Extension( 1 ) );

			assertThat( sessionImplementor.getExtensionStorage( MySometimesFailingExtensionStorage.class,
					() -> new MySometimesFailingExtensionStorage( new HashMap<>() ) ).get( 1 ) )
					.isNotNull()
					.isEqualTo( new Extension( 1 ) );
		} );

		scope.inStatelessSession( sessionImplementor -> {
			sessionImplementor.getExtensionStorage( MySometimesFailingExtensionStorage.class,
							() -> new MySometimesFailingExtensionStorage( new HashMap<>() ) )
					.add( new Extension( 1 ) );

			assertThat( sessionImplementor.getExtensionStorage( MySometimesFailingExtensionStorage.class, MySometimesFailingExtensionStorage::new ).get( 1 ) )
					.isNotNull()
					.isEqualTo( new Extension( 1 ) );

			assertThat( sessionImplementor.getExtensionStorage( MySometimesFailingExtensionStorage.class,
					() -> new MySometimesFailingExtensionStorage( new HashMap<>() ) ).get( 1 ) )
					.isNotNull()
					.isEqualTo( new Extension( 1 ) );
		} );
	}

	public static class MySometimesFailingExtensionStorage implements ExtensionStorage {
		Map<Integer, Extension> extensions = new HashMap<>();

		public MySometimesFailingExtensionStorage() {
			throw new UnsupportedOperationException();
		}

		MySometimesFailingExtensionStorage(Map<Integer, Extension> extensions) {
			this.extensions = extensions;
		}

		public void add(Extension extension) {
			extensions.put( extension.number, extension );
		}

		public Extension get(int number) {
			return extensions.get( number );
		}
	}

	public record Extension(int number) {
	}

	static class UselessEntity {
		@Id
		Long id;
	}
}
