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

import static org.hibernate.jpamodelgen.util.Constants.LIST;
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
	List<OrderBy> getOrderBys() {
		return orderBys;
	}

	@Override
	public String getAttributeDeclarationString() {
		final List<String> paramTypes = parameterTypes();
		final StringBuilder declaration = new StringBuilder();
		comment( declaration );
		modifiers( declaration );
		preamble( declaration, paramTypes );
		nullChecks( paramTypes, declaration );
		createCriteriaQuery( declaration );
		where( declaration, paramTypes );
//		orderBy( paramTypes, declaration );
		executeQuery( declaration, paramTypes );
		convertExceptions( declaration );
		closingBrace( declaration );
		return declaration.toString();
	}

	private void preamble(StringBuilder declaration, List<String> paramTypes) {
		declaration
				.append(returnType())
				.append(" ")
				.append(methodName);
		parameters(paramTypes, declaration);
		declaration
				.append(" {\n");
	}

	private void executeQuery(StringBuilder declaration, List<String> paramTypes) {
		declaration
				.append('\n');
		collectOrdering( declaration, paramTypes );
		tryReturn( declaration, paramTypes, containerType );
		castResult( declaration );
		createQuery( declaration );
		handlePageParameters( declaration, paramTypes, containerType );
		boolean unwrapped = specialNeeds( declaration );
		unwrapped = applyOrder( declaration, paramTypes, containerType, unwrapped );
		execute( declaration, paramTypes, unwrapped );
	}

	private void castResult(StringBuilder declaration) {
		if ( containerType == null && !fetchProfiles.isEmpty()
				&& isUsingEntityManager() ) {
			declaration
					.append("(")
					.append(annotationMetaEntity.importType(entity))
					.append(") ");
		}
	}

	private boolean specialNeeds(StringBuilder declaration) {
		boolean unwrapped = !isUsingEntityManager();
		unwrapped = enableFetchProfile( declaration, unwrapped );
		unwrapped = unwrapIfNecessary( declaration, containerType, unwrapped );
		return unwrapped;
	}

	@Override
	void createQuery(StringBuilder declaration) {
		declaration
				.append(sessionName)
				.append(".createQuery(_query)\n");
	}

	private void execute(StringBuilder declaration, List<String> paramTypes, boolean unwrapped) {
		final boolean mustUnwrap =
				containerType != null && containerType.startsWith("org.hibernate");
		executeSelect( declaration, paramTypes, containerType, unwrapped, mustUnwrap );
		if ( dataRepository ) {
			declaration
					.append('\n');
		}
	}

	private void createCriteriaQuery(StringBuilder declaration) {
		declaration
				.append("\tvar _builder = ")
				.append(sessionName)
				.append(isUsingEntityManager()
						? ".getEntityManagerFactory()"
						: ".getFactory()")
				.append(".getCriteriaBuilder();\n")
				.append("\tvar _query = _builder.createQuery(")
				.append(annotationMetaEntity.importType(entity))
				.append(".class);\n")
				.append("\tvar _entity = _query.from(")
				.append(annotationMetaEntity.importType(entity))
				.append(".class);\n");
	}

	private void nullChecks(List<String> paramTypes, StringBuilder declaration) {
		for ( int i = 0; i< paramNames.size(); i++ ) {
			final String paramName = paramNames.get(i);
			final String paramType = paramTypes.get(i);
			if ( !isNullable(i) && !isPrimitive(paramType) ) {
				nullCheck( declaration, paramName );
			}
		}
	}

	private static void nullCheck(StringBuilder declaration, String paramName) {
		declaration
				.append("\tif (")
				.append(paramName)
				.append(" == null) throw new IllegalArgumentException(\"Null ")
				.append(paramName)
				.append("\");\n");
	}

	private void where(StringBuilder declaration, List<String> paramTypes) {
		declaration
				.append("\t_query.where(");
		boolean first = true;
		for ( int i = 0; i < paramNames.size(); i ++ ) {
			final String paramName = paramNames.get(i);
			final String paramType = paramTypes.get(i);
			if ( !isSpecialParam(paramType) ) {
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

	private void parameter(StringBuilder declaration, int i, String paramName, String paramType) {
		declaration
				.append("\n\t\t\t");
		if ( isNullable(i) && !isPrimitive(paramType) ) {
			declaration
					.append(paramName)
					.append("==null")
					.append("\n\t\t\t\t? ")
					.append("_entity");
			path( declaration, paramName );
			declaration
					.append(".isNull()")
					.append("\n\t\t\t\t: ");
		}
		declaration
				.append("_builder.equal(_entity");
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
				&& (containerType == null || LIST.equals(containerType));
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
