/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import jakarta.annotation.Nullable;
import org.hibernate.AssertionFailure;

import javax.lang.model.element.ExecutableElement;
import java.util.List;
import java.util.StringTokenizer;

import static org.hibernate.metamodel.mapping.EntityIdentifierMapping.ID_ROLE_NAME;
import static org.hibernate.processor.util.Constants.HIB_JAKARTA_DATA_RESTRICTION;
import static org.hibernate.processor.util.Constants.JD_PAGE;
import static org.hibernate.processor.util.Constants.JD_PAGE_REQUEST;
import static org.hibernate.processor.util.Constants.LIST;
import static org.hibernate.processor.util.Constants.OPTIONAL;
import static org.hibernate.processor.util.Constants.STREAM;
import static org.hibernate.processor.util.TypeUtils.isPrimitive;

/**
 * @author Gavin King
 */
public abstract class AbstractCriteriaMethod extends AbstractFinderMethod {

	private final List<ParameterConstraint> parameterConstraints;
	private final @Nullable ResultSelection selection;

	public AbstractCriteriaMethod(
			AnnotationMetaEntity annotationMetaEntity,
			ExecutableElement method,
			String methodName, String entity,
			@Nullable ResultSelection selection,
			@Nullable String containerType,
			boolean belongsToDao,
			String sessionType, String sessionName,
			List<String> fetchProfiles,
			List<String> paramNames,
			List<String> paramTypes,
			List<OrderBy> orderBys,
			boolean addNonnullAnnotation,
			boolean convertToDataExceptions,
			List<ParameterConstraint> parameterConstraints,
			String fullReturnType,
			boolean nullable) {
		super(annotationMetaEntity, method, methodName, entity, selection == null ? entity : selection.resultTypeName(),
				containerType, belongsToDao, sessionType, sessionName,
				fetchProfiles, paramNames, paramTypes, orderBys, addNonnullAnnotation, convertToDataExceptions,
				fullReturnType, nullable);
		this.parameterConstraints = parameterConstraints;
		this.selection = selection;
	}

	@Override
	public String getAttributeDeclarationString() {
		final List<String> paramTypes = parameterTypes();
		if ( usesProjectionSpecification() ) {
			return getProjectionSpecificationAttributeDeclarationString( paramTypes );
		}
		final StringBuilder declaration = new StringBuilder();
		comment( declaration );
		modifiers( declaration );
		preamble( declaration, paramTypes );
		chainSession( declaration );
		nullChecks( declaration, paramTypes );
		createBuilder(declaration);
		createCriteriaQuery( declaration );
		where( declaration, paramTypes );
		executeQuery( declaration, paramTypes );
		convertExceptions( declaration );
		chainSessionEnd( false, declaration );
		closingBrace( declaration );
		return declaration.toString();
	}

	private String getProjectionSpecificationAttributeDeclarationString(List<String> paramTypes) {
		final StringBuilder declaration = new StringBuilder();
		comment( declaration );
		modifiers( declaration );
		preamble( declaration, paramTypes );
		chainSession( declaration );
		nullChecks( declaration, paramTypes );
		createEntitySpecification( declaration );
		augmentSpecification( declaration, paramTypes );
		handleRestrictionParameters( declaration, paramTypes );
		collectOrdering( declaration, paramTypes, containerType );
		createProjectionSpecification( declaration );
		inTry( declaration );
		createQuery( declaration, true );
		QueryOptionsSupport.setQueryOptions( this, declaration, false, false );
		declaration.append( ";\n" );
		executeProjectionQuery( declaration, paramTypes );
		convertExceptions( declaration );
		chainSessionEnd( false, declaration );
		closingBrace( declaration );
		return declaration.toString();
	}

	abstract void executeQuery(StringBuilder declaration, List<String> paramTypes);

	abstract String createCriteriaMethod();

	abstract String createQueryMethod();

