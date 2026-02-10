/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;
import org.hibernate.processor.util.Constants;

import static org.hibernate.processor.util.Constants.ENTITY_MANAGER_FACTORY;
import static org.hibernate.processor.util.Constants.HIB_SESSION_FACTORY;
import static org.hibernate.processor.util.Constants.INJECT;
import static org.hibernate.processor.util.Constants.MUTINY_SESSION;
import static org.hibernate.processor.util.Constants.MUTINY_SESSION_FACTORY;
import static org.hibernate.processor.util.Constants.MUTINY_STATELESS_SESSION;
import static org.hibernate.processor.util.Constants.PERSISTENCE_UNIT;
import static org.hibernate.processor.util.Constants.POST_CONSTRUCT;
import static org.hibernate.processor.util.Constants.PRE_DESTROY;

/**
 * Used by the container to instantiate a Jakarta Data repository.
 * This is a constructor with no parameters, used to instantiate
 * a repository which then uses field injection to obtain its
 * dependencies. By contrast, a {@link RepositoryConstructor} has
 * a parameter which accepts the session as an argument, allowing
 * direct instantiation or constructor injection. This class is
 * only needed because {@code @PersistenceUnit} is incompatible
 * with CDI-style constructor injection.
 *
 * @author Gavin King
 */
public class DefaultConstructor implements MetaAttribute {
	private final AnnotationMetaEntity annotationMetaEntity;
	private final String constructorName;
	private final String methodName;
	private final String sessionTypeName;
	private final String sessionVariableName;
	private final @Nullable String dataStore;
	private final boolean addInjectAnnotation;

	public DefaultConstructor(
			AnnotationMetaEntity annotationMetaEntity,
			String constructorName,
			String methodName,
			String sessionTypeName,
			String sessionVariableName,
			@Nullable String dataStore,
			boolean addInjectAnnotation) {
		this.annotationMetaEntity = annotationMetaEntity;
		this.constructorName = constructorName;
		this.methodName = methodName;
		this.sessionTypeName = sessionTypeName;
		this.sessionVariableName = sessionVariableName;
		this.dataStore = dataStore;
		this.addInjectAnnotation = addInjectAnnotation;
	}

	private boolean isReactive() {
		return annotationMetaEntity.isReactive();
	}

	@Override
	public boolean hasTypedAttribute() {
		return true;
	}

	@Override
	public boolean hasStringAttribute() {
		return false;
	}

	@Override
	public String getAttributeDeclarationString() {
		final StringBuilder declaration = new StringBuilder();
		declaration
				.append('\n');
		if ( annotationMetaEntity.getSuperTypeElement() == null ) {
			declaration
					.append("@")
					.append(annotationMetaEntity.importType(PERSISTENCE_UNIT));
			if ( dataStore != null ) {
				declaration
						.append("(unitName=\"")
						.append(dataStore)
						.append("\")");
			}
			declaration
					.append("\nprivate ")
					.append(annotationMetaEntity.importType(ENTITY_MANAGER_FACTORY))
					.append(" ")
					.append(sessionVariableName)
					.append("Factory;\n\n");
			final String sessionFactoryType = isReactive() ? MUTINY_SESSION_FACTORY : HIB_SESSION_FACTORY;
			declaration.append('@')
					.append(annotationMetaEntity.importType(POST_CONSTRUCT))
					.append("\nprivate void openSession() {")
					.append("\n\t")
					.append(sessionVariableName)
					.append(" = ")
					.append(sessionVariableName)
					.append("Factory.unwrap(")
					.append(annotationMetaEntity.importType( sessionFactoryType ))
					.append(".class).openStatelessSession()");
			if ( MUTINY_SESSION.equals(sessionTypeName)
					|| MUTINY_STATELESS_SESSION.equals(sessionTypeName) ) {
				// this is crap
				declaration
						.append(".await().indefinitely()");
			}
			declaration
					.append(";\n}\n\n");
			// TODO: is it a problem that we never close the session?
			if ( !isReactive() ) {
				declaration.append('@')
						.append(annotationMetaEntity.importType(PRE_DESTROY))
						.append("\nprivate void closeSession() {")
						.append("\n\t")
						.append(sessionVariableName)
						.append(".close();")
						.append("\n}\n\n");
			}
		}
		inject( declaration );
		declaration
				.append(constructorName)
				.append("(")
				.append(") {")
				.append("\n}");
		return declaration.toString();
	}

	private void inject(StringBuilder declaration) {
		if ( addInjectAnnotation ) {
			declaration
					.append('@')
					.append(annotationMetaEntity.importType(INJECT))
					.append('\n');
		}
	}

	@Override
	public String getAttributeNameDeclarationString() {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public String getMetaType() {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public String getPropertyName() {
		return methodName;
	}

	@Override
	public String getTypeDeclaration() {
		return Constants.ENTITY_MANAGER;
	}

	@Override
	public Metamodel getHostingEntity() {
		return annotationMetaEntity;
	}
}
