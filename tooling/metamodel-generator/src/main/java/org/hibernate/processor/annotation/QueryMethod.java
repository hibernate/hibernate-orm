/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.internal.util.StringHelper;

import javax.lang.model.element.ExecutableElement;
import java.util.List;

import static org.hibernate.processor.util.Constants.QUERY;
import static org.hibernate.processor.util.StringUtil.getUpperUnderscoreCaseFromLowerCamelCase;

/**
 * @author Gavin King
 * @author Yanming Zhou
 */
public class QueryMethod extends AbstractQueryMethod {
	private final String queryString;
	private final @Nullable String returnTypeClass;
	private final @Nullable String containerType;
	private final boolean isUpdate;
	private final boolean isNative;

	QueryMethod(
			AnnotationMetaEntity annotationMetaEntity,
			ExecutableElement method,
			String methodName,
			String queryString,
			@Nullable
			String returnTypeName,
			@Nullable
			String returnTypeClass,
			@Nullable
			String containerType,
			List<String> paramNames,
			List<String> paramTypes,
			boolean isUpdate,
			boolean isNative,
			boolean belongsToDao,
			String sessionType,
			String sessionName,
			List<OrderBy> orderBys,
			boolean addNonnullAnnotation,
			boolean dataRepository,
			String fullReturnType,
			boolean nullable) {
		super( annotationMetaEntity, method,
				methodName,
				paramNames, paramTypes, returnTypeName,
				sessionType, sessionName,
				belongsToDao, orderBys,
				addNonnullAnnotation,
				dataRepository,
				fullReturnType,
				nullable );
		this.queryString = queryString;
		this.returnTypeClass = returnTypeClass;
		this.containerType = containerType;
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
		return containerType == null;
	}

	@Override
	public String getAttributeDeclarationString() {
		final List<String> paramTypes = parameterTypes();
		final StringBuilder declaration = new StringBuilder();
		comment( declaration );
		modifiers( paramTypes, declaration );
		preamble( declaration, paramTypes );
		collectOrdering( declaration, paramTypes );
		chainSession( declaration );
		tryReturn( declaration, paramTypes, containerType );
		castResult( declaration );
		createQuery( declaration );
		setParameters( declaration, paramTypes, "");
		handlePageParameters( declaration, paramTypes, containerType );
		boolean unwrapped = !isUsingEntityManager();
		unwrapped = applyOrder( declaration, paramTypes, containerType, unwrapped );
		execute( declaration, unwrapped );
		convertExceptions( declaration );
		chainSessionEnd( isUpdate, declaration );
		closingBrace( declaration );
		return declaration.toString();
	}

	@Override
	void createQuery(StringBuilder declaration) {
		declaration
				.append(localSessionName())
				.append('.')
				.append(createQueryMethod())
				.append("(")
				.append(getConstantName());
		if ( returnTypeClass != null && !isUpdate ) {
			declaration
					.append(", ")
					.append(annotationMetaEntity.importType(returnTypeClass))
					.append(".class");
		}
		declaration.append(")\n");
	}

	private String createQueryMethod() {
		if ( isNative ) {
			return "createNativeQuery";
		}
		else if ( isUsingEntityManager() || isReactive() || isUnspecializedQueryType(containerType) ) {
			return "createQuery";
		}
		else {
			return isUpdate ? "createMutationQuery" : "createSelectionQuery";
		}
	}

	private void castResult(StringBuilder declaration) {
		if ( isNative && returnTypeName != null && containerType == null
				&& isUsingEntityManager() ) {
			// EntityManager.createNativeQuery() does not return TypedQuery,
			// so we need to cast to the entity type
			declaration
					.append("(")
					.append(fullReturnType)
					.append(") ");
		}
	}

	private void execute(StringBuilder declaration, boolean unwrapped) {
		if ( isUpdate ) {
			declaration
					.append("\t\t\t")
					.append(".executeUpdate()");
			if ( "boolean".equals(returnTypeName) ) {
				declaration
						.append(" > 0");
			}
			declaration
					.append(";\n");
		}
		else {
			final boolean mustUnwrap =
					isHibernateQueryType(containerType)
							|| isNative && returnTypeName != null;
			executeSelect( declaration, paramTypes, containerType, unwrapped, mustUnwrap );
		}
	}

	@Override
	void setParameters(StringBuilder declaration, List<String> paramTypes, String indent) {
		for ( int i = 0; i < paramNames.size(); i++ ) {
			final String paramName = paramNames.get(i);
			final String paramType = paramTypes.get(i);
			if ( !isSpecialParam(paramType) ) {
				final int ordinal = i+1;
				if ( queryString.contains(":" + paramName) ) {
					declaration.append(indent);
					setNamedParameter( declaration, paramName );
				}
				else if ( queryString.contains("?" + ordinal) ) {
					declaration.append(indent);
					setOrdinalParameter( declaration, ordinal, paramName );
				}
			}
		}
	}

	private static void setOrdinalParameter(StringBuilder declaration, int i, String paramName) {
		declaration
				.append("\t\t\t.setParameter(")
				.append(i)
				.append(", ")
				.append(paramName)
				.append(")\n");
	}

	private static void setNamedParameter(StringBuilder declaration, String paramName) {
		declaration
				.append("\t\t\t.setParameter(\"")
				.append(paramName)
				.append("\", ")
				.append(paramName)
				.append(")\n");
	}

//	private String returnType() {
//		final StringBuilder type = new StringBuilder();
//		if ( "[]".equals(containerType) ) {
//			if ( returnTypeName == null ) {
//				throw new AssertionFailure("array return type, but no type name");
//			}
//			type.append(annotationMetaEntity.importType(returnTypeName)).append("[]");
//		}
//		else {
//			final boolean returnsUni = isReactive() && isUnifiableReturnType(containerType);
//			if ( returnsUni ) {
//				type.append(annotationMetaEntity.importType(UNI)).append('<');
//			}
//			if ( containerType != null ) {
//				type.append(annotationMetaEntity.importType(containerType));
//				if ( returnTypeName != null ) {
//					type.append("<").append(annotationMetaEntity.importType(returnTypeName)).append(">");
//				}
//			}
//			else if ( returnTypeName != null )  {
//				type.append(annotationMetaEntity.importType(returnTypeName));
//			}
//			if ( returnsUni ) {
//				type.append('>');
//			}
//		}
//		return type.toString();
//	}

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
		final String stem = getUpperUnderscoreCaseFromLowerCamelCase(methodName);
		if ( paramTypes.isEmpty() ) {
			return stem;
		}
		else {
			return stem + "_"
					+ paramTypes.stream()
							.filter(type -> !isSpecialParam(type))
							.map(type -> type.indexOf('<')>0 ? type.substring(0, type.indexOf('<')) : type)
							.map(StringHelper::unqualify)
							.map(type -> type.replace("[]", "Array"))
							.reduce((x,y) -> x + '_' + y)
							.orElse("");
		}
	}

	public String getTypeDeclaration() {
		return QUERY;
	}
}
