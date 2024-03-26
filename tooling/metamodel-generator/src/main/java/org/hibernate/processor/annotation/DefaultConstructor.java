/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;
import org.hibernate.processor.util.Constants;

import static org.hibernate.processor.annotation.AnnotationMetaEntity.usingReactiveSession;
import static org.hibernate.processor.util.Constants.ENTITY_MANAGER_FACTORY;
import static org.hibernate.processor.util.Constants.HIB_SESSION_FACTORY;
import static org.hibernate.processor.util.Constants.MUTINY_SESSION;
import static org.hibernate.processor.util.Constants.MUTINY_SESSION_FACTORY;
import static org.hibernate.processor.util.Constants.MUTINY_STATELESS_SESSION;

/**
 * Used by the container to instantiate a Jakarta Data repository.
 *
 * @author Gavin King
 */
public class DefaultConstructor implements MetaAttribute {
	private final Metamodel annotationMetaEntity;
	private final String constructorName;
	private final String methodName;
	private final String sessionTypeName;
	private final String sessionVariableName;
	private final @Nullable String dataStore;
	private final boolean addInjectAnnotation;

	public DefaultConstructor(
			Metamodel annotationMetaEntity,
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
		return usingReactiveSession(sessionTypeName);
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
		if ( annotationMetaEntity.getSupertypeName() == null ) {
			declaration
					.append("@")
					.append(annotationMetaEntity.importType("jakarta.persistence.PersistenceUnit"));
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
			declaration.append('@')
					.append(annotationMetaEntity.importType("jakarta.annotation.PostConstruct"))
					.append("\nprivate void openSession() {")
					.append("\n\t")
					.append(sessionVariableName)
					.append(" = ")
					.append(sessionVariableName)
					.append("Factory.unwrap(")
					.append(annotationMetaEntity.importType(isReactive() ? MUTINY_SESSION_FACTORY : HIB_SESSION_FACTORY))
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
						.append(annotationMetaEntity.importType("jakarta.annotation.PreDestroy"))
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
					.append(annotationMetaEntity.importType("jakarta.inject.Inject"))
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
