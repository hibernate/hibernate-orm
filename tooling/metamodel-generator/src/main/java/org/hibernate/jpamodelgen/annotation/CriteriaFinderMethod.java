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
import java.util.StringTokenizer;

import static org.hibernate.jpamodelgen.util.Constants.JD_SORT;
import static org.hibernate.jpamodelgen.util.TypeUtils.isPrimitive;

/**
 * @author Gavin King
 */
public class CriteriaFinderMethod extends AbstractFinderMethod {

	private final @Nullable String containerType;
	private final List<Boolean> paramNullability;
	private final List<OrderBy> orderBys;

	CriteriaFinderMethod(
			AnnotationMetaEntity annotationMetaEntity,
			String methodName, String entity,
			@Nullable String containerType,
			List<String> paramNames, List<String> paramTypes,
			List<Boolean> paramNullability,
			boolean belongsToDao,
			String sessionType,
			String sessionName,
			List<String> fetchProfiles,
			List<OrderBy> orderBys,
			boolean addNonnullAnnotation,
			boolean dataRepository) {
		super( annotationMetaEntity, methodName, entity, belongsToDao, sessionType, sessionName, fetchProfiles,
				paramNames, paramTypes, addNonnullAnnotation,
				dataRepository );
		this.containerType = containerType;
		this.paramNullability = paramNullability;
		this.orderBys = orderBys;
	}

	@Override
	public boolean isNullable(int index) {
		return paramNullability.get(index);
	}

	@Override
	boolean singleResult() {
		return containerType == null;
	}

	@Override
	public String getAttributeDeclarationString() {
		final List<String> paramTypes = parameterTypes();
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
		nullChecks(paramTypes, declaration);
		createQuery(declaration);
		where(declaration, paramTypes);
		orderBy(paramTypes, declaration);
		executeQuery(declaration, paramTypes);
		convertExceptions( declaration );
		declaration
				.append("\n}");
		return declaration.toString();
	}

	private void executeQuery(StringBuilder declaration, List<String> paramTypes) {
		declaration
				.append('\n');
		if (dataRepository) {
			declaration
					.append("\ttry {\n\t");
		}
		declaration
				.append("\treturn ")
				.append(sessionName)
				.append(".createQuery(query)");
		final boolean hasOrderParameter =
				paramTypes.stream().anyMatch(AbstractQueryMethod::isOrderParam);
		final boolean hasEnabledFetchProfiles = !fetchProfiles.isEmpty();
		final boolean hasNativeReturnType =
				containerType != null && containerType.startsWith("org.hibernate");
		final boolean unwrap =
				( hasOrderParameter || hasEnabledFetchProfiles || hasNativeReturnType )
						&& isUsingEntityManager();
		if ( unwrap ) {
			declaration
					.append("\n\t\t\t.unwrap(")
					.append(annotationMetaEntity.importType(Constants.HIB_SELECTION_QUERY))
					.append(".class)");
		}
		for ( int i = 0; i < paramNames.size(); i ++ ) {
			final String paramName = paramNames.get(i);
			final String paramType = paramTypes.get(i);
			if ( isPageParam(paramType) ) {
				setPage(declaration, paramName, paramType );
			}
			else if ( isOrderParam(paramType) && !isJakartaSortParam(paramType) ) {
				setOrder(declaration, true, paramName, paramType );
			}
		}
		enableFetchProfile(declaration);
		if ( containerType == null) {
			if ( unwrap || hasEnabledFetchProfiles ) {
				declaration.append("\n\t\t\t");
			}
			declaration
					.append(".getSingleResult()");
		}
		else if ( containerType.equals(Constants.LIST) ) {
			if ( unwrap || hasOrderParameter || hasEnabledFetchProfiles ) {
				declaration.append("\n\t\t\t");
			}
			declaration
					.append(".getResultList()");
		}
		declaration
				.append(';');
		if (dataRepository) {
			declaration
					.append('\n');
		}
	}

	private void createQuery(StringBuilder declaration) {
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
				.append(".class);");
	}

