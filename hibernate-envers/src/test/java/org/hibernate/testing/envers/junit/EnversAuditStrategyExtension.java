/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.envers.junit;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.strategy.internal.DefaultAuditStrategy;
import org.hibernate.envers.strategy.spi.AuditStrategy;
import org.hibernate.testing.orm.junit.EntityManagerFactoryExtension;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.junit.jupiter.api.extension.ClassTemplateInvocationContext;
import org.junit.jupiter.api.extension.ClassTemplateInvocationContextProvider;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

public class EnversAuditStrategyExtension implements ClassTemplateInvocationContextProvider {
	@Override
	public boolean supportsClassTemplate(ExtensionContext context) {
		return true;
	}

	@Override
	public Stream<ClassTemplateInvocationContext> provideClassTemplateInvocationContexts(ExtensionContext context) {
		// Try to find @AuditStrategies on the test element (method/class) or the test class
		EnversTest ann = null;
		Optional<AnnotatedElement> elementOpt = context.getElement();
		if ( elementOpt.isPresent() ) {
			ann = elementOpt.get().getAnnotation( EnversTest.class );
		}
		if ( ann == null ) {
			Optional<Class<?>> testClassOpt = context.getTestClass();
			if ( testClassOpt.isPresent() ) {
				ann = testClassOpt.get().getAnnotation( EnversTest.class );
			}
		}

		Class<? extends AuditStrategy>[] strategyClasses = castNonNull( ann ).auditStrategies();

		List<ClassTemplateInvocationContext> contexts = new ArrayList<>();
		for ( Class<? extends AuditStrategy> cls : strategyClasses ) {
			String auditStrategyName = DefaultAuditStrategy.class.equals( cls ) ? null : cls.getName();
			contexts.add( new AuditStrategyInvocationContext( auditStrategyName ) );
		}

		return contexts.stream();
	}

	private record AuditStrategyInvocationContext(String auditStrategy) implements ClassTemplateInvocationContext {
		@Override
		public String getDisplayName(int invocationIndex) {
			String strategyName = auditStrategy != null
					? auditStrategy.substring( auditStrategy.lastIndexOf( '.' ) + 1 )
					: DefaultAuditStrategy.class.getSimpleName();
			return "[" + strategyName + "]";
		}

		@Override
		public List<Extension> getAdditionalExtensions() {
			return List.of( new AuditStrategyConditionExtension( auditStrategy ) );
		}


		@Override
		public void prepareInvocation(ExtensionContext context) {
			final var store = getStore( context );
			// reset the entity manager factory
			final EntityManagerFactoryScope scope = getParameter( store, EntityManagerFactoryExtension.EMF_KEY );
			scope.releaseEntityManagerFactory();
			// integrate settings with envers-specific configuration
			final Map<String, Object> integrationSettings = getParameter( store, EntityManagerFactoryExtension.INTEGRATION_SETTINGS_KEY );
			if ( auditStrategy != null ) {
				integrationSettings.put( EnversSettings.AUDIT_STRATEGY, auditStrategy );
			}
			integrationSettings.putIfAbsent( EnversSettings.USE_REVISION_ENTITY_WITH_NATIVE_ID, "false" );
			// Envers tests expect sequences to not skip values...
			integrationSettings.putIfAbsent( EnversSettings.REVISION_SEQUENCE_NOCACHE, "true" );
		}

		<T> T getParameter(ExtensionContext.Store store, String key) {
			final var o = store.get( key );
			if ( o == null ) {
				throw new IllegalStateException( "Could not find required in extension store: '" + key + "'");
			}
			//noinspection unchecked
			return (T) o;
		}

		ExtensionContext.Store getStore(ExtensionContext context) {
			final var testInstance = context.getRequiredTestInstance();
			return context.getStore( ExtensionContext.Namespace.create( EntityManagerFactoryExtension.class.getName(), testInstance ) );
		}
	}
}
