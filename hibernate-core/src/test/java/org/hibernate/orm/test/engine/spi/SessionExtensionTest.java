/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.engine.spi;

import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DomainModel(annotatedClasses = {
		SessionExtensionTest.UselessEntity.class,
})
@SessionFactory
public class SessionExtensionTest {

	@Test
	public void smoke(SessionFactoryScope scope) {
		final String extensionName = "my-extension-key";
		scope.inSession( sessionImplementor -> {
			sessionImplementor.attachExtension( extensionName, new Extension( 1 ) );

			assertThat( sessionImplementor.retrieveExtension( extensionName, Extension.class ) )
					.isNotNull()
					.isEqualTo( new Extension( 1 ) );
		} );

		scope.inStatelessSession( sessionImplementor -> {
			sessionImplementor.attachExtension( extensionName, new Extension( 1 ) );

			assertThat( sessionImplementor.retrieveExtension( extensionName, Extension.class ) )
					.isNotNull()
					.isEqualTo( new Extension( 1 ) );
		} );
	}

	@Test
	public void cast(SessionFactoryScope scope) {
		final String extensionName = "my-extension-key";
		scope.inSession( sessionImplementor -> {
			sessionImplementor.attachExtension( extensionName, new Extension( 1 ) );

			assertThatThrownBy(
					() -> sessionImplementor.retrieveExtension( extensionName, SessionExtensionTest.class ) )
					.isInstanceOf( ClassCastException.class );
		} );

		scope.inStatelessSession( sessionImplementor -> {
			sessionImplementor.attachExtension( extensionName, new Extension( 1 ) );

			assertThatThrownBy(
					() -> sessionImplementor.retrieveExtension( extensionName, SessionExtensionTest.class ) )
					.isInstanceOf( ClassCastException.class );
		} );
	}

	private record Extension(int number) {
	}

	static class UselessEntity {
		@Id
		Long id;
	}
}
