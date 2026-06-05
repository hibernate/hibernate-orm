/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import jakarta.annotation.Nullable;
import org.hibernate.AssertionFailure;

import javax.lang.model.element.ExecutableElement;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import static java.util.stream.Collectors.toList;
import static org.hibernate.metamodel.mapping.EntityIdentifierMapping.ID_ROLE_NAME;
import static org.hibernate.processor.util.Constants.BOXED_VOID;
import static org.hibernate.processor.util.Constants.COMPLETABLE_FUTURE;
import static org.hibernate.processor.util.Constants.COLLECTORS;
import static org.hibernate.processor.util.Constants.HIB_JAKARTA_DATA_RESTRICTION;
import static org.hibernate.processor.util.Constants.HIB_KEYED_PAGE;
import static org.hibernate.processor.util.Constants.HIB_KEYED_RESULT_LIST;
import static org.hibernate.processor.util.Constants.HIB_ORDER;
import static org.hibernate.processor.util.Constants.HIB_PAGE;
import static org.hibernate.processor.util.Constants.HIB_QUERY;
import static org.hibernate.processor.util.Constants.HIB_RANGE;
import static org.hibernate.processor.util.Constants.HIB_RESTRICTION;
import static org.hibernate.processor.util.Constants.HIB_SELECTION_QUERY;
import static org.hibernate.processor.util.Constants.HIB_SORT_DIRECTION;
import static org.hibernate.processor.util.Constants.JD_CURSORED_PAGE;
import static org.hibernate.processor.util.Constants.JD_FIRST;
import static org.hibernate.processor.util.Constants.JD_LIMIT;
import static org.hibernate.processor.util.Constants.JD_ORDER;
import static org.hibernate.processor.util.Constants.JD_PAGE;
import static org.hibernate.processor.util.Constants.JD_PAGE_REQUEST;
import static org.hibernate.processor.util.Constants.JD_RESTRICT;
import static org.hibernate.processor.util.Constants.JD_RESTRICTION;
import static org.hibernate.processor.util.Constants.JD_SORT;
import static org.hibernate.processor.util.Constants.LIST;
import static org.hibernate.processor.util.Constants.NONNULL;
import static org.hibernate.processor.util.Constants.NULLABLE;
import static org.hibernate.processor.util.Constants.OPTIONAL;
import static org.hibernate.processor.util.Constants.QUERY;
import static org.hibernate.processor.util.Constants.SESSION_TYPES;
import static org.hibernate.processor.util.Constants.STREAM;
import static org.hibernate.processor.util.Constants.TYPED_QUERY;
import static org.hibernate.processor.util.TypeUtils.getGeneratedClassFullyQualifiedName;
import static org.hibernate.processor.util.TypeUtils.getAnnotationMirror;
import static org.hibernate.processor.util.TypeUtils.getAnnotationValue;
import static org.hibernate.processor.util.TypeUtils.isPrimitive;

/**
 * @author Gavin King
 */
public abstract class AbstractQueryMethod extends AbstractAnnotatedMethod {
	final String methodName;
	final List<String> paramNames;
	final List<String> paramTypes;
	final @Nullable String returnTypeName;
	final boolean belongsToDao;
	final List<OrderBy> orderBys;
	final boolean addNonnullAnnotation;
	final boolean dataRepository;
	final String fullReturnType;
	final boolean nullable;

	AbstractQueryMethod(
			AnnotationMetaEntity annotationMetaEntity,
			ExecutableElement method,
			String methodName,
			List<String> paramNames, List<String> paramTypes,
			@Nullable String returnTypeName,
			String sessionType,
			String sessionName,
			boolean belongsToDao,
			List<OrderBy> orderBys,
			boolean addNonnullAnnotation,
			boolean dataRepository,
			String fullReturnType,
			boolean nullable) {
		super(annotationMetaEntity, method, sessionName, sessionType);
		this.methodName = methodName;
		this.paramNames = paramNames;
		this.paramTypes = paramTypes;
		this.returnTypeName = returnTypeName;
		this.belongsToDao = belongsToDao;
		this.orderBys = orderBys;
		this.addNonnullAnnotation = addNonnullAnnotation;
		this.dataRepository = dataRepository;
		this.fullReturnType = fullReturnType;
		this.nullable = nullable;
	}

