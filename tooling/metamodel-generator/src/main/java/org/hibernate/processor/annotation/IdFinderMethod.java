/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.annotation;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.hibernate.processor.util.TypeUtils.isPrimitive;

/**
 * @author Gavin King
 */
public class IdFinderMethod extends AbstractFinderMethod {

	private final String paramName;
	private final String paramType;

	public IdFinderMethod(
			AnnotationMetaEntity annotationMetaEntity,
			String methodName, String entity,
			List<String> paramNames, List<String> paramTypes,
			boolean belongsToDao,
			String sessionType,
			String sessionName,
			List<String> fetchProfiles,
			boolean addNonnullAnnotation,
			boolean dataRepository) {
		super( annotationMetaEntity, methodName, entity, belongsToDao, sessionType, sessionName, fetchProfiles,
				paramNames, paramTypes, emptyList(), addNonnullAnnotation, dataRepository );
		int idParameter = idParameter(paramNames, paramTypes);
		this.paramName = paramNames.get(idParameter);
		this.paramType = paramTypes.get(idParameter);
	}

	private static int idParameter(List<String> paramNames, List<String> paramTypes) {
		for (int i = 0; i < paramNames.size(); i ++ ) {
			if ( !isSessionParameter( paramTypes.get(i) ) ) {
				return i;
			}
		}
		return -1; // should never occur!
	}

	@Override
	boolean isNullable(int index) {
		return false;
	}

	@Override
	boolean singleResult() {
		return true;
	}

	@Override
	public String getAttributeDeclarationString() {
		final StringBuilder declaration = new StringBuilder();
		comment( declaration );
		preamble( declaration );
		if ( paramName != null && !isPrimitive(paramType) ) {
			nullCheck( declaration, paramName );
		}
		tryReturn( declaration );
		if ( fetchProfiles.isEmpty() ) {
			findWithNoFetchProfiles( declaration );
		}
		else {
			findWithFetchProfiles( declaration );
		}
		convertExceptions( declaration );
		closingBrace( declaration );
		return declaration.toString();
	}

	private void findWithFetchProfiles(StringBuilder declaration) {
		unwrapSession( declaration );
		declaration
				.append(".byId(")
				.append(annotationMetaEntity.importType(entity))
				.append(".class)\n");
		enableFetchProfile( declaration, true );
		declaration
				.append("\t\t\t.load(")
				.append(paramName)
				.append(");");
		if (dataRepository) {
			declaration
					.append("\n");
		}
	}

	private void findWithNoFetchProfiles(StringBuilder declaration) {
		declaration
				.append(isUsingStatelessSession() ? ".get(" : ".find(")
				.append(annotationMetaEntity.importType(entity))
				.append(".class, ")
				.append(paramName)
				.append(");");
		if (dataRepository) {
			declaration
					.append("\n");
		}
	}

	private static void nullCheck(StringBuilder declaration, String parameterName) {
		declaration
				.append("\tif (")
				.append(parameterName)
				.append(" == null) throw new IllegalArgumentException(\"Null ")
				.append(parameterName)
				.append("\");\n");
	}
}
