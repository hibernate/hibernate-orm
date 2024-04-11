/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.annotation;

import javax.lang.model.element.ExecutableElement;
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
			ExecutableElement method,
			String methodName, String entity,
			List<String> paramNames, List<String> paramTypes,
			boolean belongsToDao,
			String sessionType,
			String sessionName,
			List<String> fetchProfiles,
			boolean addNonnullAnnotation,
			boolean dataRepository,
			String fullReturnType) {
		super( annotationMetaEntity, method, methodName, entity, belongsToDao, sessionType, sessionName, fetchProfiles,
				paramNames, paramTypes, emptyList(), addNonnullAnnotation, dataRepository, fullReturnType );
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
		return false; // we don't need to convert Query exceptions
	}

	@Override
	public String getAttributeDeclarationString() {
		final StringBuilder declaration = new StringBuilder();
		comment( declaration );
		modifiers( declaration );
		preamble( declaration, paramTypes );
		if ( paramName != null && !isPrimitive(paramType) ) {
			nullCheck( declaration, paramName );
		}
		varOrReturn( declaration );
		if ( fetchProfiles.isEmpty() ) {
			findWithNoFetchProfiles( declaration );
		}
		else {
			findWithFetchProfiles( declaration );
		}
		throwIfNull( declaration );
		convertExceptions( declaration );
		closingBrace( declaration );
		return declaration.toString();
	}

	private void throwIfNull(StringBuilder declaration) {
		if ( isUsingStatelessSession() ) {
			if (dataRepository) {
				declaration
						.append("\t\tif (_result == null) throw new ")
						.append(annotationMetaEntity.importType("jakarta.data.exceptions.EmptyResultException"))
						.append("(\"No '")
						.append(annotationMetaEntity.importType(entity))
						.append("' for given id [\" + ")
						.append(paramName)
						.append(" + \"]\",\n\t\t\t\tnew ")
						.append(annotationMetaEntity.importType("org.hibernate.ObjectNotFoundException"))
						.append("((Object) ")
						.append(paramName)
						.append(", \"")
						.append(entity)
						.append("\"));\n")
						.append("\t\treturn _result;\n");
			}
			else {
				declaration
						.append("\tif (_result == null) throw new ")
						.append(annotationMetaEntity.importType("org.hibernate.ObjectNotFoundException"))
						.append("((Object) ")
						.append(paramName)
						.append(", \"")
						.append(entity)
						.append("\");\n")
						.append("\treturn _result;\n");
			}
		}
	}

	private void varOrReturn(StringBuilder declaration) {
		if (dataRepository) {
			declaration
					.append("\ttry {\n");
		}
		if ( isUsingStatelessSession() ) {
			declaration
					.append("\t\tvar _result = ");
		}
		else {
			declaration
					.append("\t\treturn ");
		}
		declaration
				.append(sessionName);
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
				.append(");\n");
	}

	private void findWithNoFetchProfiles(StringBuilder declaration) {
		if ( isReactiveSessionAccess() ) {
			declaration
					.append(".chain(")
					.append(localSessionName())
					.append(" -> ")
					.append(localSessionName());
		}
		declaration
				.append(isUsingStatelessSession() ? ".get(" : ".find(")
				.append(annotationMetaEntity.importType(entity))
				.append(".class, ")
				.append(paramName);
		if ( isReactiveSessionAccess() ) {
			declaration
					.append(')');
		}
		declaration
				.append(");\n");
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
