/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.annotation;

import org.hibernate.jpamodelgen.model.MetaAttribute;
import org.hibernate.jpamodelgen.model.Metamodel;
import org.hibernate.jpamodelgen.util.Constants;

import java.util.List;
import java.util.Set;

import static org.hibernate.jpamodelgen.util.Constants.SESSION_TYPES;

/**
 * @author Gavin King
 */
public abstract class AbstractQueryMethod implements MetaAttribute {
	final Metamodel annotationMetaEntity;
	final String methodName;
	final List<String> paramNames;
	final List<String> paramTypes;
	final String sessionType;
	final String sessionName;
	final boolean belongsToDao;
	final boolean usingEntityManager;
	final boolean reactive;
	final boolean addNonnullAnnotation;

	public AbstractQueryMethod(
			Metamodel annotationMetaEntity,
			String methodName,
			List<String> paramNames, List<String> paramTypes,
			String sessionType,
			String sessionName,
			boolean belongsToDao,
			boolean addNonnullAnnotation) {
		this.annotationMetaEntity = annotationMetaEntity;
		this.methodName = methodName;
		this.paramNames = paramNames;
		this.paramTypes = paramTypes;
		this.sessionType = sessionType;
		this.sessionName = sessionName;
		this.belongsToDao = belongsToDao;
		this.addNonnullAnnotation = addNonnullAnnotation;
		this.usingEntityManager = Constants.ENTITY_MANAGER.equals(sessionType);
		this.reactive = Constants.MUTINY_SESSION.equals(sessionType);
	}

	@Override
	public Metamodel getHostingEntity() {
		return annotationMetaEntity;
	}

	@Override
	public String getMetaType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getPropertyName() {
		return methodName;
	}

	String parameterList() {
		return paramTypes.stream()
				.map(this::strip)
				.map(annotationMetaEntity::importType)
				.reduce((x, y) -> x + ',' + y)
				.orElse("");
	}

	String strip(String type) {
		int index = type.indexOf("<");
		String stripped = index > 0 ? type.substring(0, index) : type;
		return type.endsWith("...") ? stripped + "..." : stripped;
	}

	void sessionParameter(StringBuilder declaration) {
		if ( !belongsToDao ) {
			notNull(declaration);
			declaration
					.append(annotationMetaEntity.importType(sessionType))
					.append(' ')
					.append(sessionName);
		}
	}

	void notNull(StringBuilder declaration) {
		if ( addNonnullAnnotation ) {
			declaration
					.append('@')
					.append(annotationMetaEntity.importType("jakarta.annotation.Nonnull"))
					.append(' ');
		}
	}
}
