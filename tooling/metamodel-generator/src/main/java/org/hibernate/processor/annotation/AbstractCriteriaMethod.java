/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.StringTokenizer;

import static org.hibernate.metamodel.mapping.EntityIdentifierMapping.ID_ROLE_NAME;
import static org.hibernate.processor.util.TypeUtils.getGeneratedClassFullyQualifiedName;
import static org.hibernate.processor.util.TypeUtils.isPrimitive;

/**
 * @author Gavin King
 */
public abstract class AbstractCriteriaMethod extends AbstractFinderMethod {

	private final List<ParameterConstraint> parameterConstraints;

	public AbstractCriteriaMethod(
			AnnotationMetaEntity annotationMetaEntity,
			ExecutableElement method,
			String methodName, String entity,
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
		super(annotationMetaEntity, method, methodName, entity, containerType, belongsToDao, sessionType, sessionName,
				fetchProfiles, paramNames, paramTypes, orderBys, addNonnullAnnotation, convertToDataExceptions,
				fullReturnType, nullable);
		this.parameterConstraints = parameterConstraints;
	}

	@Override
	public String getAttributeDeclarationString() {
		final List<String> paramTypes = parameterTypes();
		final StringBuilder declaration = new StringBuilder();
		comment( declaration );
		modifiers( declaration );
		preamble( declaration, paramTypes );
		chainSession( declaration );
		nullChecks( declaration, paramTypes );
		createBuilder(declaration);
		createCriteriaQuery( declaration );
		where( declaration, paramTypes );
//		orderBy( paramTypes, declaration );
		executeQuery( declaration, paramTypes );
		convertExceptions( declaration );
		chainSessionEnd( false, declaration );
		closingBrace( declaration );
		return declaration.toString();
	}

	abstract void executeQuery(StringBuilder declaration, List<String> paramTypes);

	abstract String createCriteriaMethod();

//	abstract String returnType();

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
		if ( useSpecificationCreateQuery() ) {
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

	void createCriteriaQuery(StringBuilder declaration) {
		final String entityClass = annotationMetaEntity.importType(entity);
		declaration
				.append("\tvar _query = _builder.")
				.append(createCriteriaMethod())
				.append('(')
				.append(entityClass)
				.append(".class);\n")
				.append("\tvar _entity = _query.from(")
				.append(entityClass)
				.append(".class);\n");
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
		final String parameterName = parameterVariableName(i);
		final ParameterConstraint parameterConstraint = parameterConstraints.get(i);
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
		if ( parameterConstraint == ParameterConstraint.RUNTIME ) {
			declaration
					.append( annotationMetaEntity.importType( "org.hibernate.query.restriction.JakartaDataRestriction" ) )
					.append( ".predicate(_entity" );
			path( declaration, paramName );
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
			path( declaration, paramName );
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
			path( declaration, paramName );
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

	private String parameterVariableName(int index) {
		final String baseName = parameterName( paramNames.get(index) );
		int collisions = 0;
		for ( int i = 0; i < index; i++ ) {
			if ( parameterName( paramNames.get(i) ).equals( baseName ) ) {
				collisions++;
			}
		}
		return collisions == 0 ? baseName : baseName + (collisions + 1);
	}

	private void path(StringBuilder declaration, String paramName) {
		final StringTokenizer tokens = new StringTokenizer(paramName, ".");
		String typeName = entity;
		while ( typeName != null && tokens.hasMoreTokens() ) {
			final TypeElement typeElement =
					annotationMetaEntity.getContext().getElementUtils()
							.getTypeElement( typeName );
			final String memberName = tokens.nextToken();
			declaration
					.append( ".get(" );
			if ( ID_ROLE_NAME.equals(memberName) ) {
				declaration
						.append( '"' )
						.append( memberName )
						.append( '"' );
			}
			else {
				declaration
						.append( annotationMetaEntity.importType(
								getGeneratedClassFullyQualifiedName( typeElement, false ) ) )
						.append( '.' )
						.append( memberName );
			}
			declaration.append( ')' );
			typeName = annotationMetaEntity.getMemberType(typeName, memberName);
		}
	}

}
