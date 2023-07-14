/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.jpamodelgen.util.Constants;

import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * @author Gavin King
 */
public class CriteriaFinderMethod extends AbstractFinderMethod {

	private final @Nullable String containerType;
	private final boolean isId;

	public CriteriaFinderMethod(
			AnnotationMetaEntity annotationMetaEntity,
			String methodName, String entity,
			@Nullable String containerType,
			List<String> paramNames, List<String> paramTypes,
			boolean isId,
			boolean belongsToDao,
			String sessionType,
			String sessionName,
			List<String> fetchProfiles,
			boolean addNonnullAnnotation) {
		super( annotationMetaEntity, methodName, entity, belongsToDao, sessionType, sessionName, fetchProfiles,
				paramNames, paramTypes, addNonnullAnnotation );
		this.containerType = containerType;
		this.isId = isId;
	}

	@Override
	public boolean isId() {
		return isId;
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
		parameters( paramTypes, declaration );
		declaration
				.append(" {");
		if ( isId ) {
			declaration
					.append("\n\tif (")
					.append(paramNames.get(0))
					.append(" == null) throw new IllegalArgumentException(\"Null identifier\");");
		}
		declaration
				.append("\n\tvar builder = ")
				.append(sessionName)
				.append(isUsingEntityManager()
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
		boolean first = true;
		for ( int i = 0; i < paramNames.size(); i ++ ) {
			final String paramName = paramNames.get(i);
			final String paramType = paramTypes.get(i);
			if ( !isSessionParameter(paramType) ) {
				if ( first ) {
					first = false;
				}
				else {
					declaration
							.append(", ");
				}
				declaration
						.append("\n\t\t\t");
				if ( !isId && !isPrimitive(paramType) ) { //TODO: check the entity to see if it's @Basic(optional=false)
					declaration
							.append(paramName)
							.append("==null")
							.append("\n\t\t\t\t? ")
							.append("entity");
					path( declaration, paramName );
					declaration
							.append(".isNull()")
							.append("\n\t\t\t\t: ");
				}
				declaration
						.append("builder.equal(entity");
				path( declaration, paramName );
				declaration
						.append(", ")
						//TODO: only safe if we are binding literals as parameters!!!
						.append(paramName)
						.append(')');
			}
		}
		declaration
				.append("\n\t);")
				.append("\n\treturn ")
				.append(sessionName)
				.append(".createQuery(query)");
		final boolean hasEnabledFetchProfiles = !fetchProfiles.isEmpty();
		final boolean hasNativeReturnType =
				containerType != null && containerType.startsWith("org.hibernate");
		final boolean unwrap =
				( hasEnabledFetchProfiles || hasNativeReturnType )
						&& isUsingEntityManager();
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
			if ( unwrap || hasEnabledFetchProfiles ) {
				declaration.append("\n\t\t\t");
			}
			declaration
					.append(".getResultList()");
		}
		declaration
				.append(";\n}");
		return declaration.toString();
	}

	private void path(StringBuilder declaration, String paramName) {
		final StringTokenizer tokens = new StringTokenizer(paramName, "$");
		String typeName = entity;
		while ( typeName!= null && tokens.hasMoreTokens() ) {
			final String memberName = tokens.nextToken();
			declaration
					.append(".get(")
					.append(annotationMetaEntity.importType(typeName + '_'))
					.append('.')
					.append(memberName)
					.append(')');
			typeName = annotationMetaEntity.getMemberType(typeName, memberName);
		}
	}

	private static boolean isPrimitive(String paramType) {
		return PRIMITIVE_TYPES.contains( paramType );
	}

	private static final Set<String> PRIMITIVE_TYPES =
			Set.of("boolean", "char", "long", "int", "short", "byte", "double", "float");

	private StringBuilder returnType() {
		StringBuilder type = new StringBuilder();
		boolean returnsUni = isReactive()
				&& (containerType == null || Constants.LIST.equals(containerType));
		if ( returnsUni ) {
			type.append(annotationMetaEntity.importType(Constants.UNI)).append('<');
		}
		if ( containerType != null ) {
			type.append(annotationMetaEntity.importType(containerType)).append('<');
		}
		type.append(annotationMetaEntity.importType(entity));
		if ( containerType != null ) {
			type.append('>');
		}
		if ( returnsUni ) {
			type.append('>');
		}
		return type;
	}

}