	String specificationType() {
		return "org.hibernate.query.specification.SelectionSpecification";
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
			declaration
					.append("var _select = ");
		}
		if ( usesProjectionSpecification() ) {
			declaration
					.append("_projection.createQuery(");
			localSession( declaration );
			declaration
					.append(")");
		}
		else if ( useSpecificationCreateQuery() ) {
			declaration
					.append("_spec.createQuery(");
			localSession( declaration );
			declaration
					.append(")");
		}
		else {
			localSession( declaration );
			declaration
					.append(".")
					.append(createQueryMethod())
					.append('(');
			if ( isUsingSpecification() ) {
				declaration
						.append("_spec.buildCriteria(_builder)");
			}
			else {
				declaration.append("_query");
			}
			declaration.append( ")" );
		}
	}

	@Override
	void createSpecification(StringBuilder declaration) {
		if ( isUsingSpecification() ) {
			declaration
					.append( "\tvar _spec = " )
					.append( annotationMetaEntity.importType( specificationType() ) )
					.append( ".create(_query);\n" );
		}
	}

	@Override
	boolean isUsingSpecification() {
		return hasRestriction()
			|| hasOrder() && !isJakartaCursoredPage(containerType);
	}

	private boolean usesProjectionSpecification() {
		return selection != null
			&& useSpecificationCreateQuery()
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

	private void createEntitySpecification(StringBuilder declaration) {
		declaration
				.append( "\tvar _spec = " )
				.append( annotationMetaEntity.importType( specificationType() ) )
				.append( ".create(" )
				.append( annotationMetaEntity.importType( entity ) )
				.append( ".class);\n" );
	}

	private void augmentSpecification(StringBuilder declaration, List<String> paramTypes) {
		if ( hasFinderParameters( paramTypes ) ) {
			declaration
					.append( "\t_spec.augment((_builder, _query, _entity) -> _query.where(" );
			wherePredicates( declaration, paramTypes );
			declaration
					.append( "\n\t));\n" );
		}
	}

	private static boolean hasFinderParameters(List<String> paramTypes) {
		for ( String paramType : paramTypes ) {
			if ( !isSpecialParam( paramType ) ) {
				return true;
			}
		}
		return false;
	}

	private void createProjectionSpecification(StringBuilder declaration) {
		if ( selection == null ) {
			throw new AssertionFailure( "missing result selection" );
		}
		declaration
				.append( "\tvar _projection = " )
				.append( annotationMetaEntity.importType( "org.hibernate.query.specification.ProjectionSpecification" ) )
				.append( ".create(_spec);\n" );
		for ( int i = 0; i < selection.paths().size(); i++ ) {
			declaration
					.append( "\tvar " )
					.append( selectionVariableName( i ) )
					.append( " = _projection.select(" );
			projectionSelection( declaration, selection.paths().get( i ) );
			declaration
					.append( ");\n" );
		}
	}

	void createCriteriaQuery(StringBuilder declaration) {
		final String entityClass = annotationMetaEntity.importType(entity);
		final String resultClass =
				annotationMetaEntity.importType(selection == null ? entity : selection.resultTypeName());
		declaration
				.append("\tvar _query = _builder.")
				.append(createCriteriaMethod())
				.append('(')
				.append(resultClass)
				.append(".class);\n")
				.append("\tvar _entity = _query.from(")
				.append(entityClass)
				.append(".class);\n");
		if ( selection != null ) {
			select( declaration, selection );
		}
	}

	private void select(StringBuilder declaration, ResultSelection selection) {
		declaration
				.append("\t_query.select(");
		if ( selection.recordProjection() ) {
			declaration
					.append("_builder.construct(")
					.append(annotationMetaEntity.importType(selection.resultTypeName()))
					.append(".class");
			for ( String path : selection.paths() ) {
				declaration
						.append(", ");
				selectionExpression( declaration, path, entity );
			}
			declaration
					.append(")");
		}
		else {
			selectionExpression( declaration, selection.paths().get( 0 ), entity );
		}
		declaration
				.append(");\n");
	}

	private void createBuilder(StringBuilder declaration) {
		declaration
				.append("\tvar _builder = ");
		localSession( declaration );
		declaration
				.append(".getCriteriaBuilder();\n");
	}

	@Override
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
					.append(parameterVariableName(i));
		}
		declaration
				.append(")");
	}

	void nullChecks(StringBuilder declaration, List<String> paramTypes) {
		for ( int i = 0; i<paramNames.size(); i++ ) {
			if ( isNonNull(i, paramTypes) ) {
				nullCheck( declaration, parameterVariableName(i) );
			}
		}
	}

	void where(StringBuilder declaration, List<String> paramTypes) {
		if ( !hasNonSpecialParams( paramTypes ) ) {
			return;
		}
		declaration
				.append("\t_query.where(");
		wherePredicates( declaration, paramTypes );
		declaration
				.append("\n\t);");
	}

	void applyOrderingParameters(
			StringBuilder declaration,
			List<String> paramTypes,
			@Nullable String containerType) {
		if ( isJakartaCursoredPage( containerType ) ) {
			collectOrdering( declaration, paramTypes, containerType );
		}
		else {
			applyCriteriaOrdering( declaration, paramTypes, "\t", entity );
		}
	}

	private boolean hasNonSpecialParams(List<String> paramTypes) {
		for ( int i = 0; i < paramNames.size(); i++ ) {
			if ( !isSpecialParam( paramTypes.get( i ) ) ) {
				return true;
			}
		}
		return false;
	}

	private void wherePredicates(StringBuilder declaration, List<String> paramTypes) {
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
	}

	private void condition(StringBuilder declaration, int i, String paramName, String paramType) {
		declaration
				.append("\n\t\t\t");
		final String parameterName = parameterVariableName(i);
		final ParameterConstraint parameterConstraint = parameterConstraints.get(i);
		if ( isNullable(i) && !isPrimitive(paramType) ) {
			declaration
					.append(parameterName)
					.append("==null")
					.append("\n\t\t\t\t? ")
					.append("_entity");
			path( declaration, paramName, entity );
			declaration
					.append(".isNull()")
					.append("\n\t\t\t\t: ");
		}
		if ( parameterConstraint == ParameterConstraint.RUNTIME ) {
			declaration
					.append( annotationMetaEntity.staticImport(
							HIB_JAKARTA_DATA_RESTRICTION, "applyConstraint" ) )
					.append( "(_entity" );
			path( declaration, paramName, entity );
			declaration
					.append( ", " )
					.append( parameterName )
					.append( ", _entity, _builder)" );
		}
		else if ( parameterConstraint.isMultivalued() ) {
			if ( parameterConstraint == ParameterConstraint.NOT_IN ) {
				declaration
						.append( "_builder.not(" );
			}
			declaration
					.append("_entity");
			path( declaration, paramName, entity );
			declaration
					.append(".in(");
			if ( paramType.endsWith("[]") ) {
				declaration
						.append("(Object[]) ");

			}
			declaration
					//TODO: only safe if we are binding literals as parameters!!!
					.append(parameterName)
					.append(")");
			if ( parameterConstraint == ParameterConstraint.NOT_IN ) {
				declaration
						.append( ")" );
			}
		}
		else {
			//TODO: change to use Expression.equalTo() in JPA 3.2
			declaration
					.append("_builder.")
					.append(criteriaBuilderMethod( parameterConstraint ))
					.append("(_entity");
			path( declaration, paramName, entity );
			declaration
					.append(", ")
					//TODO: only safe if we are binding literals as parameters!!!
					.append(parameterName)
					.append(')');
		}
	}

	private static String criteriaBuilderMethod(ParameterConstraint parameterConstraint) {
		return switch ( parameterConstraint ) {
			case EQUAL -> "equal";
			case NOT_EQUAL -> "notEqual";
			case GREATER_THAN -> "greaterThan";
			case AT_LEAST -> "greaterThanOrEqualTo";
			case LESS_THAN -> "lessThan";
			case AT_MOST -> "lessThanOrEqualTo";
			case LIKE -> "like";
			case NOT_LIKE -> "notLike";
			case IN, NOT_IN, RUNTIME ->
					throw new IllegalArgumentException( "Unexpected parameter constraint: " + parameterConstraint );
		};
	}

	@Override
	String parameterVariableName(int index) {
		final String baseName = parameterName( paramNames.get(index) );
		int collisions = 0;
		for ( int i = 0; i < index; i++ ) {
			if ( parameterName( paramNames.get(i) ).equals( baseName ) ) {
				collisions++;
			}
		}
		return collisions == 0 ? baseName : baseName + (collisions + 1);
	}

	private void executeProjectionQuery(StringBuilder declaration, List<String> paramTypes) {
		final String indent = dataRepository && !isReactive() ? "\t\t" : "\t";
		if ( JD_PAGE.equals( containerType ) ) {
			projectionTotalResults( declaration, indent, paramTypes );
			declaration
					.append( indent )
					.append( "var _results = " );
			selectProjectionResultList( declaration, paramTypes );
			declaration
					.append( ";\n" )
					.append( indent );
			returnResult( declaration );
			declaration
					.append( "new " )
					.append( annotationMetaEntity.importType( "jakarta.data.page.impl.PageRecord" ) )
					.append( "<>(" )
					.append( parameterName(JD_PAGE_REQUEST, paramTypes, paramNames) )
					.append( ", _results, _totalResults)" );
			endReturnResult( declaration );
		}
		else if ( containerType == null ) {
			declaration
					.append( indent )
					.append( "var _result = " );
			select( declaration );
			setFirstResultLimit( declaration );
			handlePageParameters( declaration, paramTypes, null );
			declaration
					.append( nullable ? "\t\t\t.getSingleResultOrNull();\n" : "\t\t\t.getSingleResult();\n" )
					.append( indent );
			returnResult( declaration );
			if ( nullable ) {
				declaration.append( "_result == null ? null : " );
			}
			projectionResult( declaration, "_result" );
			endReturnResult( declaration );
		}
		else {
			declaration
					.append( indent );
			returnResult( declaration );
			selectProjectionResult( declaration, paramTypes );
			endReturnResult( declaration );
		}
	}

	private void projectionTotalResults(StringBuilder declaration, String indent, List<String> paramTypes) {
		declaration
				.append( indent )
				.append( "long _totalResults = \n" )
				.append( indent )
				.append( "\t\t" )
				.append( parameterName(JD_PAGE_REQUEST, paramTypes, paramNames) )
				.append( ".requestTotal()\n" )
				.append( indent )
				.append( "\t\t\t\t? _select\n" )
				.append( indent )
				.append( "\t\t\t\t\t\t.getResultCount()\n" )
				.append( indent )
				.append( "\t\t\t\t: -1;\n" );
	}

	private void selectProjectionResult(StringBuilder declaration, List<String> paramTypes) {
		final String containerType = this.containerType;
		if ( containerType == null ) {
			throw new AssertionFailure( "missing container type" );
		}
		switch ( containerType ) {
			case "[]":
				select( declaration );
				setFirstResultLimit( declaration );
				handlePageParameters( declaration, paramTypes, containerType );
				declaration
						.append( "\t\t\t.getResultList()\n" )
						.append( "\t\t\t.stream()\n" )
						.append( "\t\t\t.map(_result -> " );
				projectionResult( declaration, "_result" );
				declaration
						.append( ")\n" )
						.append( "\t\t\t.toArray(" )
						.append( annotationMetaEntity.importType( resultTypeName() ) )
						.append( "[]::new)" );
				break;
			case OPTIONAL:
				select( declaration );
				setFirstResultLimit( declaration );
				handlePageParameters( declaration, paramTypes, containerType );
				declaration
						.append( "\t\t\t.uniqueResultOptional()\n" )
						.append( "\t\t\t.map(_result -> " );
				projectionResult( declaration, "_result" );
				declaration.append( ")" );
				break;
			case STREAM:
				select( declaration );
				setFirstResultLimit( declaration );
				handlePageParameters( declaration, paramTypes, containerType );
				declaration
						.append( "\t\t\t.getResultStream()\n" )
						.append( "\t\t\t.map(_result -> " );
				projectionResult( declaration, "_result" );
				declaration.append( ")" );
				break;
			case LIST:
				selectProjectionResultList( declaration, paramTypes );
				break;
			default:
				throw new AssertionFailure( "unsupported projection container type: " + containerType );
		}
	}

	private void selectProjectionResultList(StringBuilder declaration, List<String> paramTypes) {
		select( declaration );
		setFirstResultLimit( declaration );
		handlePageParameters( declaration, paramTypes, containerType );
		declaration
				.append( "\t\t\t.getResultList()\n" )
				.append( "\t\t\t.stream()\n" )
				.append( "\t\t\t.map(_result -> " );
		projectionResult( declaration, "_result" );
		declaration
				.append( ")\n" )
				.append( "\t\t\t.toList()" );
	}

	private String resultTypeName() {
		if ( returnTypeName == null ) {
			throw new AssertionFailure( "missing return type" );
		}
		return returnTypeName;
	}

	private void projectionResult(StringBuilder declaration, String resultVariable) {
		if ( selection == null ) {
			throw new AssertionFailure( "missing result selection" );
		}
		if ( selection.recordProjection() ) {
			declaration
					.append( "new " )
					.append( annotationMetaEntity.importType( selection.resultTypeName() ) )
					.append( "(" );
			for ( int i = 0; i < selection.paths().size(); i++ ) {
				if ( i > 0 ) {
					declaration.append( ", " );
				}
				selectionValue( declaration, i, resultVariable );
			}
			declaration.append( ")" );
		}
		else {
			selectionValue( declaration, 0, resultVariable );
		}
	}

	private void selectionValue(StringBuilder declaration, int selectionIndex, String resultVariable) {
		declaration
				.append( selectionVariableName( selectionIndex ) )
				.append( ".in(" )
				.append( resultVariable )
				.append( ")" );
	}

	private String selectionVariableName(int selectionIndex) {
		if ( selection == null ) {
			throw new AssertionFailure( "missing result selection" );
		}
		else if ( selection.recordProjection() ) {
			return "_" + unqualifiedName( selection.resultTypeName() )
					+ "_" + selection.componentNames().get( selectionIndex );
		}
		else {
			return "_selection" + selectionIndex;
		}
	}

	private static String unqualifiedName(String name) {
		return name.substring( Math.max( name.lastIndexOf( '.' ), name.lastIndexOf( '$' ) ) + 1 );
	}

	private void projectionSelection(StringBuilder declaration, String path) {
		final StringTokenizer tokens = new StringTokenizer(path, ".");
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
				typeName = annotationMetaEntity.getMemberType(typeName, memberName);
			}
		}
	}

}
