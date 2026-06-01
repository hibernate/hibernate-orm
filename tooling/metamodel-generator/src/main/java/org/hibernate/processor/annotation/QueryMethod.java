/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import jakarta.annotation.Nullable;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.sql.internal.ParameterParser;
import org.hibernate.query.sql.spi.ParameterRecognizer;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.StringTokenizer;

import static org.hibernate.metamodel.mapping.EntityIdentifierMapping.ID_ROLE_NAME;
import static org.hibernate.processor.annotation.QueryOptionsSupport.stringLiteral;
import static org.hibernate.processor.util.Constants.BOOLEAN;
import static org.hibernate.processor.util.Constants.JD_PAGE;
import static org.hibernate.processor.util.Constants.LIST;
import static org.hibernate.processor.util.Constants.OPTIONAL;
import static org.hibernate.processor.util.Constants.QUERY_OPTIONS;
import static org.hibernate.processor.util.Constants.QUERY;
import static org.hibernate.processor.util.Constants.STATIC_STATEMENT_REFERENCE;
import static org.hibernate.processor.util.Constants.STATIC_TYPED_QUERY_REFERENCE;
import static org.hibernate.processor.util.Constants.STREAM;
import static org.hibernate.processor.util.Constants.VOID;
import static org.hibernate.processor.util.NullnessUtil.castNonNull;
import static org.hibernate.processor.util.StringUtil.getUpperUnderscoreCaseFromLowerCamelCase;
import static org.hibernate.processor.util.TypeUtils.getAnnotationMirror;
import static org.hibernate.processor.util.TypeUtils.getGeneratedClassFullyQualifiedName;

/**
 * @author Gavin King
 * @author Yanming Zhou
 */
public class QueryMethod extends AbstractQueryMethod {
	private final String queryString;
	private final @Nullable String namedQueryName;
	private final @Nullable String returnTypeClass;
	private final @Nullable String containerType;
	private final @Nullable String resultSetMapping;
	private final boolean isUpdate;
	private final boolean isNative;
	private final boolean generatedQueryReferenceMethod;
	private final @Nullable ResultSelection querySelection;
	private final @Nullable String entity;

