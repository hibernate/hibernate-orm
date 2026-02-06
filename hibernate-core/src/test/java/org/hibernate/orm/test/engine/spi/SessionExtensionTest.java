/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.engine.spi;

import jakarta.persistence.Id;
import org.hibernate.engine.extension.spi.ExtensionIntegration;
import org.hibernate.engine.extension.spi.ExtensionIntegrationContext;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		SessionExtensionTest.UselessEntity.class,
})
@BootstrapServiceRegistry(
		javaServices = @BootstrapServiceRegistry.JavaService(role = ExtensionIntegration.class,
				impl = SessionExtensionTest.TestExtensionIntegration.class)
)
@SessionFactory
public class SessionExtensionTest {

	@Test
	public void smoke(SessionFactoryScope scope) {
		scope.inSession( sessionImplementor -> {
			sessionImplementor.getExtension( MySometimesFailingExtensionStorage.class ).add( new ExtensionData( 1 ) );
			assertThat( sessionImplementor.getExtension( MySometimesFailingExtensionStorage.class ).get( 1 ) )
					.isNotNull();
		} );

		scope.inSession( sessionImplementor -> {
			assertThat( sessionImplementor.getExtension( MySometimesFailingExtensionStorage.class ).get( 1 ) )
					.isNull();
			sessionImplementor.getExtension( MySometimesFailingExtensionStorage.class ).add( new ExtensionData( 1 ) );
			assertThat( sessionImplementor.getExtension( MySometimesFailingExtensionStorage.class ).get( 1 ) )
					.isNotNull();
		} );
	}

	public static class MySometimesFailingExtensionStorage implements org.hibernate.engine.extension.spi.Extension {
		Map<Integer, ExtensionData> data = new HashMap<>();

		public MySometimesFailingExtensionStorage() {
			throw new UnsupportedOperationException();
		}

		MySometimesFailingExtensionStorage(Map<Integer, ExtensionData> data) {
			this.data = data;
		}

		public void add(ExtensionData extension) {
			data.put( extension.number, extension );
		}

		public ExtensionData get(int number) {
			return data.get( number );
		}
	}

	public record ExtensionData(int number) {
	}

	static class UselessEntity {
		@Id
		Long id;
	}

	public static class TestExtensionIntegration implements ExtensionIntegration<MySometimesFailingExtensionStorage> {

		@Override
		public Class<MySometimesFailingExtensionStorage> getExtensionType() {
			return MySometimesFailingExtensionStorage.class;
		}

		@Override
		public MySometimesFailingExtensionStorage createExtension(ExtensionIntegrationContext context) {
			return new MySometimesFailingExtensionStorage( new HashMap<>() );
		}
	}
}
