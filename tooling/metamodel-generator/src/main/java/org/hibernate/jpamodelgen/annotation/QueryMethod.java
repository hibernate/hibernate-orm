/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpamodelgen.util.Constants;

import java.util.List;

import static org.hibernate.jpamodelgen.util.StringUtil.getUpperUnderscoreCaseFromLowerCamelCase;

/**
 * @author Gavin King
 * @author Yanming Zhou
 */
public class QueryMethod extends AbstractQueryMethod {
	private final String queryString;
	private final @Nullable String containerTypeName;
	private final boolean isUpdate;
	private final boolean isNative;

	public QueryMethod(
			AnnotationMetaEntity annotationMetaEntity,
			String methodName,
			String queryString,
			@Nullable
			String returnTypeName,
			@Nullable
			String containerTypeName,
			List<String> paramNames,
			List<String> paramTypes,
			boolean isUpdate,
			boolean isNative,
			boolean belongsToDao,
			String sessionType,
			String sessionName,
			boolean addNonnullAnnotation,
			boolean dataRepository) {
		super( annotationMetaEntity,
				methodName,
				paramNames, paramTypes, returnTypeName,
				sessionType, sessionName,
				belongsToDao, addNonnullAnnotation,
				dataRepository );
		this.queryString = queryString;
		this.containerTypeName = containerTypeName;
		this.isUpdate = isUpdate;
		this.isNative = isNative;
	}

	@Override
	public boolean hasTypedAttribute() {
		return true;
	}

	@Override
	public boolean hasStringAttribute() {
		return true;
	}

	@Override
	boolean isNullable(int index) {
		return true;
	}

	@Override
	boolean singleResult() {
		return containerTypeName == null;
	}

	@Override
	public String getAttributeDeclarationString() {
		final List<String> paramTypes = parameterTypes();
		final StringBuilder returnType = returnType();
		final StringBuilder declaration = new StringBuilder();
		comment( declaration );
		modifiers( paramTypes, declaration );
		preamble( declaration, returnType, paramTypes );
		tryReturn( declaration );
		castResult( declaration, returnType );
		createQuery( declaration );
		boolean unwrapped = setParameters( paramTypes, declaration );
		execute( declaration, unwrapped );
		convertExceptions( declaration );
		closingBrace( declaration );
		return declaration.toString();
	}

	private void createQuery(StringBuilder declaration) {
		declaration
				.append(sessionName)
				.append(isNative ? ".createNativeQuery" : ".createQuery")
				.append("(")
				.append(getConstantName());
		if ( returnTypeName != null && !isUpdate ) {
			declaration
					.append(", ")
					.append(annotationMetaEntity.importType(returnTypeName))
					.append(".class");
		}
		declaration.append(")");
	}

	private void castResult(StringBuilder declaration, StringBuilder returnType) {
		if ( isNative && returnTypeName != null && containerTypeName == null
				&& isUsingEntityManager() ) {
			// EntityManager.createNativeQuery() does not return TypedQuery,
			// so we need to cast to the entity type
			declaration.append("(")
					.append(returnType)
					.append(") ");
		}
	}

	private void tryReturn(StringBuilder declaration) {
		if (dataRepository) {
			declaration
					.append("\ttry {\n\t");
		}
		declaration
				.append("\t");
		if ( returnTypeName == null || !returnTypeName.equals("void") ) {
			declaration
					.append("return ");
		}
	}

	private void preamble(StringBuilder declaration, StringBuilder returnType, List<String> paramTypes) {
		declaration
				.append(returnType)
				.append(" ")
				.append(methodName);
		parameters( paramTypes, declaration );
		declaration
				.append(" {\n");
	}

	private void execute(StringBuilder declaration, boolean unwrapped) {
		if ( isUpdate ) {
			declaration
					.append("\n\t\t\t.executeUpdate()");
			if ( "boolean".equals(returnTypeName) ) {
				declaration.append(" > 0");
			}
		}
		else if ( containerTypeName == null ) {
			declaration
					.append("\n\t\t\t.getSingleResult()");
		}
		else if ( containerTypeName.equals(Constants.OPTIONAL) ) {
			unwrapQuery( declaration, unwrapped );
			declaration
					.append("\n\t\t\t.uniqueResultOptional()");
		}
		else if ( containerTypeName.equals(Constants.LIST) ) {
			declaration
					.append("\n\t\t\t.getResultList()");
		}
		else {
			if ( isUsingEntityManager() && !unwrapped
					&& ( containerTypeName.startsWith("org.hibernate")
						|| isNative && returnTypeName != null ) ) {
				declaration
						.append("\n\t\t\t.unwrap(")
						.append(annotationMetaEntity.importType(containerTypeName))
						.append(".class)");

			}
		}
		declaration.append(';');
		if (dataRepository) {
			declaration.append('\n');
		}
	}

