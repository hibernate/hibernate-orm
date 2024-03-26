/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.annotation;

import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.hibernate.processor.annotation.AnnotationMetaEntity.usingReactiveSession;
import static org.hibernate.processor.annotation.AnnotationMetaEntity.usingReactiveSessionAccess;
import static org.hibernate.processor.annotation.AnnotationMetaEntity.usingStatelessSession;
import static org.hibernate.processor.util.Constants.ENTITY_MANAGER;
import static org.hibernate.processor.util.TypeUtils.hasAnnotation;

/**
 * @author Gavin King
 */
public abstract class AbstractAnnotatedMethod implements MetaAttribute {

	final AnnotationMetaEntity annotationMetaEntity;
	private final ExecutableElement method;
	final String sessionType;
	final String sessionName;

	public AbstractAnnotatedMethod(
			AnnotationMetaEntity annotationMetaEntity,
			ExecutableElement method,
			String sessionName, String sessionType) {
		this.annotationMetaEntity = annotationMetaEntity;
		this.method = method;
		this.sessionName = sessionName;
		this.sessionType = sessionType;
	}

	@Override
	public Metamodel getHostingEntity() {
		return annotationMetaEntity;
	}

	boolean isUsingEntityManager() {
		return ENTITY_MANAGER.equals(sessionType);
	}

	boolean isUsingStatelessSession() {
		return usingStatelessSession(sessionType);
	}

	boolean isReactive() {
		return usingReactiveSession(sessionType);
	}

	boolean isReactiveSessionAccess() {
		return usingReactiveSessionAccess(sessionType);
	}

	String localSessionName() {
		return isReactiveSessionAccess() ? "_session" : sessionName;
	}

	@Override
	public List<AnnotationMirror> inheritedAnnotations() {
		if ( annotationMetaEntity.isJakartaDataRepository() ) {
			return method.getAnnotationMirrors().stream()
					.filter(annotationMirror -> hasAnnotation(annotationMirror.getAnnotationType().asElement(),
							"jakarta.interceptor.InterceptorBinding"))
					.collect(toList());
		}
		else {
			return emptyList();
		}
	}
}
