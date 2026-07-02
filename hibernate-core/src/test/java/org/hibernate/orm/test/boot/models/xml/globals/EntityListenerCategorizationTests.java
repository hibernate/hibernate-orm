/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.globals;

import org.hibernate.boot.models.internal.DomainModelCategorizationCollector;
import org.hibernate.boot.models.internal.GlobalRegistrationsImpl;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.orm.test.boot.models.SourceModelTestHelper;
import org.hibernate.testing.boot.BootstrapContextImpl;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that {@link DomainModelCategorizationCollector} discovers classes
 * with JPA lifecycle callback annotations.
 */
public class EntityListenerCategorizationTests {

	@Test
	void testAnnotatedListenerClassIsDiscovered() {
		final ModelsContext modelsContext = SourceModelTestHelper.createBuildingContext(
				AnnotatedEntityListener.class
		);

		final DomainModelCategorizationCollector collector = new DomainModelCategorizationCollector(
				new GlobalRegistrationsImpl( modelsContext, new BootstrapContextImpl() ),
				modelsContext
		);

		collector.apply( modelsContext.getClassDetailsRegistry().resolveClassDetails( AnnotatedEntityListener.class.getName() ) );

		final Set<String> listenerClassNames = collector.getEntityListenerClasses().stream()
				.map( ClassDetails::getClassName )
				.collect( Collectors.toSet() );

		assertThat( listenerClassNames ).containsExactly( AnnotatedEntityListener.class.getName() );
	}

	@Test
	void testEntityWithCallbackIsDiscovered() {
		final ModelsContext modelsContext = SourceModelTestHelper.createBuildingContext(
				EntityWithCallback.class
		);

		final DomainModelCategorizationCollector collector = new DomainModelCategorizationCollector(
				new GlobalRegistrationsImpl( modelsContext, new BootstrapContextImpl() ),
				modelsContext
		);

		collector.apply( modelsContext.getClassDetailsRegistry().resolveClassDetails( EntityWithCallback.class.getName() ) );

		final Set<String> listenerClassNames = collector.getEntityListenerClasses().stream()
				.map( ClassDetails::getClassName )
				.collect( Collectors.toSet() );

		assertThat( listenerClassNames ).containsExactly( EntityWithCallback.class.getName() );
	}

	@Test
	void testPlainEntityIsNotDiscovered() {
		final ModelsContext modelsContext = SourceModelTestHelper.createBuildingContext(
				PlainEntity.class
		);

		final DomainModelCategorizationCollector collector = new DomainModelCategorizationCollector(
				new GlobalRegistrationsImpl( modelsContext, new BootstrapContextImpl() ),
				modelsContext
		);

		collector.apply( modelsContext.getClassDetailsRegistry().resolveClassDetails( PlainEntity.class.getName() ) );

		assertThat( collector.getEntityListenerClasses() ).isEmpty();
	}
}