	private boolean setParameters(List<String> paramTypes, StringBuilder declaration) {
		boolean unwrapped = !isUsingEntityManager();
		for ( int i = 0; i < paramNames.size(); i++ ) {
			final String paramName = paramNames.get(i);
			final String paramType = paramTypes.get(i);
			if ( !isSessionParameter(paramType) ) {
				final int ordinal = i+1;
				if ( queryString.contains(":" + paramName) ) {
					setNamedParameter( declaration, paramName );
				}
				else if ( queryString.contains("?" + ordinal) ) {
					setOrdinalParameter( declaration, ordinal, paramName );
				}
				else if ( isPageParam(paramType) ) {
					setPage( declaration, paramName, paramType );
				}
				else if ( isOrderParam(paramType) ) {
					unwrapped = setOrder( declaration, unwrapped, paramName, paramType );
				}
			}
		}
		return unwrapped;
	}

	private static void setOrdinalParameter(StringBuilder declaration, int i, String paramName) {
		declaration
				.append("\n\t\t\t.setParameter(")
				.append(i)
				.append(", ")
				.append(paramName)
				.append(")");
	}

	private static void setNamedParameter(StringBuilder declaration, String paramName) {
		declaration
				.append("\n\t\t\t.setParameter(\"")
				.append(paramName)
				.append("\", ")
				.append(paramName)
				.append(")");
	}

	private StringBuilder returnType() {
		StringBuilder type = new StringBuilder();
		boolean returnsUni = isReactive()
				&& (containerTypeName == null || Constants.LIST.equals(containerTypeName));
		if ( returnsUni ) {
			type.append(annotationMetaEntity.importType(Constants.UNI)).append('<');
		}
		if ( containerTypeName != null ) {
			type.append(annotationMetaEntity.importType(containerTypeName));
			if ( returnTypeName != null ) {
				type.append("<").append(annotationMetaEntity.importType(returnTypeName)).append(">");
			}
		}
		else if ( returnTypeName != null )  {
			type.append(annotationMetaEntity.importType(returnTypeName));
		}
		if ( returnsUni ) {
			type.append('>');
		}
		return type;
	}

	private void comment(StringBuilder declaration) {
		declaration
				.append("\n/**");
		declaration
				.append("\n * Execute the query {@value #")
				.append(getConstantName())
				.append("}.")
				.append("\n *");
		see( declaration );
		declaration
				.append("\n **/\n");
	}

	private void modifiers(List<String> paramTypes, StringBuilder declaration) {
		boolean hasVarargs = paramTypes.stream().anyMatch(ptype -> ptype.endsWith("..."));
		if ( hasVarargs ) {
			declaration
					.append("@SafeVarargs\n");
		}
		if ( belongsToDao ) {
			declaration
					.append("@Override\npublic ");
			if ( hasVarargs ) {
				declaration
						.append("final ");
			}
		}
		else {
			declaration
					.append("public static ");
		}
	}

	@Override
	public String getAttributeNameDeclarationString() {
		StringBuilder sb = new StringBuilder(queryString.length() + 100)
				.append( "static final String " )
				.append( getConstantName() )
				.append( " = \"" );
		for ( int i = 0; i < queryString.length(); i++ ) {
			final char c = queryString.charAt( i );
			switch ( c ) {
				case '\r':
					sb.append( "\\r" );
					break;
				case '\n':
					sb.append( "\\n" );
					break;
				case '\\':
					sb.append( "\\\\" );
					break;
				case '"':
					sb.append( "\\\"" );
					break;
				default:
					sb.append( c );
					break;
			}
		}
		return sb.append("\";").toString();
	}

	private String getConstantName() {
		String stem = getUpperUnderscoreCaseFromLowerCamelCase(methodName);
		if ( paramTypes.isEmpty() ) {
			return stem;
		}
		else {
			return stem + "_"
					+ paramTypes.stream()
							.filter(type -> !isPageParam(type) && !isOrderParam(type)
									&& !isSessionParameter(type))
							.map(StringHelper::unqualify)
							.reduce((x,y) -> x + '_' + y)
							.orElse("");
		}
	}

	public String getTypeDeclaration() {
		return Constants.QUERY;
	}
}
