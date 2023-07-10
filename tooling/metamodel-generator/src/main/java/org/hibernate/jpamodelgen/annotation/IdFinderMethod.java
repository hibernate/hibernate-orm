/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.annotation;

import org.hibernate.jpamodelgen.model.Metamodel;
import org.hibernate.jpamodelgen.util.Constants;

import java.util.List;

/**
 * @author Gavin King
 */
public class IdFinderMethod extends AbstractFinderMethod {

	private final String paramName;
	private final boolean usingStatelessSession;

	public IdFinderMethod(
			Metamodel annotationMetaEntity,
			String methodName, String entity,
			String paramName, String paramType,
			boolean belongsToDao,
			String sessionType,
			List<String> fetchProfiles,
			boolean addNonnullAnnotation) {
		super( annotationMetaEntity, methodName, entity, belongsToDao, sessionType, fetchProfiles,
				List.of(paramName), List.of(paramType), addNonnullAnnotation );
		this.paramName = paramName;
		usingStatelessSession = Constants.HIB_STATELESS_SESSION.equals(sessionType);
	}

	@Override
	boolean isId() {
		return true;
	}

	@Override
	public String getAttributeDeclarationString() {
		final StringBuilder declaration = new StringBuilder();
		comment( declaration );
		preamble( declaration );
		if ( usingStatelessSession || usingEntityManager && fetchProfiles.isEmpty() ) {
			declaration
					.append(usingStatelessSession ? ".get(" : ".find(")
					.append(annotationMetaEntity.importType(entity))
					.append(".class, ")
					.append(paramName)
					.append(");")
					.append("\n}");
		}
		else {
			unwrapSession( declaration );
			declaration
					.append(".byId(")
					.append(annotationMetaEntity.importType(entity))
					.append(".class)");
			enableFetchProfile( declaration );
			declaration
					.append("\n\t\t\t.load(")
					.append(paramName)
					.append(");\n}");

		}
		return declaration.toString();
	}
}