	QueryMethod(
			AnnotationMetaEntity annotationMetaEntity,
			ExecutableElement method,
			String methodName,
			String queryString,
			@Nullable String namedQueryName,
			@Nullable
			String returnTypeName,
			@Nullable
			String returnTypeClass,
			@Nullable
			String containerType,
			@Nullable
			String resultSetMapping,
			List<String> paramNames,
			List<String> paramTypes,
			boolean isUpdate,
			boolean isNative,
			boolean generatedQueryReferenceMethod,
			boolean belongsToDao,
			String sessionType,
			String sessionName,
			List<OrderBy> orderBys,
			boolean addNonnullAnnotation,
			boolean dataRepository,
			String fullReturnType,
			boolean nullable,
			@Nullable ResultSelection querySelection,
			@Nullable String entity) {
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
		this.namedQueryName = namedQueryName;
		this.returnTypeClass = returnTypeClass;
		this.containerType = containerType;
		this.resultSetMapping = resultSetMapping;
		this.isUpdate = isUpdate;
		this.isNative = isNative;
		this.generatedQueryReferenceMethod = generatedQueryReferenceMethod;
		this.querySelection = querySelection;
		this.entity = entity;
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
	@Nullable String containerType() {
		return containerType;
	}

	@Override
	boolean singleResult() {
		return containerType == null && !isUpdate;
	}

	private boolean usesProjectionSpecification() {
		return querySelection != null
			&& querySelection.recordProjection()
			&& entity != null
			&& !isUpdate
			&& !isNative
			&& !isReactive()
			&& isProjectionSpecificationContainer();
	}

	private boolean isProjectionSpecificationContainer() {
		return containerType == null
			|| "[]".equals( containerType )
			|| LIST.equals( containerType )
			|| OPTIONAL.equals( containerType )
			|| STREAM.equals( containerType )
			|| JD_PAGE.equals( containerType );
	}

	@Override
	public String getAttributeDeclarationString() {
		if ( usesProjectionSpecification() ) {
			return getProjectionSpecificationAttributeDeclarationString();
		}
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
		inTry( declaration );
		createQuery( declaration, true );
		if ( !bindsParametersFromReference() ) {
			setParameters( declaration, paramTypes );
		}
		if ( !useNamedQuery() && !bindsParametersFromReference() ) {
			QueryOptionsSupport.setQueryOptions( this, declaration, isUpdate, isNative );
		}
		declaration.append( ";\n" );
		results( declaration, paramTypes, containerType );
		castResult( declaration );
		select( declaration );
		setFirstResultLimit( declaration );
		handlePageParameters( declaration, paramTypes, containerType );
		execute( declaration, initiallyUnwrapped() );
		convertExceptions( declaration );
		chainSessionEnd( isUpdate, declaration );
		closingBrace( declaration );
		return declaration.toString();
	}

	private String getProjectionSpecificationAttributeDeclarationString() {
		final List<String> paramTypes = parameterTypes();
		final StringBuilder declaration = new StringBuilder();
		comment( declaration );
		modifiers( declaration, paramTypes );
		preamble( declaration, paramTypes );
		chainSession( declaration );
		nullChecks( declaration, paramTypes );
		// var _spec = SelectionSpecification.create(EntityType.class, QUERY_CONSTANT);
		declaration
				.append( "\tvar _spec = " )
				.append( annotationMetaEntity.importType( specificationType() ) )
				.append( ".create(" )
				.append( annotationMetaEntity.importType( entity ) )
				.append( ".class, " )
				.append( getConstantName() )
				.append( ");\n" );
		handleRestrictionParameters( declaration, paramTypes );
		collectOrdering( declaration, paramTypes, containerType );
		createProjectionSpecification( declaration );
		inTry( declaration );
		// var _select = _projection.createQuery(session)
		declaration.append( "\t\tvar _select = _projection.createQuery(" );
		localSession( declaration );
		declaration.append( ")" );
		setParameters( declaration, paramTypes );
		QueryOptionsSupport.setQueryOptions( this, declaration, false, false );
		declaration.append( ";\n" );
		executeProjectionQuery( declaration, paramTypes );
		convertExceptions( declaration );
		chainSessionEnd( false, declaration );
		closingBrace( declaration );
		return declaration.toString();
	}

	private void createProjectionSpecification(StringBuilder declaration) {
		declaration
				.append( "\tvar _projection = " )
				.append( annotationMetaEntity.importType( "org.hibernate.query.specification.ProjectionSpecification" ) )
				.append( ".create(_spec);\n" );
		for ( int i = 0; i < querySelection.paths().size(); i++ ) {
			declaration
					.append( "\tvar " )
					.append( projectionVariableName( i ) )
					.append( " = _projection.select(" );
			projectionSelection( declaration, querySelection.paths().get( i ) );
			declaration
					.append( ");\n" );
		}
	}

	private void projectionSelection(StringBuilder declaration, String path) {
		final StringTokenizer tokens = new StringTokenizer( path, "." );
		if ( tokens.countTokens() == 1 && !ID_ROLE_NAME.equals( path ) ) {
			metamodelAttribute( declaration, entity, path );
		}
		else {
			declaration
					.append( annotationMetaEntity.importType( "org.hibernate.query.restriction.Path" ) )
					.append( ".from(" )
					.append( annotationMetaEntity.importType( entity ) )
					.append( ".class)" );
			String typeName = entity;
			while ( typeName != null && tokens.hasMoreTokens() ) {
				final String memberName = tokens.nextToken();
				declaration.append( ".to(" );
				if ( ID_ROLE_NAME.equals( memberName ) ) {
					declaration
							.append( '"' )
							.append( memberName )
							.append( "\", " )
							.append( annotationMetaEntity.importType( "java.lang.Object" ) )
							.append( ".class" );
				}
				else {
					metamodelAttribute( declaration, typeName, memberName );
				}
				declaration.append( ")" );
				typeName = annotationMetaEntity.getMemberType( typeName, memberName );
			}
		}
	}

	private void metamodelAttribute(StringBuilder declaration, String typeName, String memberName) {
		final TypeElement typeElement =
				annotationMetaEntity.getContext().getElementUtils()
						.getTypeElement( typeName );
		declaration
				.append( annotationMetaEntity.importType(
						getGeneratedClassFullyQualifiedName( typeElement, false ) ) )
				.append( '.' )
				.append( memberName );
	}

	private void executeProjectionQuery(StringBuilder declaration, List<String> paramTypes) {
		final String indent = dataRepository && !isReactive() ? "\t\t" : "\t";
		if ( containerType == null ) {
			declaration
					.append( indent )
					.append( "var _result = _select\n" );
			setFirstResultLimit( declaration );
			handlePageParameters( declaration, paramTypes, null );
			declaration
					.append( nullable ? "\t\t\t.getSingleResultOrNull();\n" : "\t\t\t.getSingleResult();\n" )
					.append( indent )
					.append( "return " );
			if ( nullable ) {
				declaration.append( "_result == null ? null : " );
			}
			projectionResult( declaration, "_result" );
		}
		else {
			declaration
					.append( indent )
					.append( "return _select\n" );
			setFirstResultLimit( declaration );
			handlePageParameters( declaration, paramTypes, containerType );
			switch ( containerType ) {
				case "[]":
					declaration
							.append( "\t\t\t.getResultList()\n" )
							.append( "\t\t\t.stream()\n" )
							.append( "\t\t\t.map(_result -> " );
					projectionResult( declaration, "_result" );
					declaration
							.append( ")\n" )
							.append( "\t\t\t.toArray(" )
							.append( annotationMetaEntity.importType( returnTypeName ) )
							.append( "[]::new)" );
					break;
				case OPTIONAL:
					declaration
							.append( "\t\t\t.uniqueResultOptional()\n" )
							.append( "\t\t\t.map(_result -> " );
					projectionResult( declaration, "_result" );
					declaration.append( ")" );
					break;
				case STREAM:
					declaration
							.append( "\t\t\t.getResultStream()\n" )
							.append( "\t\t\t.map(_result -> " );
					projectionResult( declaration, "_result" );
					declaration.append( ")" );
					break;
				case LIST:
					declaration
							.append( "\t\t\t.getResultList()\n" )
							.append( "\t\t\t.stream()\n" )
							.append( "\t\t\t.map(_result -> " );
					projectionResult( declaration, "_result" );
					declaration
							.append( ")\n" )
							.append( "\t\t\t.toList()" );
					break;
				default:
					break;
			}
		}
	}

	private void projectionResult(StringBuilder declaration, String resultVariable) {
		declaration
				.append( "new " )
				.append( annotationMetaEntity.importType( querySelection.resultTypeName() ) )
				.append( "(" );
		for ( int i = 0; i < querySelection.paths().size(); i++ ) {
			if ( i > 0 ) {
				declaration.append( ", " );
			}
			declaration
					.append( projectionVariableName( i ) )
					.append( ".in(" )
					.append( resultVariable )
					.append( ")" );
		}
		declaration.append( ")" );
	}

	private String projectionVariableName(int index) {
		return "_" + unqualifiedName( querySelection.resultTypeName() )
				+ "_" + querySelection.componentNames().get( index );
	}

	private static String unqualifiedName(String name) {
		return name.substring( Math.max( name.lastIndexOf( '.' ), name.lastIndexOf( '$' ) ) + 1 );
	}

	String specificationType() {
		return isUpdate
				? "org.hibernate.query.specification.MutationSpecification"
				: "org.hibernate.query.specification.SelectionSpecification";
	}

	@Override
	void createQuery(StringBuilder declaration, boolean declareVariable) {
		if ( declareVariable ) {
			if ( dataRepository && !isReactive() ) {
				declaration
						.append('\t');
			}
			declaration
					.append('\t');
			declaration.append("var _select = ");
		}
		if ( useSpecificationCreateQuery() ) {
			declaration
					.append("_spec.createQuery(");
			localSession( declaration );
			declaration
					.append(")");
		}
		else if ( isUsingSpecification() ) {
			localSession( declaration );
			declaration
					.append(".createQuery(_spec.buildCriteria(");
			localSession( declaration );
			if ( isReactive() ) {
				declaration
						.append(".getFactory().getCriteriaBuilder()))\n");
			}
			else {
				declaration
						.append(".getCriteriaBuilder()))");
			}
		}
		else if ( useQueryReferenceCreateQuery() ) {
			localSession( declaration );
			declaration
					.append( isUpdate ? ".createStatement(" : ".createQuery(" );
			createQueryReference( declaration );
			declaration.append(")");
		}
		else if ( useNamedQuery() ) {
			localSession( declaration );
			declaration
					.append('.')
					.append(createNamedQueryMethod())
					.append("(")
					.append(stringLiteral(castNonNull(namedQueryName)));
			if ( returnTypeClass != null && !isUpdate ) {
				declaration
						.append(", ")
						.append(annotationMetaEntity.importType(returnTypeClass))
						.append(".class");
			}
			declaration.append(")");
		}
		else {
			localSession( declaration );
			declaration
					.append('.')
					.append(createQueryMethod())
					.append("(")
					.append(getConstantName());
			if ( resultSetMapping != null && !isUpdate && !isReactive() ) {
				declaration
						.append(", ")
						.append(resultSetMapping);
			}
			else if ( returnTypeClass != null && !isUpdate ) {
				declaration
						.append(", ")
						.append(annotationMetaEntity.importType(returnTypeClass))
						.append(".class");
			}
			declaration.append(")");
		}
	}

	@Override
	void createSpecification(StringBuilder declaration) {
		final String targetType = specificationTargetType();
		if ( targetType != null && isUsingSpecification() ) {
			declaration
					.append( "\tvar _spec = " )
					.append( annotationMetaEntity.importType( specificationType() ) );
			if ( isUpdate && namedQueryName != null ) {
				declaration
						.append( ".<" )
						.append( annotationMetaEntity.importType( targetType ) )
						.append( ">create(" );
			}
			else {
				declaration.append( ".create(" );
			}
			if ( namedQueryName == null ) {
				declaration
						.append( annotationMetaEntity.importType( targetType ) )
						.append( ".class, " )
						.append( getConstantName() );
			}
			else {
				createQueryReference( declaration );
			}
			declaration.append( ");\n" );
		}
	}

	@Override
	final boolean isUsingSpecification() {
		return specificationTargetType() != null
			&& ( hasRestriction() || hasOrder() && !isJakartaCursoredPage(containerType) );
	}

	private boolean bindsParametersFromReference() {
		return namedQueryName != null
			&& ( useSpecificationCreateQuery() || useQueryReferenceCreateQuery() );
	}

	private boolean useQueryReferenceCreateQuery() {
		return namedQueryName != null
			&& !isUsingSpecification()
			&& useGeneratedQueryReferenceMethod();
	}

	private boolean useNamedQuery() {
		return namedQueryName != null
			&& !isUsingSpecification()
			&& !useQueryReferenceCreateQuery();
	}

	private @Nullable String specificationTargetType() {
		if ( isUpdate ) {
			final String restrictionTargetType = restrictionTargetType();
			return restrictionTargetType == null ? returnTypeClass : restrictionTargetType;
		}
		else {
			return returnTypeClass;
		}
	}

	private @Nullable String restrictionTargetType() {
		for ( String paramType : paramTypes ) {
			if ( isRestrictionParam( paramType ) ) {
				final String targetType = restrictionTargetType( paramType );
				if ( targetType != null ) {
					return targetType;
				}
			}
		}
		return null;
	}

	private static @Nullable String restrictionTargetType(String paramType) {
		final int restrictionIndex = paramType.indexOf( "Restriction<" );
		if ( restrictionIndex < 0 ) {
			return null;
		}
		final String superBound = "? super ";
		int start = restrictionIndex + "Restriction<".length();
		if ( paramType.startsWith( superBound, start ) ) {
			start += superBound.length();
		}
		final int end = paramType.indexOf( '>', start );
		return end > start ? paramType.substring( start, end ) : null;
	}

	private void createQueryReference(StringBuilder declaration) {
		if ( useGeneratedQueryReferenceMethod() ) {
			declaration
					.append( annotationMetaEntity.importType(
							annotationMetaEntity.getQueryMetamodelFullyQualifiedName() ) )
					.append( '.' )
					.append( methodName )
					.append( '(' );
			appendQueryReferenceArguments( declaration );
			declaration.append( ')' );
		}
		else {
			createInlineQueryReference( declaration );
		}
	}

	private boolean useGeneratedQueryReferenceMethod() {
		return generatedQueryReferenceMethod;
	}

	private void appendQueryReferenceArguments(StringBuilder declaration) {
		final List<String> names = queryParameterNames();
		for ( int i = 0; i < names.size(); i++ ) {
			if ( i > 0 ) {
				declaration.append( ", " );
			}
			declaration.append( parameterName( names.get( i ) ) );
		}
	}

	private void createInlineQueryReference(StringBuilder declaration) {
		if ( isUpdate ) {
			declaration
					.append( "new " )
					.append( annotationMetaEntity.importType( STATIC_STATEMENT_REFERENCE ) )
					.append( "(" );
		}
		else {
			declaration
					.append( "new " )
					.append( annotationMetaEntity.importType( STATIC_TYPED_QUERY_REFERENCE ) )
					.append( "<>(" );
		}
		StaticQueryMethod.constructorArguments(
				annotationMetaEntity,
				declaration,
				castNonNull( namedQueryName ),
				methodName,
				isUpdate,
				isNative,
				returnTypeClass,
				queryParameterNames(),
				queryParameterTypes(),
				getAnnotationMirror( method, QUERY_OPTIONS )
		);
		declaration
				.append( "\n\t)" );
	}

	private List<String> queryParameterNames() {
		return AnnotationMetaEntity.queryParameterNames( paramNames, paramTypes );
	}

	private List<String> queryParameterTypes() {
		return AnnotationMetaEntity.queryParameterTypes( paramTypes );
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

	private String createNamedQueryMethod() {
		return isUpdate
			&& !isUsingEntityManager()
			&& !isReactive()
				? "createNamedMutationQuery"
				: "createNamedQuery";
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
	void setParameters(StringBuilder declaration, List<String> paramTypes) {
		for ( int i = 0; i < paramNames.size(); i++ ) {
			if ( !isSpecialParam( paramTypes.get(i) ) ) {
				final String paramName = paramNames.get(i);
				final int ordinal = i+1;
				if ( queryString.contains(":" + paramName) ) {
					setNamedParameter( declaration, paramName );
				}
				else if ( queryString.contains("?" + ordinal) ) {
					setOrdinalParameter( declaration, ordinal, paramName );
				}
				else if ( isNative && hasJdbcOrdinalParameter( ordinal ) ) {
					setOrdinalParameter( declaration, ordinal, paramName );
				}
			}
		}
	}

	private boolean hasJdbcOrdinalParameter(int ordinal) {
		class OrdinalParameterRecognizer implements ParameterRecognizer {
			boolean found = false;
			int ordinalCount = 0;
			@Override
			public void ordinalParameter(int sourcePosition) {
				if ( ++ordinalCount == ordinal ) {
					found = true;
				}
			}
			@Override
			public void namedParameter(String name, int sourcePosition) {
			}
			@Override
			public void jpaPositionalParameter(int label, int sourcePosition) {
			}
			@Override
			public void other(char character) {
			}
		}
		final OrdinalParameterRecognizer recognizer = new OrdinalParameterRecognizer();
		ParameterParser.parse( queryString, recognizer );
		return recognizer.found;
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
				.append("\n/**")
				.append("\n * Execute the query {@value #")
				.append(getConstantName())
				.append("}.")
				.append("\n *");
		see( declaration );
		declaration
				.append("\n **/\n");
	}

	private void modifiers(StringBuilder declaration, List<String> paramTypes) {
		final boolean hasVarargs =
				paramTypes.stream().anyMatch(ptype -> ptype.endsWith("..."));
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
			if ( isSpecialParam(paramType) ) {
				nullCheck( declaration, paramNames.get(i) );
			}
		}
	}

	@Override
	public String getAttributeNameDeclarationString() {
		final StringBuilder declaration =
				new StringBuilder( queryString.length() + 200 );
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
			declaration.append(switch ( c ) {
				case '\r' -> "\\r";
				case '\n' -> "\\n";
				case '\\' -> "\\\\";
				case '"' -> "\\\"";
				default -> c;
			});
		}
		return declaration
				.append("\";")
				.toString();
	}

	private String getConstantName() {
		final String stem = getUpperUnderscoreCaseFromLowerCamelCase(methodName);
		return paramTypes.isEmpty()
			|| paramTypes.stream().allMatch(AbstractQueryMethod::isSpecialParam)
				? stem
				: stem + "_" + paramTypes.stream()
						.filter( type -> !isSpecialParam(type) )
						.map( type -> type.indexOf('<') > 0
								? type.substring(0, type.indexOf('<'))
								: type )
						.map( StringHelper::unqualify )
						.map( type -> type.replace("[]", "Array") )
						.reduce( (x, y) -> x + '_' + y )
						.orElseThrow();
	}

	public String getTypeDeclaration() {
		return QUERY;
	}
}
