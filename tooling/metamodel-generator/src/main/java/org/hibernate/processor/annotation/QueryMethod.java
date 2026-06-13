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

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.processor.annotation.QueryOptionsSupport.setQueryOptions;
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
		final var declaration = new StringBuilder();
		comment( declaration );
		modifiers( declaration, paramTypes );
		preamble( declaration, paramTypes );
		nullChecks( declaration, paramTypes );
		if ( useAugmentedQuery() ) {
			createAugmentedQuery( declaration );
		}
		else {
			createSpecification( declaration );
			handleRestrictionParameters( declaration, paramTypes );
			collectOrdering( declaration, paramTypes, containerType );
		}
		chainSession( declaration );
		inTry( declaration );
		createQuery( declaration, true );
		if ( !bindsParametersFromReference() ) {
			setParameters( declaration, paramTypes );
		}
		if ( !usesQueryReference() ) {
			setQueryOptions( this, declaration, isUpdate, isNative );
		}
		declaration.append( ";\n" );
		results( declaration, paramTypes, containerType );
		select( declaration );
		setFirstResultLimit( declaration );
		handlePageParameters( declaration, paramTypes, containerType );
		execute( declaration, initiallyUnwrapped() );
		convertExceptions( declaration );
		chainSessionEnd( isUpdate, declaration );
		closingBrace( declaration );
		return declaration.toString();
	}

	private String specificationType() {
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
		if ( useAugmentedQueryReference() ) {
			localSession( declaration );
			declaration
					.append(".createQuery(_reference)");
		}
		else if ( usesAugmentedQueryString() ) {
			localSession( declaration );
			declaration
					.append(".createQuery(_query)");
		}
		else if ( useSpecificationCreateQuery() ) {
			declaration
					.append("_spec.createQuery(");
			localSession( declaration );
			declaration
					.append(')');
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
			if ( useReactiveStatementReferenceCreateQuery() ) {
				declaration.append( ".createNamedQuery(" );
				createQueryReference( declaration );
				declaration.append(".getName())");
			}
			else {
				declaration
						.append(isUpdate
								? ".createStatement("
								: ".createQuery(");
				createQueryReference( declaration );
				declaration.append(")");
			}
		}
		else {
			localSession( declaration );
			declaration
					.append('.')
					.append(createQueryMethod())
					.append('(')
					.append(getConstantName());
			if ( !isUpdate ) {
				declaration
						.append( ", " );
				if ( resultSetMapping != null && !isReactive() ) {
					declaration
							.append( resultSetMapping );
				}
				else if ( returnTypeClass != null) {
					declaration
							.append( annotationMetaEntity.importType( returnTypeClass ) )
							.append( ".class" );
				}
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
			if ( isUpdate && useGeneratedQueryReferenceMethod() ) {
				declaration
						.append( ".<" )
						.append( annotationMetaEntity.importType( targetType ) )
						.append( ">create(" );
			}
			else {
				declaration.append( ".create(" );
			}
			if ( useGeneratedQueryReferenceMethod() ) {
				createQueryReference( declaration );
			}
			else {
				createStringSpecificationArguments( declaration, targetType );
			}
			declaration.append( ");\n" );
		}
	}

	@Override
	final boolean isUsingSpecification() {
		return specificationTargetType() != null
			&& ( hasRestriction() || hasOrder() && !isJakartaCursoredPage(containerType) );
	}

	private void createStringSpecificationArguments(StringBuilder declaration, String targetType) {
		declaration
				.append( annotationMetaEntity.importType( targetType ) )
				.append( ".class, " )
				.append( getConstantName() );
	}

	/**
	 * Does this query need Criteria augmentation to apply a Jakarta Data {@code @Select}
	 * projection to an annotated query?
	 * <p>
	 * This is limited to non-native, non-reactive selection queries. Native SQL cannot
	 * be converted to Criteria, mutation queries cannot have a select projection, and
	 * the reactive generator uses Hibernate Reactive query APIs.
	 */
	private boolean useAugmentedQuery() {
		return selection != null
			&& selectionEntity != null
			&& !isUpdate
			&& !isNative
			&& !isReactive();
	}

	/**
	 * Should an augmented {@code @Select} query start from the generated static query
	 * reference method?
	 * <p>
	 * When the reference method is available, it carries the query name, arguments,
	 * and {@code @QueryOptions}, so the repository can augment the reference instead
	 * of reconstructing the query from a string.
	 */
	private boolean useAugmentedQueryReference() {
		return useAugmentedQuery()
			&& useGeneratedQueryReferenceMethod();
	}

	/**
	 * Should an augmented {@code @Select} query be built directly from the query string
	 * constant?
	 * <p>
	 * This is the fallback for annotated queries whose generated reference method is
	 * unavailable, for example, because the method parameters cannot be represented as
	 * reference arguments. We still need Criteria augmentation for the projection.
	 */
	private boolean usesAugmentedQueryString() {
		return useAugmentedQuery()
			&& !useGeneratedQueryReferenceMethod();
	}

	/**
	 * Are arguments to ordinary query parameters bound by the static query reference
	 * or specification?
	 * <p>
	 * {@code Reference}s to static queries and statements include argument values, and
	 * {@code QuerySpecification} binds them when it creates the query. Build criteria
	 * paths are excluded because they turn the specification into a Criteria query first,
	 * so the generated code must still call {@code setParameter()} explicitly.
	 */
	private boolean bindsParametersFromReference() {
		return useAugmentedQueryReference()
			|| useGeneratedQueryReferenceMethod()
				&& ( isUsingSpecification()
						? useSpecificationCreateQuery()
						: !useReactiveStatementReferenceCreateQuery() );
	}

	/**
	 * Does the generated query originate from a generated query reference method?
	 * <p>
	 * This is intentionally broader than {@link #bindsParametersFromReference()} because
	 * some paths still need explicit parameter binding, but all reference-backed paths
	 * already carry {@code @QueryOptions}, so those options must not be applied again to
	 * the query object.
	 */
	private boolean usesQueryReference() {
		return useAugmentedQueryReference()
			|| useGeneratedQueryReferenceMethod();
	}

	/**
	 * Should the repository method call {@code createQuery(reference)} or
	 * {@code createStatement(reference)} directly?
	 * <p>
	 * This is only valid when there is a named repository query, no dynamic specification
	 * augmentation is needed, and a generated reference method exists. If a specification
	 * is needed, the reference must be used to create the specification instead. If no
	 * generated reference exists, the method falls back to either a named query or the
	 * query string constant.
	 */
	private boolean useQueryReferenceCreateQuery() {
		return !isUsingSpecification()
			&& useGeneratedQueryReferenceMethod();
	}

	/**
	 * Is this the Hibernate Reactive mutation special case?
	 * <p>
	 * Hibernate Reactive does not yet support {@code StatementReference}, so reactive
	 * mutation queries use the generated reference only to obtain the named query name
	 * and then call {@code createNamedQuery(name)}. Since the query is not created from
	 * the reference object, parameters and options cannot be assumed to have been applied
	 * by the reference.
	 */
	// TODO: Fix Hibernate Reactive to remove this special case!
	private boolean useReactiveStatementReferenceCreateQuery() {
		return isReactive() && isUpdate;
	}

	private @Nullable String specificationTargetType() {
		if ( isUpdate ) {
			final var restrictionTargetType = restrictionTargetType();
			return restrictionTargetType == null
					? returnTypeClass
					: restrictionTargetType;
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
		declaration
				.append( annotationMetaEntity.importType(annotationMetaEntity.getQueryMetamodelFullyQualifiedName() ) )
				.append( '.' )
				.append( methodName )
				.append( '(' );
		appendQueryReferenceArguments( declaration );
		declaration.append( ')' );
	}

	/**
	 * Is there a generated static query reference method in the query metamodel for
	 * this repository method?
	 * <p>
	 * The method is only generated when the repository query can be represented as a
	 * static reference with bindable arguments. If not, generated code must avoid both
	 * calls to the metamodel reference method and inline {@code Static*Reference}
	 * construction, and use the static query string constant instead.
	 */
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

	private void createAugmentedQuery(StringBuilder declaration) {
		createBuilder( declaration );
		final boolean augmentedQueryReference = useAugmentedQueryReference();
		if ( augmentedQueryReference ) {
			declaration
					.append( "\tvar _reference = _builder.augment(" );
			createQueryReference( declaration );
			declaration
					.append( ", " )
					// Never null because this is called only when useAugmentedQuery() is true
					.append( annotationMetaEntity.importType( selection.resultTypeName() ) )
					.append( ".class, _query -> {\n\t" );
		}
		else {
			declaration
					.append( "\tvar _query = _builder.createQuery(" )
					// Never null when useAugmentedQuery() is true but useAugmentedQueryReference() is false
					.append( annotationMetaEntity.importType( castNonNull( returnTypeClass ) ) )
					.append( ".class, " )
					.append( getConstantName() )
					.append( ");\n" );
		}
		getRoot( declaration );
		applyCriteriaRestrictionParameters( declaration, paramTypes, "\t\t", false );
		// Never null because this is called only when useAugmentedQuery() is true
		applyCriteriaOrdering( declaration, paramTypes, "\t\t", castNonNull( selectionEntity ), false );
		select( declaration, castNonNull( selectionEntity ), castNonNull( selection ) );
		if ( augmentedQueryReference ) {
			declaration.append( "\t});\n" );
		}
	}

	private void getRoot(StringBuilder declaration) {
		declaration
				.append( "\tvar _entity = (" )
				.append( annotationMetaEntity.importType( "jakarta.persistence.criteria.Root" ) )
				.append( "<" )
				// Never null because this is called only when useAugmentedQuery() is true
				.append( annotationMetaEntity.importType( castNonNull( selectionEntity ) ) )
				.append( ">) _query.getRootList().get(0);\n" );
	}

	private List<String> queryParameterNames() {
		return AnnotationMetaEntity.queryParameterNames( paramNames, paramTypes );
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
			return isUpdate
					? "createMutationQuery"
					: "createSelectionQuery";
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
		int positionalParameterPosition = 0;
		for ( int i = 0; i < paramNames.size(); i++ ) {
			if ( !isSpecialParam( paramTypes.get(i) ) ) {
				final var paramName = paramNames.get(i);
				if ( queryString.contains(":" + paramName) ) {
					setNamedParameter( declaration, paramName );
				}
				else {
					positionalParameterPosition++;
					if ( queryString.contains( "?" + positionalParameterPosition ) ) {
						setOrdinalParameter( declaration, positionalParameterPosition, paramName );
					}
					else if ( isNative && hasJdbcOrdinalParameter( positionalParameterPosition ) ) {
						setOrdinalParameter( declaration, positionalParameterPosition, paramName );
					}
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
