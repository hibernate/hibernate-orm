/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.annotation;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpamodelgen.util.Constants;

import java.util.List;
import java.util.Locale;

import static org.hibernate.jpamodelgen.util.StringUtil.getUpperUnderscoreCaseFromLowerCamelCase;

/**
 * @author Gavin King
 */
public abstract class AbstractFinderMethod extends AbstractQueryMethod  {
	final String entity;
	final List<String> fetchProfiles;
	final boolean convertToDataExceptions;

	public AbstractFinderMethod(
			AnnotationMetaEntity annotationMetaEntity,
			String methodName,
			String entity,
			boolean belongsToDao,
			String sessionType,
			String sessionName,
			List<String> fetchProfiles,
			List<String> paramNames,
			List<String> paramTypes,
			boolean addNonnullAnnotation,
			boolean convertToDataExceptions) {
		super( annotationMetaEntity,
				methodName,
				paramNames, paramTypes, entity,
				sessionType, sessionName,
				belongsToDao, addNonnullAnnotation );
		this.entity = entity;
		this.fetchProfiles = fetchProfiles;
		this.convertToDataExceptions = convertToDataExceptions;
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
	public String getTypeDeclaration() {
		return entity;
	}

	@Override
	public String getAttributeNameDeclarationString() {
		return new StringBuilder()
				.append("public static final String ")
				.append(constantName())
				.append(" = \"!")
				.append(annotationMetaEntity.getQualifiedName())
				.append('.')
				.append(methodName)
				.append("(")
				.append(parameterList())
				.append(")")
				.append("\";")
				.toString();
	}

	String constantName() {
		return getUpperUnderscoreCaseFromLowerCamelCase(methodName) + "_BY_"
				+ paramNames.stream()
				.map(StringHelper::unqualify)
				.map(name -> name.toUpperCase(Locale.ROOT))
				.reduce((x,y) -> x + "_AND_" + y)
				.orElse("");
	}

	void comment(StringBuilder declaration) {
		declaration
				.append("\n/**")
				.append("\n * Find ")
				.append("{@link ")
				.append(annotationMetaEntity.importType(entity))
				.append("} by ");
		long paramCount = paramTypes.stream()
				.filter(type -> !isOrderParam(type) && !isPageParam(type)
						&& !isSessionParameter(type))
				.count();
		int count = 0;
		for (int i = 0; i < paramTypes.size(); i++) {
			String type = paramTypes.get(i);
			if ( !isPageParam(type) && !isOrderParam(type)
					&& !isSessionParameter(type) ) {
				if ( count>0 ) {
					if ( count + 1 == paramCount) {
						declaration
								.append(paramCount>2 ? ", and " : " and "); //Oxford comma
					}
					else {
						declaration
								.append(", ");
					}
				}
				count++;
				final String path = paramNames.get(i)
						.replace('$', '.');
				declaration
						.append("{@link ")
						.append(annotationMetaEntity.importType(entity))
						.append('#')
						.append(qualifier(path))
						.append(' ')
						.append(path)
						.append("}");
			}
		}
		declaration
				.append('.')
				.append("\n *");
		see( declaration );
//		declaration
//				.append("\n *");
//		for (String param : paramNames) {
//			declaration
//					.append("\n * @see ")
//					.append(annotationMetaEntity.importType(entity))
//					.append('#')
//					.append(param);
//		}
		declaration
				.append("\n **/\n");
	}

	String qualifier(String name) {
		final int index = name.indexOf('$');
		return index > 0 ? name.substring(0, index) : name;
	}

	void unwrapSession(StringBuilder declaration) {
		if ( isUsingEntityManager() ) {
			declaration
					.append(".unwrap(")
					.append(annotationMetaEntity.importType(Constants.HIB_SESSION))
					.append(".class)\n\t\t\t");
		}
	}

	void enableFetchProfile(StringBuilder declaration) {
//		if ( !usingEntityManager ) {
//			declaration
//					.append("\n\t\t\t.enableFetchProfile(")
//					.append(constantName())
//					.append(")");
//		}
		for ( String profile : fetchProfiles ) {
			declaration
					.append("\n\t\t\t.enableFetchProfile(")
					.append(profile)
					.append(")");
		}
	}

	void preamble(StringBuilder declaration) {
		modifiers( declaration );
		entityType( declaration );
		declaration
				.append(" ")
				.append(methodName);
		parameters( paramTypes, declaration ) ;
		declaration
				.append(" {\n");
		if (convertToDataExceptions) {
			declaration
					.append("\ttry {\n\t");
		}
		declaration
				.append("\treturn ")
				.append(sessionName);
	}

	void convertExceptions(StringBuilder declaration) {
		if (convertToDataExceptions) {
			declaration
					.append("\t}\n")
					.append("\tcatch (")
					.append(annotationMetaEntity.importType("jakarta.persistence.NoResultException"))
					.append(" exception) {\n")
					.append("\t\tthrow new ")
					.append(annotationMetaEntity.importType("jakarta.data.exceptions.EmptyResultException"))
					.append("(exception);\n")
					.append("\t}\n")
					.append("\tcatch (")
					.append(annotationMetaEntity.importType("jakarta.persistence.NonUniqueResultException"))
					.append(" exception) {\n")
					.append("\t\tthrow new ")
					.append(annotationMetaEntity.importType("jakarta.data.exceptions.NonUniqueResultException"))
					.append("(exception);\n")
					.append("\t}\n");
		}
	}

	private void entityType(StringBuilder declaration) {
		if ( isReactive() ) {
			declaration
					.append(annotationMetaEntity.importType(Constants.UNI))
					.append('<');
		}
		declaration
				.append(annotationMetaEntity.importType(entity));
		if ( isReactive() ) {
			declaration
					.append('>');
		}
	}

	void modifiers(StringBuilder declaration) {
		declaration
				.append(belongsToDao ? "@Override\npublic " : "public static ");
	}

	@Override
	String getSortableEntityClass() {
		return entity;
	}
}