	@Override
	public String getMetaType() {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public String getPropertyName() {
		return methodName;
	}

	abstract boolean isNullable(int index);

	@Nullable
	abstract String containerType();

	boolean useSpecificationCreateQuery() {
		return isUsingSpecification()
			&& !isReactive()
			&& !isUsingEntityAgent()
			&& !isUnspecializedQueryType( containerType() );
	}

	boolean initiallyUnwrapped() {
		return !isUsingEntityHandler() // a TypedQuery from EntityHandler is not a SelectionQuery
			|| useSpecificationCreateQuery(); // SelectionSpecification.createQuery() returns SelectionQuery
	}

	List<String> parameterTypes() {
		return paramTypes.stream()
				.map(paramType -> isOrderParam(paramType) && paramType.endsWith("[]")
						? paramType.substring(0, paramType.length() - 2) + "..."
						: paramType)
				.collect(toList());
	}

	String parameterList() {
		return paramTypes.stream()
				.map(this::strip)
				.map(annotationMetaEntity::importType)
				.reduce((x, y) -> x + ',' + y)
				.orElse("");
	}

	boolean hasRestriction() {
		return paramTypes.stream()
				.anyMatch( type -> isRestrictionParam( type )
								|| isRangeParam( type ) );
	}

	boolean hasOrder() {
		return paramTypes.stream().anyMatch(AbstractQueryMethod::isOrderParam)
			|| !orderBys.isEmpty();
	}

	@Nullable String orderingTypeName() {
		return returnTypeName;
	}

	@Nullable String restrictionTypeName() {
		return returnTypeName;
	}

	String parameterVariableName(int index) {
		return parameterName( paramNames.get( index ) );
	}

	String strip(final String fullType) {
		var type = fullType;
		// strip off type annotations
		while ( type.charAt(0) == '@' ) {
			var startIndex = type.lastIndexOf( ' ' );
			if ( startIndex > 0 ) {
				type = type.substring(startIndex+1);
			}
		}
		// strip off type arguments
		final var endIndex = type.indexOf("<");
		if ( endIndex > 0 ) {
			type = type.substring(0, endIndex);
		}
		return fullType.endsWith("...") ? type + "..." : type;
	}

	void preamble(StringBuilder declaration, List<String> paramTypes) {
		declaration
				.append(annotationMetaEntity.importType(fullReturnType))
				.append(" ")
				.append(methodName);
		parameters( paramTypes, declaration );
		declaration
				.append(" {\n");
	}

	void parameters(List<String> paramTypes, StringBuilder declaration) {
		declaration
				.append("(");
		sessionParameter( declaration );
		for ( int i = 0; i < paramNames.size(); i++ ) {
			if ( i > 0 ) {
				declaration
						.append(", ");
			}
			if ( isNonNull(i, paramTypes) ) {
				notNull( declaration );
			}
			declaration
					.append(annotationMetaEntity.importType(paramTypes.get(i)))
					.append(" ")
					.append(parameterName(paramNames.get(i)));
		}
		declaration
				.append(")");
	}

	boolean isNonNull(int i, List<String> paramTypes) {
		final var paramType = paramTypes.get(i);
		return !isNullable(i) && !isPrimitive(paramType)
			|| isSpecialParam(paramType);
	}

	static boolean isSessionParameter(String paramType) {
		return SESSION_TYPES.contains(paramType);
	}

	void sessionParameter(StringBuilder declaration) {
		if ( !belongsToDao && paramTypes.stream().noneMatch(SESSION_TYPES::contains) ) {
			notNull(declaration);
			declaration
					.append(annotationMetaEntity.importType(sessionType))
					.append(' ')
					.append(sessionName);
			if ( !paramNames.isEmpty() ) {
				declaration
					.append(", ");
			}
		}
	}

	void notNull(StringBuilder declaration) {
		if ( addNonnullAnnotation ) {
			declaration
					.append('@')
					.append(annotationMetaEntity.importType(NONNULL))
					.append(' ');
		}
	}

	void returnNullness(StringBuilder declaration) {
		if ( addNonnullAnnotation && returnTypeName != null && !returnsVoid() && !isPrimitive( returnTypeName ) ) {
			declaration
					.append('@')
					.append(annotationMetaEntity.importType(nullable ? NULLABLE : NONNULL))
					.append('\n');
		}
	}

	void see(StringBuilder declaration) {
		declaration
				.append("\n * @see ")
				.append(annotationMetaEntity.getQualifiedName())
				.append("#");
		signature(declaration);
	}

	void signature(StringBuilder declaration) {
		declaration
				.append(methodName)
				.append("(")
				.append(parameterList())
				.append(")");
	}

	void chainSession(StringBuilder declaration) {
		// Reactive calls always have a return type
		if ( isReactiveSessionAccess() ) {
			declaration
					.append("\treturn ")
					.append(sessionName)
					.append(".chain(")
					.append(localSessionName())
					.append(" -> {\n");
		}
	}

	void chainSessionEnd(boolean isUpdate, StringBuilder declaration) {
		if ( isReactiveSessionAccess() ) {
			declaration.append("\t})");
			// here we're checking for a boxed void and not Uni<Void> because the returnType has already
			// been checked, and is ununi-ed
			if ( isUpdate && returnTypeName != null && returnTypeName.equals(BOXED_VOID) ) {
				declaration.append(".replaceWithVoid();\n");
			}
			else {
				declaration.append(";\n");
			}
		}
	}

	void setPage(StringBuilder declaration, String paramName, String paramType) {
		final var jakartaLimit = JD_LIMIT.equals(paramType);
		final var jakartaPageRequest = JD_PAGE_REQUEST.equals(paramType);
		if ( jakartaLimit || jakartaPageRequest
				|| isUsingEntityHandler() ) {
			final String firstResult;
			final String maxResults;
			if ( jakartaLimit ) {
				firstResult = "(int) " + paramName + ".startAt() - 1";
				maxResults = paramName + ".maxResults()";
			}
			else if ( jakartaPageRequest ) {
				firstResult = "(int) (" + paramName + ".page()-1) * " + paramName + ".size()";
				maxResults = paramName + ".size()";
			}
			else {
				firstResult = paramName + ".getFirstResult()";
				maxResults = paramName + ".getMaxResults()";
			}
			declaration
					.append("\t\t\t.setFirstResult(")
					.append(firstResult)
					.append(")\n")
					.append("\t\t\t.setMaxResults(")
					.append(maxResults)
					.append(")\n");
		}
		else {
			declaration
					.append("\t\t\t.setPage(")
					.append(paramName)
					.append(")\n");
		}
	}

	void handlePageParameters(
			StringBuilder declaration, List<String> paramTypes,
			@Nullable String containerType) {
		if ( !isJakartaCursoredPage(containerType) ) {
			for ( int i = 0; i < paramNames.size(); i ++ ) {
				final var paramName = paramNames.get(i);
				final var paramType = paramTypes.get(i);
				if ( isPageParam(paramType) ) {
					setPage( declaration, paramName, paramType );
				}
			}
		}
	}

	void handleRestrictionParameters(
			StringBuilder declaration, List<String> paramTypes) {
		for ( int i = 0; i < paramNames.size(); i ++ ) {
			final var paramName = paramNames.get(i);
			final var paramType = paramTypes.get(i);
			if ( isRestrictionParam(paramType) ) {
				if ( isJakartaDataRestrictionParam( paramType ) ) {
					declaration
							.append( "\t_spec.restrict(" )
							.append( annotationMetaEntity.staticImport(
									HIB_JAKARTA_DATA_RESTRICTION, "adaptRestriction" ) )
							.append( "(" );
					restrictionArgument( declaration, paramType, paramName, JD_RESTRICT );
					declaration.append( "));\n" );
				}
				else {
					declaration
							.append( "\t_spec.restrict(" );
					restrictionArgument( declaration, paramType, paramName, HIB_RESTRICTION );
					declaration.append( ");\n" );
				}
			}
			else if ( isRangeParam(paramType) ) {
				final var restrictionTypeName = restrictionTypeName();
				if ( restrictionTypeName != null ) {
					final var entityElement =
							annotationMetaEntity.getContext().getElementUtils()
									.getTypeElement( restrictionTypeName );
					declaration
							.append("\t_spec.restrict(")
							.append(annotationMetaEntity.importType(HIB_RESTRICTION))
							.append(".restrict(")
							.append(annotationMetaEntity.importType(
											getGeneratedClassFullyQualifiedName( entityElement, false ) ))
							.append('.')
							.append(paramName)
							.append(", ")
							.append(paramName)
							.append("));\n");
				}
			}
		}
	}

	void applyCriteriaRestrictionParameters(
			StringBuilder declaration,
			List<String> paramTypes,
			String indent) {
		applyCriteriaRestrictionParameters( declaration, paramTypes, indent, true );
	}

	void applyCriteriaRestrictionParameters(
			StringBuilder declaration,
			List<String> paramTypes,
			String indent,
			boolean includeRanges) {
		for ( int i = 0; i < paramNames.size(); i++ ) {
			final var paramName = parameterVariableName( i );
			final var paramType = paramTypes.get( i );
			if ( isRestrictionParam( paramType ) ) {
				applyCriteriaRestrictionParameter( declaration, paramType, paramName, indent );
			}
			else if ( includeRanges && isRangeParam( paramType ) ) {
				applyCriteriaRangeParameter( declaration, i, paramName, indent );
			}
		}
	}

	private void applyCriteriaRestrictionParameter(
			StringBuilder declaration,
			String paramType,
			String paramName,
			String indent) {
		declaration.append( indent );
		if ( isJakartaDataRestrictionParam( paramType ) ) {
			declaration
					.append( annotationMetaEntity.staticImport(
							HIB_JAKARTA_DATA_RESTRICTION, "applyRestriction" ) )
					.append( "(" );
			restrictionArgument( declaration, paramType, paramName, JD_RESTRICT );
			declaration.append( ", _query, _entity, _builder);\n" );
		}
		else {
			restrictionArgument( declaration, paramType, paramName, HIB_RESTRICTION );
			declaration.append( ".apply(_query, _entity);\n" );
		}
	}

	private void applyCriteriaRangeParameter(
			StringBuilder declaration,
			int index,
			String paramName,
			String indent) {
		final var restrictionTypeName = restrictionTypeName();
		if ( restrictionTypeName != null ) {
			final var entityElement =
					annotationMetaEntity.getContext().getElementUtils()
							.getTypeElement( restrictionTypeName );
			declaration
					.append( indent )
					.append( annotationMetaEntity.importType( HIB_RESTRICTION ) )
					.append( ".restrict(" )
					.append( annotationMetaEntity.importType(
							getGeneratedClassFullyQualifiedName( entityElement, false ) ) )
					.append( '.' )
					.append( paramNames.get( index ) )
					.append( ", " )
					.append( paramName )
					.append( ").apply(_query, _entity);\n" );
		}
	}

	private void restrictionArgument(
			StringBuilder declaration,
			String paramType,
			String paramName,
			String restrictionsType) {
		if ( isMultipleRestrictions( paramType ) ) {
			declaration
					.append( annotationMetaEntity.importType( restrictionsType ) )
					.append( ".all(" )
					.append( paramName )
					.append( ")" );
		}
		else {
			declaration.append( paramName );
		}
	}

	private static boolean isMultipleRestrictions(String paramType) {
		return paramType.startsWith(LIST) || paramType.endsWith("[]");
	}

	void selectionExpression(StringBuilder declaration, String path, String typeName) {
		declaration.append( "_entity" );
		path( declaration, path, typeName );
	}

	void path(StringBuilder declaration, String path, String typeName) {
		final var tokens = new StringTokenizer( path, "." );
		while ( typeName != null && tokens.hasMoreTokens() ) {
			final var memberName = tokens.nextToken();
			declaration.append( ".get(" );
			if ( ID_ROLE_NAME.equals( memberName ) ) {
				declaration
						.append( '"' )
						.append( memberName )
						.append( '"' );
			}
			else {
				metamodelAttribute( declaration, typeName, memberName );
			}
			declaration.append( ')' );
			typeName = annotationMetaEntity.getMemberType( typeName, memberName );
		}
	}

	void metamodelAttribute(StringBuilder declaration, String typeName, String memberName) {
		final var typeElement =
				annotationMetaEntity.getContext().getElementUtils()
						.getTypeElement( typeName );
		declaration
				.append( annotationMetaEntity.importType(
						getGeneratedClassFullyQualifiedName( typeElement, false ) ) )
				.append( '.' )
				.append( memberName );
	}

	void convertExceptions(StringBuilder declaration) {
		if ( dataRepository ) {
			if ( !isReactive() ) {
				declaration
						.append(";\n")
						.append( "\t}\n" );
			}
			if ( singleResult() ) {
				handle( declaration, "jakarta.persistence.NoResultException",
						"jakarta.data.exceptions.EmptyResultException" );
				handle( declaration, "jakarta.persistence.NonUniqueResultException",
						"jakarta.data.exceptions.NonUniqueResultException" );
			}
			handle( declaration, "jakarta.persistence.PersistenceException",
					"jakarta.data.exceptions.DataException" );
			if ( isReactive() ) {
				declaration
						.append( ";\n" );
			}
		}
		else {
			declaration
					.append(";\n");
		}
	}

	abstract boolean singleResult();

	static void closingBrace(StringBuilder declaration) {
		declaration.append("}");
	}

	void unwrapQuery(StringBuilder declaration, boolean unwrapped) {
		if ( !unwrapped && isUsingEntityHandler() ) {
			declaration
					.append("\t\t\t.unwrap(")
					.append(annotationMetaEntity.importType(HIB_SELECTION_QUERY))
					.append(".class)\n");
		}
	}

	static boolean isSpecialParam(String parameterType) {
		return isPageParam(parameterType)
			|| isRestrictionParam(parameterType)
			|| isRangeParam(parameterType)
			|| isOrderParam(parameterType)
			|| isKeyedPageParam(parameterType)
			|| isSessionParameter(parameterType);
	}

	static boolean isKeyedPageParam(String parameterType) {
		return parameterType.startsWith(HIB_KEYED_PAGE);
	}

	static boolean isPageParam(String parameterType) {
		return HIB_PAGE.equals(parameterType)
			|| JD_LIMIT.equals(parameterType)
			|| JD_PAGE_REQUEST.equals(parameterType);
	}

	static boolean isOrderParam(String parameterType) {
		return parameterType.startsWith(HIB_ORDER)
			|| parameterType.startsWith(LIST + "<" + HIB_ORDER)
			|| parameterType.startsWith(JD_SORT)
			|| parameterType.startsWith(JD_ORDER) && !parameterType.endsWith("[]");
	}

	static boolean isRestrictionParam(String parameterType) {
		return parameterType.startsWith(HIB_RESTRICTION)
			|| parameterType.startsWith(LIST + "<" + HIB_RESTRICTION)
			|| isJakartaDataRestrictionParam(parameterType);
	}

	static boolean isJakartaDataRestrictionParam(String parameterType) {
		return parameterType.startsWith(JD_RESTRICTION)
			|| parameterType.startsWith(LIST + "<" + JD_RESTRICTION);
	}

	static boolean isRangeParam(String parameterType) {
		return parameterType.startsWith(HIB_RANGE);
	}

	static boolean isJakartaCursoredPage(@Nullable String containerType) {
		return JD_CURSORED_PAGE.equals(containerType);
	}

	static boolean isJakartaPage(@Nullable String containerType) {
		return JD_PAGE.equals(containerType);
	}

	void makeKeyedPage(StringBuilder declaration, List<String> paramTypes) {
		annotationMetaEntity.staticImport(HIB_SORT_DIRECTION, "*");
		annotationMetaEntity.staticImport(HIB_ORDER, "by");
		annotationMetaEntity.staticImport(HIB_PAGE, "page");
		annotationMetaEntity.staticImport("org.hibernate.query.KeyedPage.KeyInterpretation", "*");
		annotationMetaEntity.staticImport(COLLECTORS, "toList");
		if ( returnTypeName == null ) {
			throw new AssertionFailure("entity class cannot be null");
		}
		else {
			declaration
					.append(MAKE_KEYED_PAGE
							.replace("pageRequest",
									parameterName(JD_PAGE_REQUEST, paramTypes, paramNames))
							.replace("Entity", annotationMetaEntity.importType(returnTypeName)))
					.append('\n');
		}
	}

	static final String MAKE_KEYED_SLICE
			= "\t\tvar _cursors =\n" +
			"\t\t\t\t_results.getKeyList()\n" +
			"\t\t\t\t\t\t.stream()\n" +
			"\t\t\t\t\t\t.map(_key -> Cursor.forKey(_key.toArray()))\n" +
			"\t\t\t\t\t\t.collect(toList());\n" +
			"\t\treturn new CursoredPageRecord<>(_results.getResultList(), _cursors, _totalResults, pageRequest,\n" +
			"\t\t\t\t_results.isLastPage() ? null : afterCursor(_cursors.get(_cursors.size()-1), pageRequest.page()+1, pageRequest.size(), pageRequest.requestTotal()),\n" +
			"\t\t\t\t_results.isFirstPage() ? null : beforeCursor(_cursors.get(0), pageRequest.page()-1, pageRequest.size(), pageRequest.requestTotal()))";

	static final String MAKE_KEYED_PAGE
			= "\t\tvar _unkeyedPage =\n" +
			"\t\t\tpage(pageRequest.size(), (int) pageRequest.page()-1)\n" +
			"\t\t\t\t\t.keyedBy(_orders);\n" +
			"\t\tvar _keyedPage =\n" +
			"\t\t\tpageRequest.cursor()\n" +
			"\t\t\t\t\t.map(_cursor -> {\n" +
			"\t\t\t\t\t\t@SuppressWarnings(\"unchecked\")\n" +
			"\t\t\t\t\t\tvar _elements = (List<Comparable<?>>) _cursor.elements();\n" +
			"\t\t\t\t\t\treturn switch (pageRequest.mode()) {\n" +
			"\t\t\t\t\t\t\tcase CURSOR_NEXT -> _unkeyedPage.withKey(_elements, KEY_OF_LAST_ON_PREVIOUS_PAGE);\n" +
			"\t\t\t\t\t\t\tcase CURSOR_PREVIOUS -> _unkeyedPage.withKey(_elements, KEY_OF_FIRST_ON_NEXT_PAGE);\n" +
			"\t\t\t\t\t\t\tdefault -> _unkeyedPage;\n" +
			"\t\t\t\t\t\t};\n" +
			"\t\t\t\t\t}).orElse(_unkeyedPage);";

	void createQuery(StringBuilder declaration, boolean declareVariable) {}

	void createSpecification(StringBuilder declaration) {}

	void setParameters(StringBuilder declaration, List<String> paramTypes) {}

	void inTry(StringBuilder declaration) {
		if ( dataRepository && !isReactive() ) {
			declaration
					.append("\ttry {\n");
		}
	}

	void results(StringBuilder declaration, List<String> paramTypes, @Nullable String containerType) {
		if ( isJakartaCursoredPage(containerType) ) {
			makeKeyedPage( declaration, paramTypes );
		}
		if ( isJakartaCursoredPage(containerType)
				|| isJakartaPage(containerType) ) {
			if ( dataRepository ) {
				declaration
						.append('\t');
			}
			totalResults(declaration, paramTypes);
		}
		if ( dataRepository && !isReactive() ) {
			declaration
					.append('\t');
		}
		declaration
				.append('\t');
		if ( isJakartaCursoredPage(containerType)
				|| isJakartaPage(containerType) && !isReactive() ) {
			if ( isJakartaCursoredPage(containerType) && returnTypeName != null && isUsingEntityHandler() ) {
				// this is necessary to avoid losing the type
				// after unwrapping the Query object
				declaration
						.append(annotationMetaEntity.importType(HIB_KEYED_RESULT_LIST))
						.append('<')
						.append(annotationMetaEntity.importType(returnTypeName))
						.append('>');
			}
			else {
				declaration
						.append("var");
			}
			declaration
					.append(" _results = ");
		}
		else {
			if ( !returnsVoid() || isReactiveSessionAccess() ) {
				returnResult( declaration );
			}
		}
	}

	boolean returnsVoid() {
		return "void".equals( returnTypeName )
			|| isAsynchronousCompletionStageWithVoidResult();
	}

	void select(StringBuilder declaration) {
		declaration
				.append("_select\n");
	}

	void setFirstResultLimit(StringBuilder declaration) {
		final var first = getAnnotationMirror( method, JD_FIRST );
		if ( first != null ) {
			final var value = getAnnotationValue( first );
			declaration
					.append("\t\t\t.setMaxResults(")
					.append(value == null ? 1 : value.getValue())
					.append(")\n");
		}
	}

	private void totalResults(StringBuilder declaration, List<String> paramTypes) {
		declaration
				.append("\tlong _totalResults = \n\t\t\t\t");
		if ( isReactive() ) {
			declaration.append("-1;\n"); //TODO: add getResultCount() to HR
		}
		else {
			declaration
					.append(parameterName(JD_PAGE_REQUEST, paramTypes, paramNames))
					.append(".requestTotal()\n\t\t\t\t\t\t? ");
			select( declaration );
			if ( isUsingEntityHandler() ) {
				declaration
						.append("\t\t\t\t\t");
			}
			unwrapQuery( declaration, !isUsingEntityHandler() );
			declaration
					.append("\t\t\t\t\t\t\t\t.getResultCount()\n\t\t\t\t\t\t: -1;\n");
		}
	}

	void collectOrdering(StringBuilder declaration, List<String> paramTypes, @Nullable String containerType) {
		if ( !hasOrder() ) {
			return;
		}
		final var orderingTypeName = orderingTypeName();
		if ( orderingTypeName == null ) {
			return;
		}
		final var cursoredPage = isJakartaCursoredPage( containerType );
		final String add;
		if ( cursoredPage ) {
			// we need to collect them together in a List
			declaration
					.append("\tvar _orders = new ")
					.append(annotationMetaEntity.importType("java.util.ArrayList"))
					.append("<")
					.append(annotationMetaEntity.importType(HIB_ORDER))
					.append("<? super ")
					.append(annotationMetaEntity.importType(orderingTypeName))
					.append(">>();\n");
			add = "_orders.add";
		}
		else {
			add = "_spec.sort";
		}

		// static orders declared using @OrderBy must come first
		for ( var orderBy : orderBys ) {
			annotationMetaEntity.staticImport(HIB_SORT_DIRECTION, "*");
			collectStaticOrder( declaration, add, orderBy, orderingTypeName );
		}
		for ( int i = 0; i < paramTypes.size(); i++ ) {
			collectOrderingParameter(
					declaration,
					paramTypes.get( i ),
					paramNames.get( i ),
					add,
					orderingTypeName,
					cursoredPage );
		}
	}

	private void collectOrderingParameter(
			StringBuilder declaration,
			String type,
			String name,
			String add,
			String orderingTypeName,
			boolean cursoredPage) {
		if ( type.startsWith(HIB_ORDER) && type.endsWith("...") ) {
			collectOrders( declaration, name, add );
		}
		else if ( type.startsWith(HIB_ORDER) ) {
			collectOrder( declaration, add, name, "\t" );
		}
		else if ( type.startsWith(LIST + "<" + HIB_ORDER) ) {
			if ( cursoredPage ) {
				collectAllOrders( declaration, name );
			}
			else {
				collectOrders( declaration, name, add );
			}
		}
		else if ( type.startsWith(JD_ORDER) ) {
			collectJakartaDataSorts( declaration, name + ".sorts()", add, orderingTypeName );
		}
		else if ( type.startsWith(JD_SORT) && type.endsWith("...") ) {
			collectJakartaDataSorts( declaration, name, add, orderingTypeName );
		}
		else if ( type.startsWith(JD_SORT) ) {
			collectJakartaDataSort( declaration, name, add, orderingTypeName );
		}
	}

	private void collectStaticOrder(
			StringBuilder declaration,
			String add,
			OrderBy orderBy,
			String orderingTypeName) {
		declaration
				.append("\t")
				.append(add)
				.append('(');
		staticOrderExpression( declaration, orderBy, orderingTypeName );
		declaration.append(");\n");
	}

	private static void collectOrder(StringBuilder declaration, String add, String order, String indent) {
		declaration
				.append(indent)
				.append(add)
				.append('(')
				.append(order)
				.append(");\n");
	}

	private static void collectOrders(StringBuilder declaration, String orders, String add) {
		forEach( declaration, orders, "\t", "_sort" );
		collectOrder( declaration, add, "_sort", "\t\t" );
		endForEach( declaration, "\t" );
	}

	private static void collectAllOrders(StringBuilder declaration, String orders) {
		declaration
				.append("\t_orders.addAll(")
				.append(orders)
				.append(");\n");
	}

	private void collectJakartaDataSorts(
			StringBuilder declaration,
			String sorts,
			String add,
			String orderingTypeName) {
		annotationMetaEntity.staticImport(HIB_SORT_DIRECTION, "*");
		forEach( declaration, sorts, "\t", "_sort" );
		declaration
				.append("\t\t")
				.append(add)
				.append('(');
		jakartaDataSort( declaration, "_sort", orderingTypeName );
		declaration.append(");\n");
		endForEach( declaration, "\t" );
	}

	private void collectJakartaDataSort(
			StringBuilder declaration,
			String sort,
			String add,
			String orderingTypeName) {
		annotationMetaEntity.staticImport(HIB_SORT_DIRECTION, "*");
		declaration
				.append("\t")
				.append(add)
				.append('(');
		jakartaDataSort( declaration, sort, orderingTypeName );
		declaration.append(");\n");
	}

	private void jakartaDataSort(
			StringBuilder declaration,
			String sort,
			String orderingTypeName) {
		declaration
				.append(annotationMetaEntity.staticImport(HIB_ORDER, "asc"))
				.append('(')
				.append(annotationMetaEntity.importType(orderingTypeName))
				.append(".class, ")
				.append(sort)
				.append(".property())")
				.append("\n\t\t\t\t\t")
				.append(".reversedIf(")
				.append(sort)
				.append(".isDescending())")
				.append("\n\t\t\t\t\t")
				.append(".ignoringCaseIf(")
				.append(sort)
				.append(".ignoreCase())");
	}

	void applyCriteriaOrdering(
			StringBuilder declaration,
			List<String> paramTypes,
			String indent,
			String orderingTypeName) {
		applyCriteriaOrdering( declaration, paramTypes, indent, orderingTypeName, true );
	}

	void applyCriteriaOrdering(
			StringBuilder declaration,
			List<String> paramTypes,
			String indent,
			String orderingTypeName,
			boolean includeHibernateOrders) {
		if ( !hasCriteriaOrdering( paramTypes, includeHibernateOrders ) ) {
			return;
		}
		for ( var orderBy : orderBys ) {
			applyStaticCriteriaOrder( declaration, orderBy, indent, orderingTypeName );
		}
		for ( int i = 0; i < paramNames.size(); i++ ) {
			applyCriteriaOrderParameter(
					declaration,
					paramTypes.get( i ),
					parameterVariableName( i ),
					indent,
					includeHibernateOrders );
		}
	}

	private boolean hasCriteriaOrdering(List<String> paramTypes, boolean includeHibernateOrders) {
		return !orderBys.isEmpty()
			|| paramTypes.stream()
					.anyMatch( type -> includeHibernateOrders
							? isOrderParam( type )
							: isJakartaDataOrderingParam( type ) );
	}

	private void applyCriteriaOrderParameter(
			StringBuilder declaration,
			String paramType,
			String paramName,
			String indent,
			boolean includeHibernateOrders) {
		if ( includeHibernateOrders && isHibernateOrderSequence( paramType ) ) {
			applyHibernateCriteriaOrderingSequence( declaration, paramName, indent );
		}
		else if ( includeHibernateOrders && paramType.startsWith( HIB_ORDER ) ) {
			applyHibernateCriteriaOrdering( declaration, paramName, indent );
		}
		else if ( paramType.startsWith( JD_ORDER ) ) {
			applyCriteriaOrdering( declaration, "applyOrder", paramName, indent );
		}
		else if ( isJakartaDataSortSequence( paramType ) ) {
			applyCriteriaOrderingSequence( declaration, "applySort", paramName, indent );
		}
		else if ( paramType.startsWith( JD_SORT ) ) {
			applyCriteriaOrdering( declaration, "applySort", paramName, indent );
		}
	}

	private static boolean isHibernateOrderSequence(String paramType) {
		return paramType.startsWith( LIST + "<" + HIB_ORDER )
			|| paramType.startsWith( HIB_ORDER ) && ( paramType.endsWith( "..." ) || paramType.endsWith( "[]" ) );
	}

	private static boolean isJakartaDataOrderingParam(String paramType) {
		return paramType.startsWith( JD_ORDER )
			|| paramType.startsWith( JD_SORT );
	}

	private static boolean isJakartaDataSortSequence(String paramType) {
		return paramType.startsWith( JD_SORT )
			&& ( paramType.endsWith( "..." ) || paramType.endsWith( "[]" ) );
	}

	private static void forEach(
			StringBuilder declaration,
			String source,
			String indent,
			String variable) {
		declaration
				.append( indent )
				.append( "for (var " )
				.append( variable )
				.append( " : " )
				.append( source )
				.append( ") {\n" );
	}

	private static void endForEach(StringBuilder declaration, String indent) {
		declaration
				.append( indent )
				.append( "}\n" );
	}

	private void applyStaticCriteriaOrder(
			StringBuilder declaration,
			OrderBy orderBy,
			String indent,
			String orderingTypeName) {
		declaration
				.append( indent )
				.append( annotationMetaEntity.staticImport(
						HIB_JAKARTA_DATA_RESTRICTION, "applySort" ) )
				.append( "(" );
		staticSortExpression( declaration, orderBy, orderingTypeName );
		declaration.append( ", _query, _entity, _builder);\n" );
	}

	private void staticSortExpression(
			StringBuilder declaration,
			OrderBy orderBy,
			String orderingTypeName) {
		staticDataMetamodelAttribute( declaration, orderBy, orderingTypeName );
		declaration
				.append( '.' )
				.append( sortMethod( orderBy ) )
				.append( "()" );
	}

	private void staticDataMetamodelAttribute(
			StringBuilder declaration,
			OrderBy orderBy,
			String orderingTypeName) {
		final var typeElement =
				annotationMetaEntity.getContext().getElementUtils()
						.getTypeElement( orderingTypeName );
		declaration
				.append( annotationMetaEntity.importType(
						getGeneratedClassFullyQualifiedName( typeElement, true ) ) )
				.append( '.' )
				.append( orderBy.fieldName.replace( '.', '_' ) );
	}

	private static String sortMethod(OrderBy orderBy) {
		if ( orderBy.descending ) {
			return orderBy.ignoreCase ? "descIgnoreCase" : "desc";
		}
		else {
			return orderBy.ignoreCase ? "ascIgnoreCase" : "asc";
		}
	}

	private void staticOrderExpression(
			StringBuilder declaration,
			OrderBy orderBy,
			String orderingTypeName) {
		declaration
				.append( annotationMetaEntity.staticImport( HIB_ORDER, orderBy.descending ? "desc" : "asc" ) )
				.append( "(" )
				.append( annotationMetaEntity.importType( orderingTypeName ) )
				.append( ".class, \"" )
				.append( orderBy.fieldName )
				.append( "\")" );
		if ( orderBy.ignoreCase ) {
			declaration.append( ".ignoringCase()" );
		}
	}

	private void applyCriteriaOrderingSequence(
			StringBuilder declaration,
			String operation,
			String ordering,
			String indent) {
		forEach( declaration, ordering, indent, "_sort" );
		applyCriteriaOrdering( declaration, operation, "_sort", indent + '\t' );
		endForEach( declaration, indent );
	}

	private static void applyHibernateCriteriaOrderingSequence(
			StringBuilder declaration,
			String ordering,
			String indent) {
		forEach( declaration, ordering, indent, "_sort" );
		applyHibernateCriteriaOrdering( declaration, "_sort", indent + '\t' );
		endForEach( declaration, indent );
	}

	private static void applyHibernateCriteriaOrdering(
			StringBuilder declaration,
			String ordering,
			String indent) {
		declaration
				.append( indent )
				.append( ordering )
				.append( ".apply(_query, _entity, _builder);\n" );
	}

	private void applyCriteriaOrdering(
			StringBuilder declaration,
			String operation,
			String ordering,
			String indent) {
		declaration
				.append( indent )
				.append( annotationMetaEntity.staticImport(
						HIB_JAKARTA_DATA_RESTRICTION, operation ) )
				.append( "(" )
				.append( ordering )
				.append( ", _query, _entity, _builder);\n" );
	}

	boolean isUsingSpecification() {
		return false;
	}

	protected void executeSelect(
			StringBuilder declaration,
			List<String> paramTypes,
			@Nullable String containerType,
			boolean unwrapped,
			boolean mustUnwrap) {
		if ( containerType == null ) {
			if ( nullable ) {
				declaration
						.append("\t\t\t.getSingleResultOrNull()");
			}
			else {
				declaration
						.append("\t\t\t.getSingleResult()");
			}
			endReturnResult( declaration );
		}
		else {
			switch (containerType) {
				case "[]":
					if ( returnTypeName== null ) {
						throw new AssertionFailure("missing return type");
					}
					else {
						declaration
								.append("\t\t\t.getResultList()\n\t\t\t.toArray(new ")
								.append(annotationMetaEntity.importType(returnTypeName))
								.append("[0])");
					}
					endReturnResult( declaration );
					break;
				case OPTIONAL:
					unwrapQuery(declaration, unwrapped);
					declaration
							.append("\t\t\t.uniqueResultOptional()");
					endReturnResult( declaration );
					break;
				case STREAM:
					declaration
							.append("\t\t\t.getResultStream()");
					endReturnResult( declaration );
					break;
				case LIST:
					declaration
							.append("\t\t\t.getResultList()");
					endReturnResult( declaration );
					break;
				case HIB_KEYED_RESULT_LIST:
					unwrapQuery(declaration, unwrapped);
					declaration
							.append("\t\t\t.getKeyedResultList(")
							.append(parameterName(HIB_KEYED_PAGE, paramTypes, paramNames))
							.append(")");
					endReturnResult( declaration );
					break;
				case JD_PAGE:
					if ( isReactive() ) {
						if ( returnTypeName == null ) {
							throw new AssertionFailure("entity class cannot be null");
						}
						declaration
								.append("\t\t\t.getResultList()\n")
								.append("\t\t\t.map(_results -> (Page<")
								.append(annotationMetaEntity.importType(returnTypeName))
								.append(">)");
					}
					else {
						declaration
								.append("\t\t\t.getResultList();\n")
								.append("\t\t");
						returnResult( declaration );
					}
					declaration
							.append("new ")
							.append(annotationMetaEntity.importType("jakarta.data.page.impl.PageRecord"))
							.append("<>")
							.append('(')
							.append(parameterName(JD_PAGE_REQUEST, paramTypes, paramNames))
							.append(", _results, _totalResults)");
					if ( isReactive() ) {
						declaration
								.append(')');
					}
					endReturnResult( declaration );
					break;
				case JD_CURSORED_PAGE:
					if ( returnTypeName == null ) {
						throw new AssertionFailure("entity class cannot be null");
					}
					else {
						unwrapQuery(declaration, unwrapped);
						declaration
								.append("\t\t\t.getKeyedResultList(_keyedPage);\n");
						annotationMetaEntity.importType(JD_PAGE_REQUEST);
						annotationMetaEntity.importType(JD_PAGE_REQUEST + ".Cursor");
						annotationMetaEntity.importType("jakarta.data.page.impl.CursoredPageRecord");
						annotationMetaEntity.staticImport(JD_PAGE_REQUEST, "beforeCursor");
						annotationMetaEntity.staticImport(JD_PAGE_REQUEST, "afterCursor");
						var fragment = MAKE_KEYED_SLICE
								.replace("pageRequest",
										parameterName(JD_PAGE_REQUEST, paramTypes, paramNames))
								.replace("Entity",
										annotationMetaEntity.importType(returnTypeName));
						if ( isAsynchronousCompletionStage() ) {
							fragment = fragment.replace( "\t\treturn ", "\t\treturn "
									+ annotationMetaEntity.staticImport( COMPLETABLE_FUTURE, "completedStage" )
									+ "(" );
						}
						declaration
								.append(fragment);
						endReturnResult( declaration );
					}
					break;
				default:
					if ( isUsingEntityHandler() && !unwrapped && mustUnwrap ) {
						declaration
								.append("\t\t\t.unwrap(")
								.append(annotationMetaEntity.importType(containerType))
								.append(".class)");

					}
					else {
						final var lastIndex = declaration.length() - 1;
						if ( declaration.charAt(lastIndex) == '\n' )  {
							declaration.setLength(lastIndex);
						}
					}
					endReturnResult( declaration );
			}
		}
	}

	static String parameterName(String paramType, List<String> paramTypes, List<String> paramNames) {
		for (int i = 0; i < paramTypes.size(); i++) {
			if ( paramTypes.get(i).startsWith(paramType) ) {
				return paramNames.get(i);
			}
		}
		throw new AssertionFailure("could not find parameter");
	}

	private static final Set<String> UNSPECIALIZED_QUERY_TYPES
			= Set.of(QUERY, TYPED_QUERY, HIB_QUERY);

	static boolean isUnspecializedQueryType(@Nullable String containerType) {
		return containerType != null
			&& UNSPECIALIZED_QUERY_TYPES.contains(containerType);
	}

	static boolean isHibernateQueryType(@Nullable String containerType) {
		return containerType != null
			&& containerType.startsWith("org.hibernate");
	}

	boolean isUnifiableReturnType(@Nullable String containerType) {
		return containerType == null
			|| LIST.equals(containerType)
			|| JD_PAGE.equals(containerType)
			|| JD_CURSORED_PAGE.equals(containerType);
	}
}
