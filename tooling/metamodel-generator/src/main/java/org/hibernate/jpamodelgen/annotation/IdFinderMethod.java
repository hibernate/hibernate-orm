/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.annotation;

import java.util.List;

/**
 * @author Gavin King
 */
public class IdFinderMethod extends AbstractFinderMethod {

	private final String paramName;

	public IdFinderMethod(
			AnnotationMetaEntity annotationMetaEntity,
			String methodName, String entity,
			List<String> paramNames, List<String> paramTypes,
			boolean belongsToDao,
			String sessionType,
			String sessionName,
			List<String> fetchProfiles,
			boolean addNonnullAnnotation) {
		super( annotationMetaEntity, methodName, entity, belongsToDao, sessionType, sessionName, fetchProfiles,
				paramNames, paramTypes, addNonnullAnnotation );
		this.paramName = idParameterName( paramNames, paramTypes );
	}

	private static String idParameterName(List<String> paramNames, List<String> paramTypes) {
		for (int i = 0; i < paramNames.size(); i ++ ) {
			if ( !isSessionParameter( paramTypes.get(i) ) ) {
				return paramNames.get(i);
			}
		}
		return ""; // should never occur!
	}

	@Override
	boolean isNullable(int index) {
		return false;
	}

	@Override
	public String getAttributeDeclarationString() {
		final StringBuilder declaration = new StringBuilder();
		comment( declaration );
		preamble( declaration );
		if ( fetchProfiles.isEmpty() ) {
			findWithNoFetchProfiles( declaration );
		}
		else {
			findWithFetchProfiles( declaration );
		}
		return declaration.toString();
	}

	private void findWithFetchProfiles(StringBuilder declaration) {
		unwrapSession( declaration );
		declaration
				.append(".byId(")
				.append(annotationMetaEntity.importType(entity))
				.append(".class)");
		enableFetchProfile(declaration);
		declaration
				.append("\n\t\t\t.load(")
				.append(paramName)
				.append(");\n}");
	}

	private void findWithNoFetchProfiles(StringBuilder declaration) {
		declaration
				.append(isUsingStatelessSession() ? ".get(" : ".find(")
				.append(annotationMetaEntity.importType(entity))
				.append(".class, ")
				.append(paramName)
				.append(");")
				.append("\n}");
	}
}