	private void nullChecks(List<String> paramTypes, StringBuilder declaration) {
		for ( int i = 0; i< paramNames.size(); i++ ) {
			final String paramName = paramNames.get(i);
			final String paramType = paramTypes.get(i);
			if ( !isNullable(i) && !isPrimitive(paramType) ) {
				declaration
						.append("\n\tif (")
						.append(paramName)
						.append(" == null) throw new IllegalArgumentException(\"Null \" + ")
						.append(paramName)
						.append(");");
			}
		}
	}

	private void orderBy(List<String> paramTypes, StringBuilder declaration) {
		final boolean hasSortParameter =
				paramTypes.stream().anyMatch(CriteriaFinderMethod::isJakartaSortParam);
		if ( !orderBys.isEmpty() || hasSortParameter ) {
			declaration.append("\n\tquery.orderBy(");
		}
		boolean firstOrderBy = true;
		if ( !orderBys.isEmpty() ) {
			for ( OrderBy orderBy : orderBys ) {
				if ( firstOrderBy ) {
					firstOrderBy = false;
				}
				else {
					declaration.append(',');
				}
				orderBy(declaration, orderBy);
			}
		}
		if ( hasSortParameter ) {
			for ( int i = 0; i < paramNames.size(); i ++ ) {
				final String paramName = paramNames.get(i);
				final String paramType = paramTypes.get(i);
				if ( isJakartaSortParam(paramType) ) {
					if ( firstOrderBy ) {
						firstOrderBy = false;
					}
					else {
						declaration.append(',');
					}
					orderBy(declaration, paramName);
				}
			}
		}
		if ( !orderBys.isEmpty() || hasSortParameter ) {
			declaration.append("\n\t);");
		}
	}

	private void where(StringBuilder declaration, List<String> paramTypes) {
		declaration
				.append("\n\tquery.where(");
		boolean first = true;
		for ( int i = 0; i < paramNames.size(); i ++ ) {
			final String paramName = paramNames.get(i);
			final String paramType = paramTypes.get(i);
			if ( !isSessionParameter(paramType)
					&& !isPageParam(paramType)
					&& !isOrderParam(paramType) ) {
				if ( first ) {
					first = false;
				}
				else {
					declaration
							.append(", ");
				}
				parameter(declaration, i, paramName, paramType );
			}
		}
		declaration
				.append("\n\t);");
	}

	private static boolean isJakartaSortParam(String paramType) {
		return paramType.startsWith(JD_SORT);
	}

	private static void orderBy(StringBuilder declaration, OrderBy orderBy) {
		declaration
				.append("\n\t\t")
				.append("builder.")
				.append(orderBy.descending ? "desc" : "asc")
				.append('(');
		if ( orderBy.ignoreCase ) {
			declaration.append("builder.lower(");
		}
		declaration
				.append("entity.get(\"")
				.append(orderBy.fieldName)
				.append("\")");
		if ( orderBy.ignoreCase ) {
			declaration
					.append(')');
		}
		declaration
				.append(')');
	}

	private static void orderBy(StringBuilder declaration, String paramName) {
		// TODO: Sort.ignoreCase()
		declaration
				.append("\n\t\t")
				.append(paramName)
				.append(".isAscending()\n")
				.append("\t\t\t\t? builder.")
				.append("asc(entity.get(")
				.append(paramName)
				.append(".property()))\n")
				.append("\t\t\t\t: builder.")
				.append("desc(entity.get(")
				.append(paramName)
				.append(".property()))");
	}

	private void parameter(StringBuilder declaration, int i, String paramName, String paramType) {
		declaration
				.append("\n\t\t\t");
		if ( isNullable(i) && !isPrimitive(paramType) ) {
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

	static class OrderBy {
		String fieldName;
		boolean descending;
		boolean ignoreCase;
		public OrderBy(String fieldName, boolean descending, boolean ignoreCase) {
			this.fieldName = fieldName;
			this.descending = descending;
			this.ignoreCase = ignoreCase;
		}
	}
}
