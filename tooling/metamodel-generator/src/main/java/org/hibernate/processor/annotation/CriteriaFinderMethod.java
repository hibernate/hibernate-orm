/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.AssertionFailure;
import org.hibernate.processor.util.Constants;

import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.hibernate.processor.util.Constants.LIST;
import static org.hibernate.processor.util.TypeUtils.isPrimitive;

/**
 * @author Gavin King
 */
public class CriteriaFinderMethod extends AbstractFinderMethod {

	private final @Nullable String containerType;
	private final List<Boolean> paramNullability;
	private final List<Boolean> multivalued;
	private final List<Boolean> paramPatterns;

	CriteriaFinderMethod(
			AnnotationMetaEntity annotationMetaEntity,
			String methodName, String entity,
			@Nullable String containerType,
			List<String> paramNames, List<String> paramTypes,
			List<Boolean> paramNullability,
			List<Boolean> multivalued,
			List<Boolean> paramPatterns,
			boolean belongsToDao,
			String sessionType,
			String sessionName,
			List<String> fetchProfiles,
			List<OrderBy> orderBys,
			boolean addNonnullAnnotation,
			boolean dataRepository) {
		super( annotationMetaEntity, methodName, entity, belongsToDao, sessionType, sessionName, fetchProfiles,
				paramNames, paramTypes, orderBys, addNonnullAnnotation, dataRepository );
		this.containerType = containerType;
		this.paramNullability = paramNullability;
		this.multivalued = multivalued;
		this.paramPatterns = paramPatterns;
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
		preamble( declaration, returnType(), paramTypes );
		chainSession( declaration );
		nullChecks( paramTypes, declaration );
		createCriteriaQuery( declaration );
		where( declaration, paramTypes );
//		orderBy( paramTypes, declaration );
		executeQuery( declaration, paramTypes );
		convertExceptions( declaration );
		chainSessionEnd( false, declaration );
		closingBrace( declaration );
		return declaration.toString();
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
				.append(localSessionName())
				.append(".createQuery(_query)\n");
	}

	private void execute(StringBuilder declaration, List<String> paramTypes, boolean unwrapped) {
		final boolean mustUnwrap =
				containerType != null && containerType.startsWith("org.hibernate");
		executeSelect( declaration, paramTypes, containerType, unwrapped, mustUnwrap );
	}

	private void createCriteriaQuery(StringBuilder declaration) {
		declaration
				.append("\tvar _builder = ")
				.append(localSessionName())
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
				.append(paramName.replace('.', '$'))
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
				condition(declaration, i, paramName, paramType );
			}
		}
		declaration
				.append("\n\t);");
	}

	private void condition(StringBuilder declaration, int i, String paramName, String paramType) {
		declaration
				.append("\n\t\t\t");
		final String parameterName = paramName.replace('.', '$');
		if ( isNullable(i) && !isPrimitive(paramType) ) {
			declaration
					.append(parameterName)
					.append("==null")
					.append("\n\t\t\t\t? ")
					.append("_entity");
			path( declaration, paramName );
			declaration
					.append(".isNull()")
					.append("\n\t\t\t\t: ");
		}
		if ( multivalued.get(i) ) {
			declaration
					.append("_entity");
			path( declaration, paramName );
			declaration
					.append(".in(");
			if ( paramType.endsWith("[]") ) {
				declaration
						.append("(Object[]) ")
						//TODO: only safe if we are binding literals as parameters!!!
						.append(parameterName);

			}
			else {
				declaration
						.append(annotationMetaEntity.staticImport(StreamSupport.class.getName(), "stream"))
						.append('(')
						//TODO: only safe if we are binding literals as parameters!!!
						.append(parameterName)
						.append(".spliterator(), false).collect(") // ugh, very ugly!
						.append(annotationMetaEntity.staticImport(Collectors.class.getName(), "toList"))
						.append("())");
			}
			declaration
					.append(")");
		}
		else {
			//TODO: change to use Expression.equalTo() in JPA 3.2
			declaration
					.append("_builder.")
					.append(paramPatterns.get(i) ? "like" : "equal")
					.append("(_entity");
			path( declaration, paramName );
			declaration
					.append(", ")
					//TODO: only safe if we are binding literals as parameters!!!
					.append(parameterName)
					.append(')');
		}
	}

	private void path(StringBuilder declaration, String paramName) {
		final StringTokenizer tokens = new StringTokenizer(paramName, ".");
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
		final StringBuilder type = new StringBuilder();
		if ( "[]".equals(containerType) ) {
			if ( returnTypeName == null ) {
				throw new AssertionFailure("array return type, but no type name");
			}
			type.append(annotationMetaEntity.importType(returnTypeName)).append("[]");
		}
		else {
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
		}
		return type;
	}
}
