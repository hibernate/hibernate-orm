/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.annotation;

import org.hibernate.jpamodelgen.model.Metamodel;

import java.util.List;

/**
 * @author Gavin King
 */
public class NaturalIdFinderMethod extends AbstractFinderMethod {

	public NaturalIdFinderMethod(
			Metamodel annotationMetaEntity,
			String methodName, String entity,
			List<String> paramNames, List<String> paramTypes,
			boolean belongsToDao,
			String sessionType,
			List<String> fetchProfiles) {
		super( annotationMetaEntity, methodName, entity, belongsToDao, sessionType, fetchProfiles,
				paramNames, paramTypes );
	}

	@Override
	public String getAttributeDeclarationString() {
		final StringBuilder declaration = new StringBuilder();
		comment( declaration );
		preamble( declaration );
		unwrapSession( declaration );
		declaration
				.append(".byNaturalId(")
				.append(annotationMetaEntity.importType(entity))
				.append(".class)");
		enableFetchProfile( declaration );
		for ( int i = 0; i < paramNames.size(); i ++ ) {
			final String paramName = paramNames.get(i);
			declaration
					.append("\n\t\t\t.using(")
					.append(annotationMetaEntity.importType(entity + '_'))
					.append('.')
					.append(paramName)
					.append(", ")
					.append(paramName)
					.append(")");
		}
		declaration
				.append("\n\t\t\t.load();\n}");
		return declaration.toString();
	}

}
