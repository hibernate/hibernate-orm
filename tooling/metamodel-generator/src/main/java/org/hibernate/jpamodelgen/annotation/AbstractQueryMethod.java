/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.jpamodelgen.model.MetaAttribute;
import org.hibernate.jpamodelgen.model.Metamodel;
import org.hibernate.jpamodelgen.util.Constants;
import org.hibernate.query.Order;
import org.hibernate.query.Page;
import org.hibernate.query.SortDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.hibernate.jpamodelgen.util.Constants.JD_LIMIT;
import static org.hibernate.jpamodelgen.util.Constants.JD_ORDER;
import static org.hibernate.jpamodelgen.util.Constants.JD_SORT;
import static org.hibernate.jpamodelgen.util.Constants.SESSION_TYPES;
import static org.hibernate.jpamodelgen.util.TypeUtils.isPrimitive;

/**
 * @author Gavin King
 */
public abstract class AbstractQueryMethod implements MetaAttribute {
	final AnnotationMetaEntity annotationMetaEntity;
	final String methodName;
	final List<String> paramNames;
	final List<String> paramTypes;
	final @Nullable String returnTypeName;
	final String sessionType;
	final String sessionName;
	final boolean belongsToDao;
	final boolean addNonnullAnnotation;

	public AbstractQueryMethod(
			AnnotationMetaEntity annotationMetaEntity,
			String methodName,
			List<String> paramNames, List<String> paramTypes,
			@Nullable String returnTypeName,
			String sessionType,
			String sessionName,
			boolean belongsToDao,
			boolean addNonnullAnnotation) {
		this.annotationMetaEntity = annotationMetaEntity;
		this.methodName = methodName;
		this.paramNames = paramNames;
		this.paramTypes = paramTypes;
		this.returnTypeName = returnTypeName;
		this.sessionType = sessionType;
		this.sessionName = sessionName;
		this.belongsToDao = belongsToDao;
		this.addNonnullAnnotation = addNonnullAnnotation;
	}

	@Override
	public Metamodel getHostingEntity() {
		return annotationMetaEntity;
	}

	@Override
	public String getMetaType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getPropertyName() {
		return methodName;
	}

	abstract boolean isNullable(int index);

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

