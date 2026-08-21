/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

import org.hibernate.testing.orm.junit.Jira;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import jakarta.persistence.*;

import static org.assertj.core.api.Assertions.assertThat;

public class GenericsHelperTest {
	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-20265")
	public void testChainedTypeVariable() throws Exception {
		Type t = GenericsHelper.actualInheritedMemberType(
				BaseLibraryEntity.class,
				BaseLibraryEntity.class.getDeclaredField( "processed" )
		);

		assertThat(t).isEqualTo(boolean.class);
	}

	@MappedSuperclass
	static abstract class BaseEntity<T> {
	}

	@MappedSuperclass
	static abstract class BaseReadableEntity<T> extends BaseEntity<T> {
	}

	@MappedSuperclass
	static abstract class BaseLibraryEntity<T> extends BaseReadableEntity<T> {
		private boolean processed;
	}

	@Entity
	static class Book extends BaseLibraryEntity<String> {
		@Id
		private String id;
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-20467")
	public void testUnboundConverterTypeVariable() {
		Type[] types = GenericsHelper.typeArguments(
				AttributeConverter.class,
				AbstractMiddleConverter.class
		);

		assertThat( types ).hasSize( 2 );
		assertThat( types[0] ).isEqualTo( Object.class );
		assertThat( types[1] ).isEqualTo( String.class );
	}

	abstract static class AbstractBaseConverter<T> implements AttributeConverter<T, String> {
	}

	abstract static class AbstractMiddleConverter<T> extends AbstractBaseConverter<T> {
	}
}
