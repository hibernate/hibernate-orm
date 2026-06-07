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
import java.util.List;

import static org.hibernate.processor.annotation.QueryOptionsSupport.setQueryOptions;
import static org.hibernate.processor.annotation.QueryOptionsSupport.stringLiteral;
import static org.hibernate.processor.util.Constants.BOOLEAN;
import static org.hibernate.processor.util.Constants.QUERY_OPTIONS;
import static org.hibernate.processor.util.Constants.QUERY;
import static org.hibernate.processor.util.Constants.STATIC_STATEMENT_REFERENCE;
import static org.hibernate.processor.util.Constants.STATIC_TYPED_QUERY_REFERENCE;
import static org.hibernate.processor.util.Constants.VOID;
import static org.hibernate.processor.util.NullnessUtil.castNonNull;
import static org.hibernate.processor.util.StringUtil.getUpperUnderscoreCaseFromLowerCamelCase;
import static org.hibernate.processor.util.TypeUtils.getAnnotationMirror;

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
	private final @Nullable ResultSelection selection;
	private final @Nullable String selectionEntity;

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
			@Nullable ResultSelection selection,
			@Nullable String selectionEntity,
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
		this.namedQueryName = namedQueryName;
		this.returnTypeClass = returnTypeClass;
		this.containerType = containerType;
		this.resultSetMapping = resultSetMapping;
		this.isUpdate = isUpdate;
		this.isNative = isNative;
		this.generatedQueryReferenceMethod = generatedQueryReferenceMethod;
		this.selection = selection;
		this.selectionEntity = selectionEntity;
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

	@Override
	void results(StringBuilder declaration, List<String> paramTypes, @Nullable String containerType) {
		super.results( declaration, paramTypes, containerType );
		if ( isUpdate && returnsLong() ) {
			declaration.append( "(long) " );
		}
	}

	@Override
	public String getAttributeDeclarationString() {
		final var paramTypes = parameterTypes();
		if ( usesAugmentedQueryReference() ) {
			return getAugmentedQueryReferenceAttributeDeclarationString( paramTypes );
		}
		final var declaration = new StringBuilder();
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
			setQueryOptions( this, declaration, isUpdate, isNative );
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

	private String getAugmentedQueryReferenceAttributeDeclarationString(List<String> paramTypes) {
		final var declaration = new StringBuilder();
		comment( declaration );
		modifiers( declaration, paramTypes );
		preamble( declaration, paramTypes );
		nullChecks( declaration, paramTypes );
		createAugmentedQueryReference( declaration );
		chainSession( declaration );
		inTry( declaration );
		createQuery( declaration, true );
		declaration.append( ";\n" );
		results( declaration, paramTypes, containerType );
		select( declaration );
		setFirstResultLimit( declaration );
		handlePageParameters( declaration, paramTypes, containerType );
		execute( declaration, initiallyUnwrapped() );
		convertExceptions( declaration );
		chainSessionEnd( false, declaration );
		closingBrace( declaration );
		return declaration.toString();
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
					.append('\t')
					.append("var _select =")
					.append(ASSIGNMENT_INDENT);
		}
		if ( usesAugmentedQueryReference() ) {
			localSession( declaration );
			declaration
					.append( ".createQuery(_reference)" );
		}
		else if ( useSpecificationCreateQuery() ) {
			declaration
					.append("_spec.createQuery(");
			localSession( declaration );
			declaration
					.append(")");
		}
		else if ( isUsingSpecification() ) {
			localSession( declaration );
			declaration
					.append(isUpdate && isUsingEntityAgent()
							? ".createStatement(_spec.buildCriteria("
							: ".createQuery(_spec.buildCriteria(");
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
		final var targetType = specificationTargetType();
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

	private boolean usesAugmentedQueryReference() {
		return selection != null
			&& selectionEntity != null
			&& !isUpdate
			&& !isNative
			&& !isReactive();
	}

	private boolean bindsParametersFromReference() {
		return namedQueryName != null
			&& ( usesAugmentedQueryReference() || useSpecificationCreateQuery() || useQueryReferenceCreateQuery() );
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
			final var restrictionTargetType = restrictionTargetType();
			return restrictionTargetType == null ? returnTypeClass : restrictionTargetType;
		}
		else {
			return returnTypeClass;
		}
	}

	private @Nullable String restrictionTargetType() {
		for ( var paramType : paramTypes ) {
			if ( isRestrictionParam( paramType ) ) {
				final var targetType = restrictionTargetType( paramType );
				if ( targetType != null ) {
					return targetType;
				}
			}
		}
		return null;
	}

	private static @Nullable String restrictionTargetType(String paramType) {
		final var restrictionIndex = paramType.indexOf( "Restriction<" );
		if ( restrictionIndex < 0 ) {
			return null;
		}
		final var superBound = "? super ";
		var start = restrictionIndex + "Restriction<".length();
		if ( paramType.startsWith( superBound, start ) ) {
			start += superBound.length();
		}
		final var end = paramType.indexOf( '>', start );
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
		final var names = queryParameterNames();
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

	private void createAugmentedQueryReference(StringBuilder declaration) {
		final var selection = castNonNull( this.selection );
		final var selectionEntity = castNonNull( this.selectionEntity );
		declaration
				.append( "\tvar _builder = " );
		localSession( declaration );
		declaration
				.append( ".getCriteriaBuilder();\n" )
				.append( "\tvar _reference = _builder.augment(" );
		createQueryReference( declaration );
		declaration
				.append( ", _query -> {\n" )
				.append( "\t\tvar _entity = (" )
				.append( annotationMetaEntity.importType( "jakarta.persistence.criteria.Root" ) )
				.append( "<" )
				.append( annotationMetaEntity.importType( selectionEntity ) )
				.append( ">) _query.getRoots().iterator().next();\n" );
		applyCriteriaRestrictionParameters( declaration, paramTypes, "\t\t", false );
		applyCriteriaOrdering( declaration, paramTypes, "\t\t", selectionEntity, false );
		declaration.append( "\t\t_query.select(" );
		if ( selection.recordProjection() ) {
			declaration
					.append( "_builder.construct(" )
					.append( annotationMetaEntity.importType( selection.resultTypeName() ) )
					.append( ".class" );
			for ( var path : selection.paths() ) {
				declaration.append( ",\n\t\t\t\t\t\t" );
				selectionExpression( declaration, path, selectionEntity );
			}
			declaration.append( ")" );
		}
		else {
			selectionExpression( declaration, selection.paths().get( 0 ), selectionEntity );
		}
		declaration.append( ")" );
		declaration
				.append( ";\n" )
				.append( "\t});\n" );
	}

	private List<String> queryParameterNames() {
		return AnnotationMetaEntity.queryParameterNames( paramNames, paramTypes );
	}

	private List<String> queryParameterTypes() {
		return AnnotationMetaEntity.queryParameterTypes( paramTypes );
	}

	private String createQueryMethod() {
		if ( isNative ) {
			return isUpdate && !isReactive()
					? "createNativeStatement"
					: "createNativeQuery";
		}
		else if ( isUpdate && isUsingEntityHandler() ) {
			return "createStatement";
		}
		else if ( isUsingEntityHandler() || isReactive() || isUnspecializedQueryType(containerType) ) {
			return "createQuery";
		}
		else {
			return isUpdate ? "createMutationQuery" : "createSelectionQuery";
		}
	}

	private String createNamedQueryMethod() {
		if ( isUpdate && !isReactive() ) {
			return isUsingEntityHandler()
					? "createNamedStatement"
					: "createNamedMutationQuery";
		}
		else {
			return "createNamedQuery";
		}
	}

	private void castResult(StringBuilder declaration) {
		if ( isNative && returnTypeName != null && containerType == null
				&& isUsingEntityHandler() ) {
			// EntityManager.createNativeQuery() does not return TypedQuery,
			// so we need to cast to the entity type
			declaration
					.append("(")
					.append(annotationMetaEntity.importType(returnTypeName))
					.append(") ");
		}
	}

	private void execute(StringBuilder declaration, boolean unwrapped) {
		if ( isUpdate ) {
			declaration
					.append( isReactive() ? ".executeUpdate()" : ".execute()" );
			if ( isAsynchronousCompletionStageWithVoidResult() ) {
				declaration
						.append( ";\n\t\t" );
				returnNullResult( declaration );
			}
			else if ( isReactive() ) {
				if ( VOID.equals(returnTypeName) ) {
					declaration
							.append( ".replaceWithVoid()" );
				}
				else if ( BOOLEAN.equals(returnTypeName) ) {
					declaration
							.append( ".map(rows -> rows>0)" );
				}
			}
			else {
				if ( "boolean".equals( returnTypeName ) || BOOLEAN.equals( returnTypeName ) ) {
					declaration
							.append( " > 0" );
				}
				endReturnResult( declaration );
			}
		}
		else {
			final var mustUnwrap =
					isHibernateQueryType(containerType)
							|| isNative && returnTypeName != null;
			executeSelect( declaration, paramTypes, containerType, unwrapped, mustUnwrap );
		}
	}

	private boolean returnsLong() {
		return "long".equals( returnTypeName )
			|| "java.lang.Long".equals( returnTypeName );
	}

	@Override
	void setParameters(StringBuilder declaration, List<String> paramTypes) {
		for ( int i = 0; i < paramNames.size(); i++ ) {
			if ( !isSpecialParam( paramTypes.get(i) ) ) {
				final var paramName = paramNames.get(i);
				final var ordinal = i+1;
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
		final var recognizer = new OrdinalParameterRecognizer();
		ParameterParser.parse( queryString, recognizer );
		return recognizer.found;
	}

	static final String PARAM_INDENT = "\n\t\t\t\t\t\t";

	private static void setOrdinalParameter(StringBuilder declaration, int i, String paramName) {
		declaration
				.append(PARAM_INDENT)
				.append(".setParameter(")
				.append(i)
				.append(", ")
				.append(paramName)
				.append(")");
	}

	private static void setNamedParameter(StringBuilder declaration, String paramName) {
		declaration
				.append(PARAM_INDENT)
				.append(".setParameter(\"")
				.append(paramName)
				.append("\", ")
				.append(paramName)
				.append(")");
	}

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
		final var hasVarargs =
				paramTypes.stream().anyMatch(ptype -> ptype.endsWith("..."));
		if ( hasVarargs ) {
			declaration
					.append("@SafeVarargs\n");
		}
		if ( belongsToDao ) {
			declaration.append("@Override\n");
			returnNullness( declaration );
			declaration.append("public ");
			if ( hasVarargs ) {
				declaration
						.append("final ");
			}
		}
		else {
			declaration.append("public static ");
		}
	}

	void nullChecks(StringBuilder declaration, List<String> paramTypes) {
		for ( int i = 0; i<paramNames.size(); i++ ) {
			final var paramType = paramTypes.get( i );
			// we don't do null checks on query parameters
			if ( isSpecialParam(paramType) ) {
				nullCheck( declaration, paramNames.get(i) );
			}
		}
	}

	@Override
	public String getAttributeNameDeclarationString() {
		final var declaration =
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
			final var c = queryString.charAt( i );
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
		final var stem = getUpperUnderscoreCaseFromLowerCamelCase(methodName);
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