	String strip(String type) {
		int index = type.indexOf("<");
		String stripped = index > 0 ? type.substring(0, index) : type;
		return type.endsWith("...") ? stripped + "..." : stripped;
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
			final String paramType = paramTypes.get(i);
			if ( !isNullable(i) && !isPrimitive(paramType)
					|| isSessionParameter(paramType) ) {
				notNull( declaration );
			}
			declaration
					.append(annotationMetaEntity.importType(importReturnTypeArgument(paramType)))
					.append(" ")
					.append(paramNames.get(i));
		}
		declaration
				.append(")");
	}

	static boolean isSessionParameter(String paramType) {
		return SESSION_TYPES.contains(paramType);
	}

	private String importReturnTypeArgument(String type) {
		return returnTypeName != null
				? type.replace(returnTypeName, annotationMetaEntity.importType(returnTypeName))
				: type;
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
					.append(annotationMetaEntity.importType("jakarta.annotation.Nonnull"))
					.append(' ');
		}
	}

	void see(StringBuilder declaration) {
		declaration
				.append("\n * @see ")
				.append(annotationMetaEntity.getQualifiedName())
				.append("#")
				.append(methodName)
				.append("(")
				.append(parameterList())
				.append(")");
	}

	boolean isUsingEntityManager() {
		return Constants.ENTITY_MANAGER.equals(sessionType);
	}

	boolean isUsingStatelessSession() {
		return Constants.HIB_STATELESS_SESSION.equals(sessionType);
	}

	boolean isReactive() {
		return Constants.MUTINY_SESSION.equals(sessionType);
	}

	void setPage(StringBuilder declaration, String paramName, String paramType) {
		boolean jakartaLimit = JD_LIMIT.equals(paramType);
		if ( jakartaLimit || isUsingEntityManager() ) {
			declaration
					.append("\n\t\t\t.setFirstResult(");
			if (jakartaLimit) {
				declaration.append("(int) ");
			}
			declaration
					.append(paramName)
					.append('.')
					.append(jakartaLimit ? "startAt" : "getFirstResult")
					.append("())")
					.append("\n\t\t\t.setMaxResults(")
					.append(paramName)
					.append('.')
					.append(jakartaLimit ? "maxResults" : "getMaxResults")
					.append("())");
		}
		else {
			declaration
					.append("\n\t\t\t.setPage(")
					.append(paramName)
					.append(")");
		}
	}

	boolean setOrder(StringBuilder declaration, boolean unwrapped, String paramName, String paramType) {
		unwrapQuery( declaration, unwrapped );
		if ( paramType.startsWith(JD_SORT) ) {
			final String sortableEntityClass = getSortableEntityClass();
			if ( sortableEntityClass != null ) {
				annotationMetaEntity.staticImport(SortDirection.class.getName(), "ASCENDING");
				annotationMetaEntity.staticImport(SortDirection.class.getName(), "DESCENDING");
				declaration
						.append("\n\t\t\t.setOrder(")
						.append(annotationMetaEntity.importType(Order.class.getName()))
						.append(".by(")
						.append(annotationMetaEntity.importType(sortableEntityClass))
						.append(".class, ")
						.append(paramName)
						.append(".property()")
						.append(",\n\t\t\t\t\t")
						.append(paramName)
						.append(".isAscending() ? ASCENDING : DESCENDING")
						.append("))");
				return true;
			}
			else {
				return false;
			}
		}
		if ( paramType.startsWith(JD_ORDER) ) {
			final String sortableEntityClass = getSortableEntityClass();
			if ( sortableEntityClass != null ) {
				annotationMetaEntity.staticImport(SortDirection.class.getName(), "ASCENDING");
				annotationMetaEntity.staticImport(SortDirection.class.getName(), "DESCENDING");
				annotationMetaEntity.staticImport(Collectors.class.getName(), "toList");
				annotationMetaEntity.staticImport(Order.class.getName(), "by");
				declaration
						.append("\n\t\t\t.setOrder(new ")
						.append(annotationMetaEntity.importType(ArrayList.class.getName()))
						.append("<>() {{\n\t\t\t\t")
						.append(paramName)
						.append(".forEach(sort -> add(")
						.append("by(")
						.append(annotationMetaEntity.importType(sortableEntityClass))
						.append(".class, ")
						.append("sort.property()")
						.append(",\n\t\t\t\t\t\t")
						.append("sort.isAscending() ? ASCENDING : DESCENDING")
						.append(", sort.ignoreCase())));\n\t\t\t}})");
				return true;
			}
			else {
				return false;
			}
		}
		else if ( paramType.endsWith("...") ) {
			declaration
					.append("\n\t\t\t.setOrder(")
					.append(annotationMetaEntity.importType(Constants.LIST))
					.append(".of(")
					.append(paramName)
					.append("))");
		}
		else {
			declaration
					.append("\n\t\t\t.setOrder(")
					.append(paramName)
					.append(")");
		}
		return true;
	}

	@Nullable String getSortableEntityClass() {
		return returnTypeName;
	}

	private void unwrapQuery(StringBuilder declaration, boolean unwrapped) {
		if ( !unwrapped ) {
			declaration
					.append("\n\t\t\t.unwrap(")
					.append(annotationMetaEntity.importType(Constants.HIB_SELECTION_QUERY))
					.append(".class)");
		}
	}

	static boolean isPageParam(String parameterType) {
		return Page.class.getName().equals(parameterType)
			|| JD_LIMIT.equals(parameterType);
	}

	static boolean isOrderParam(String parameterType) {
		return parameterType.startsWith(Order.class.getName())
			|| parameterType.startsWith(List.class.getName() + "<" + Order.class.getName())
			|| parameterType.startsWith(JD_SORT)
			|| parameterType.startsWith(JD_ORDER);
	}
}
