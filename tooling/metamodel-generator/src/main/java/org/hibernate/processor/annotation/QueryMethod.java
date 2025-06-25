/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.internal.util.StringHelper;

import javax.lang.model.element.ExecutableElement;
import java.util.List;

import static org.hibernate.processor.util.Constants.BOOLEAN;
import static org.hibernate.processor.util.Constants.QUERY;
import static org.hibernate.processor.util.Constants.VOID;
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
		return containerType == null && !isUpdate;
	}

	@Override
	public String getAttributeDeclarationString() {
		final List<String> paramTypes = parameterTypes();
		final StringBuilder declaration = new StringBuilder();
		comment( declaration );
		modifiers( declaration, paramTypes );
		preamble( declaration, paramTypes );
		nullChecks( declaration, paramTypes );
		createSpecification( declaration );
		handleRestrictionParameters( declaration, paramTypes );
		collectOrdering( declaration, paramTypes, containerType );
		chainSession( declaration );
		tryReturn( declaration, paramTypes, containerType );
		castResult( declaration );
		createQuery( declaration );
		setParameters( declaration, paramTypes, "");
		handlePageParameters( declaration, paramTypes, containerType );
		execute( declaration, initiallyUnwrapped() );
		convertExceptions( declaration );
		chainSessionEnd( isUpdate, declaration );
		closingBrace( declaration );
		return declaration.toString();
	}

	String specificationType() {
		return isUpdate
				? "org.hibernate.query.specification.MutationSpecification"
				: "org.hibernate.query.specification.SelectionSpecification";
	}

	@Override
	void createQuery(StringBuilder declaration) {
		final boolean specification = isUsingSpecification();
		if ( specification ) {
			if ( isReactive() ) {
				declaration
						.append(localSessionName())
						.append(".createQuery(_spec.buildCriteria(")
						.append(localSessionName())
						.append(".getFactory().getCriteriaBuilder()))\n");
			}
			else {
				declaration
						.append("_spec.createQuery(")
						.append(localSessionName())
						.append(")\n");
			}
		}
		else {
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
	}

	@Override
	void createSpecification(StringBuilder declaration) {
		if ( returnTypeClass != null && isUsingSpecification() ) {
			declaration
					.append( "\tvar _spec = " )
					.append( annotationMetaEntity.importType( specificationType() ) )
					.append( ".create(" )
					.append( annotationMetaEntity.importType( returnTypeClass ) )
					.append( ".class, " )
					.append( getConstantName() )
					.append( ");\n" );
		}
	}

	@Override
	boolean isUsingSpecification() {
		return returnTypeClass != null
			&& ( hasRestriction() || hasOrder() && !isJakartaCursoredPage(containerType) );
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
					.append("\t\t\t.executeUpdate()");
			if ( isReactive() ) {
				if ( VOID.equals(returnTypeName) ) {
					declaration
							.append( "\n\t\t\t.replaceWithVoid()" );
				}
				else if ( BOOLEAN.equals(returnTypeName) ) {
					declaration
							.append( "\n\t\t\t.map(rows -> rows>0)" );
				}
			}
			else {
				if ( "boolean".equals( returnTypeName ) ) {
					declaration
							.append( " > 0" );
				}
			}
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

	private void modifiers(StringBuilder declaration, List<String> paramTypes) {
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

	void nullChecks(StringBuilder declaration, List<String> paramTypes) {
		for ( int i = 0; i<paramNames.size(); i++ ) {
			final String paramType = paramTypes.get( i );
			// we don't do null checks on query parameters
			if ( isSessionParameter( paramType ) || isSpecialParam( paramType) ) {
				nullCheck( declaration, paramNames.get(i) );
			}
		}
	}

	@Override
	public String getAttributeNameDeclarationString() {
		StringBuilder declaration = new StringBuilder( queryString.length() + 200 );
		declaration
				.append("\n/**\n * @see ")
				.append("#");
		signature( declaration );
		declaration
				.append( "\n **/\n" )
				.append( "static final String " )
				.append( getConstantName() )
				.append( " = \"" );
		for ( int i = 0; i < queryString.length(); i++ ) {
			final char c = queryString.charAt( i );
			switch ( c ) {
				case '\r':
					declaration.append( "\\r" );
					break;
				case '\n':
					declaration.append( "\\n" );
					break;
				case '\\':
					declaration.append( "\\\\" );
					break;
				case '"':
					declaration.append( "\\\"" );
					break;
				default:
					declaration.append( c );
					break;
			}
		}
		return declaration.append("\";").toString();
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
