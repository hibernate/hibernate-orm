/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.cfg.StateManagementSettings;

import org.junit.jupiter.api.extension.ClassTemplateInvocationContext;
import org.junit.jupiter.api.extension.ClassTemplateInvocationContextProvider;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * JUnit extension that provides class template invocation contexts
 * for each audit strategy specified by {@link AuditedTest}.
 * <p>
 * For each strategy, the extension releases the existing session factory,
 * domain model, and service registry, then injects the
 * {@value StateManagementSettings#AUDIT_STRATEGY} setting so that a
 * fresh bootstrap picks up the correct strategy.
 */
public class AuditStrategyExtension implements ClassTemplateInvocationContextProvider {
	@Override
	public boolean supportsClassTemplate(ExtensionContext context) {
		return true;
	}

	@Override
	public Stream<ClassTemplateInvocationContext> provideClassTemplateInvocationContexts(ExtensionContext context) {
		AuditedTest ann = null;
		final Optional<AnnotatedElement> elementOpt = context.getElement();
		if ( elementOpt.isPresent() ) {
			ann = elementOpt.get().getAnnotation( AuditedTest.class );
		}
		if ( ann == null ) {
			final Optional<Class<?>> testClassOpt = context.getTestClass();
			if ( testClassOpt.isPresent() ) {
				ann = testClassOpt.get().getAnnotation( AuditedTest.class );
			}
		}

		final String[] strategies = castNonNull( ann ).strategies();
		final List<ClassTemplateInvocationContext> contexts = new ArrayList<>( strategies.length );
		for ( String strategy : strategies ) {
			contexts.add( new AuditStrategyInvocationContext( strategy ) );
		}
		return contexts.stream();
	}

	private record AuditStrategyInvocationContext(String strategy) implements ClassTemplateInvocationContext {
		@Override
		public String getDisplayName(int invocationIndex) {
			return "[" + strategy + "]";
		}

		@Override
		public List<Extension> getAdditionalExtensions() {
			return List.of();
		}

		@Override
		public void prepareInvocation(ExtensionContext context) {
			final var testInstance = context.getRequiredTestInstance();
			// Release existing SF/DomainModel/ServiceRegistry so they are rebuilt
			final var sfScope = SessionFactoryExtension.findSessionFactoryScope( testInstance, context );
			sfScope.releaseSessionFactory();
			final var domainModelScope = DomainModelExtension.findDomainModelScope( testInstance, context );
			domainModelScope.releaseModel();
			final var registryScope = ServiceRegistryExtension.findServiceRegistryScope( testInstance, context );
			registryScope.releaseRegistry();
			// Inject the audit strategy setting
			registryScope.getAdditionalSettings()
					.put( StateManagementSettings.AUDIT_STRATEGY, strategy );
		}
	}
}
