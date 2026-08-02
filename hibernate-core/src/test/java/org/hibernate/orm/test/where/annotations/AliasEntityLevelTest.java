/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.AnnotationException;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.boot.MetadataSources;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests that {@linkplain SqlFragmentAlias aliases} are rejected for
 * an entity-level {@link SQLRestriction}, where they are unsupported.
 */
@BaseUnitTest
@Jira( "https://hibernate.atlassian.net/browse/HHH-12016" )
public class AliasEntityLevelTest {
	@Test
	public void testEntityLevelAliasesRejected() {
		final var annotationException = assertThrows( AnnotationException.class, () -> {
			try (var serviceRegistry = ServiceRegistryUtil.serviceRegistry()) {
				new MetadataSources( serviceRegistry )
					.addAnnotatedClass( Person.class )
					.buildMetadata();
			}
		} );
		assertThat( annotationException.getMessage() )
			.contains( "Person" )
			.contains( "aliases" );
	}

	@Entity
	@SQLRestriction(
		value = "{r}.deleted = false",
		aliases = @SqlFragmentAlias( alias = "r", entity = Person.class )
	)
	public static class Person {
		@Id
		Long id;
		boolean deleted;
	}
}
