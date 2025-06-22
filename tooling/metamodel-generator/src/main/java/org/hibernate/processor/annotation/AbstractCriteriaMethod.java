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

	private final List<Boolean> multivalued;
	private final List<Boolean> paramPatterns;

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
			List<Boolean> multivalued,
			List<Boolean> paramPatterns,
			String fullReturnType,
			boolean nullable) {
		super(annotationMetaEntity, method, methodName, entity, containerType, belongsToDao, sessionType, sessionName,
				fetchProfiles, paramNames, paramTypes, orderBys, addNonnullAnnotation, convertToDataExceptions,
				fullReturnType, nullable);
		this.multivalued = multivalued;
		this.paramPatterns = paramPatterns;
	}

	@Override
	public String getAttributeDeclarationString() {
		final List<String> paramTypes = parameterTypes();
		final StringBuilder declaration = new StringBuilder();
		comment( declaration );
		modifiers( declaration );
		preamble( declaration, paramTypes );
		chainSession( declaration );
		nullChecks( paramTypes, declaration );
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
	void createQuery(StringBuilder declaration) {
		final boolean specification = isUsingSpecification();
		if ( specification && !isReactive() ) {
			declaration
					.append("_spec.createQuery(")
					.append(localSessionName())
					.append(")\n");
		}
		else {
			declaration
					.append(localSessionName())
					.append(".")
					.append(createQueryMethod())
					.append('(');
			if ( specification ) {
				declaration
						.append("_spec.buildCriteria(_builder)");
			}
			else {
				declaration.append("_query");
			}
			declaration.append(")\n");
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
				.append("\tvar _builder = ")
				.append(localSessionName());
		if ( isReactive() ) {
			declaration.append(".getFactory()");
		}
		declaration
				.append(".getCriteriaBuilder();\n");
	}

	void nullChecks(List<String> paramTypes, StringBuilder declaration) {
		for ( int i = 0; i< paramNames.size(); i++ ) {
			final String paramName = paramNames.get(i);
			final String paramType = paramTypes.get(i);
			if ( !isNullable(i) && !isPrimitive(paramType) ) {
				nullCheck( declaration, paramName );
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
		final String parameterName = parameterName(paramName);
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
						.append("(Object[]) ");

			}
			declaration
					//TODO: only safe if we are binding literals as parameters!!!
					.append(parameterName)
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
