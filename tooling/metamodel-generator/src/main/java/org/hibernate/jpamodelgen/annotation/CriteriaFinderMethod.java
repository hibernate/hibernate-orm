/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.jpamodelgen.model.Metamodel;
import org.hibernate.jpamodelgen.util.Constants;

import java.util.List;

/**
 * @author Gavin King
 */
public class CriteriaFinderMethod extends AbstractFinderMethod {

	private final @Nullable String containerType;

	public CriteriaFinderMethod(
			Metamodel annotationMetaEntity,
			String methodName, String entity,
			@Nullable String containerType,
			List<String> paramNames, List<String> paramTypes,
			boolean belongsToDao,
			String sessionType,
			List<String> fetchProfiles) {
		super( annotationMetaEntity, methodName, entity, belongsToDao, sessionType, fetchProfiles,
				paramNames, paramTypes );
		this.containerType = containerType;
	}

	@Override
	public String getAttributeDeclarationString() {
		final StringBuilder declaration = new StringBuilder();
		comment( declaration );
		modifiers( declaration );
		declaration
				.append(returnType())
				.append(" ")
				.append(methodName);
		parameters( declaration );
		declaration
				.append(" {")
				.append("\n\tvar builder = entityManager")
				.append(usingEntityManager
						? ".getEntityManagerFactory()"
						: ".getFactory()")
				.append(".getCriteriaBuilder();")
				.append("\n\tvar query = builder.createQuery(")
				.append(annotationMetaEntity.importType(entity))
				.append(".class);")
				.append("\n\tvar entity = query.from(")
				.append(annotationMetaEntity.importType(entity))
				.append(".class);")
				.append("\n\tquery.where(");
		for ( int i = 0; i < paramNames.size(); i ++ ) {
			if ( i>0 ) {
				declaration
						.append(", ");
			}
			final String paramName = paramNames.get(i);
			declaration
					.append("\n\t\t\tbuilder.equal(entity.get(")
					.append(annotationMetaEntity.importType(entity + '_'))
					.append('.')
					.append(paramName)
					.append("), ")
					//TODO: only safe if we are binding literals as parameters!!!
					.append(paramName)
					.append(")");
		}
		declaration
				.append("\n\t);")
				.append("\n\treturn entityManager.createQuery(query)");
		final boolean hasEnabledFetchProfiles = !fetchProfiles.isEmpty();
		final boolean hasNativeReturnType = containerType != null && containerType.startsWith("org.hibernate");
		final boolean unwrap =
				( hasEnabledFetchProfiles || hasNativeReturnType )
						&& usingEntityManager;
		if ( unwrap ) {
			declaration
					.append("\n\t\t\t.unwrap(")
					.append(annotationMetaEntity.importType(Constants.HIB_SELECTION_QUERY))
					.append(".class)");
		}
		enableFetchProfile( declaration );
		if ( containerType == null) {
			if ( unwrap || hasEnabledFetchProfiles) {
				declaration.append("\n\t\t\t");
			}
			declaration
					.append(".getSingleResult()");
		}
		else if ( containerType.equals(Constants.LIST) ) {
			if ( unwrap || hasEnabledFetchProfiles) {
				declaration.append("\n\t\t\t");
			}
			declaration
					.append(".getResultList()");
		}
		declaration
				.append(";\n}");
		return declaration.toString();
	}

	private StringBuilder returnType() {
		StringBuilder type = new StringBuilder();
		if ( containerType != null ) {
			type.append(annotationMetaEntity.importType(containerType)).append('<');
		}
		type.append(annotationMetaEntity.importType(entity));
		if ( containerType != null ) {
			type.append('>');
		}
		return type;
	}

}
