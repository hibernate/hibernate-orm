/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.AssertionFailure;
import org.hibernate.jpamodelgen.model.MetaAttribute;
import org.hibernate.jpamodelgen.model.Metamodel;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.hibernate.jpamodelgen.util.Constants.*;
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
	final List<OrderBy> orderBys;
	final boolean addNonnullAnnotation;
	final boolean dataRepository;

	AbstractQueryMethod(
			AnnotationMetaEntity annotationMetaEntity,
			String methodName,
			List<String> paramNames, List<String> paramTypes,
			@Nullable String returnTypeName,
			String sessionType,
			String sessionName,
			boolean belongsToDao,
			List<OrderBy> orderBys,
			boolean addNonnullAnnotation,
			boolean dataRepository) {
		this.annotationMetaEntity = annotationMetaEntity;
		this.methodName = methodName;
		this.paramNames = paramNames;
		this.paramTypes = paramTypes;
		this.returnTypeName = returnTypeName;
		this.sessionType = sessionType;
		this.sessionName = sessionName;
		this.belongsToDao = belongsToDao;
		this.orderBys = orderBys;
		this.addNonnullAnnotation = addNonnullAnnotation;
		this.dataRepository = dataRepository;
	}

	@Override
	public Metamodel getHostingEntity() {
		return annotationMetaEntity;
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

	void preamble(StringBuilder declaration, StringBuilder returnType, List<String> paramTypes) {
		declaration
				.append(returnType)
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
		return ENTITY_MANAGER.equals(sessionType);
	}

	boolean isUsingStatelessSession() {
		return HIB_STATELESS_SESSION.equals(sessionType);
	}

	boolean isReactive() {
		return MUTINY_SESSION.equals(sessionType)
			|| UNI_MUTINY_SESSION.equals(sessionType);
	}

	boolean isReactiveSession() {
		return UNI_MUTINY_SESSION.equals(sessionType);
	}

	String localSessionName() {
		return isReactiveSession() ? "resolvedSession" : sessionName;
	}
	
	void chainSession(StringBuilder declaration) {
		// Reactive calls always have a return type
		if ( isReactiveSession() ) {
			declaration
					.append("\treturn ")
					.append(sessionName)
					.append(".chain(")
					.append(localSessionName())
					.append(" -> {\n\t");
		}
	}

	void chainSessionEnd(boolean isUpdate, StringBuilder declaration) {
		if ( isReactiveSession() ) {
			declaration.append("\n\t})");
			// here we're checking for a boxed void and not Uni<Void> because the returnType has already
			// been checked, and is ununi-ed
			if ( isUpdate && returnTypeName != null && returnTypeName.equals(BOXED_VOID) ) {
				declaration.append(".replaceWithVoid();");
			}
			else {
				declaration.append(";");
			}
		}
	}

	void setPage(StringBuilder declaration, String paramName, String paramType) {
		boolean jakartaLimit = JD_LIMIT.equals(paramType);
		boolean jakartaPageRequest = paramType.startsWith(JD_PAGE_REQUEST);
		if ( jakartaLimit || jakartaPageRequest
				|| isUsingEntityManager() ) {
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

	boolean applyOrder(
			StringBuilder declaration, List<String> paramTypes,
			@Nullable String containerType, boolean unwrapped) {
		if ( !isJakartaKeyedSlice(containerType) && hasOrdering(paramTypes) ) {
			unwrapQuery( declaration, unwrapped );
			declaration
					.append("\t\t\t.setOrder(_orders)\n");
			return true;
		}
		return unwrapped;
	}

	void handlePageParameters(
			StringBuilder declaration, List<String> paramTypes,
			@Nullable String containerType) {
		if ( !isJakartaKeyedSlice(containerType) ) {
			for ( int i = 0; i < paramNames.size(); i ++ ) {
				final String paramName = paramNames.get(i);
				final String paramType = paramTypes.get(i);
				if ( isPageParam(paramType) ) {
					setPage( declaration, paramName, paramType );
				}
			}
		}
	}

	void convertExceptions(StringBuilder declaration) {
		if (dataRepository) {
			declaration
					.append("\t}\n");
			if ( singleResult() ) {
				declaration
						.append("\tcatch (")
						.append(annotationMetaEntity.importType("jakarta.persistence.NoResultException"))
						.append(" exception) {\n")
						.append("\t\tthrow new ")
						.append(annotationMetaEntity.importType("jakarta.data.exceptions.EmptyResultException"))
						.append("(exception);\n")
						.append("\t}\n")
						.append("\tcatch (")
						.append(annotationMetaEntity.importType("jakarta.persistence.NonUniqueResultException"))
						.append(" exception) {\n")
						.append("\t\tthrow new ")
						.append(annotationMetaEntity.importType("jakarta.data.exceptions.NonUniqueResultException"))
						.append("(exception);\n")
						.append("\t}\n");
			}
			declaration
					.append("\tcatch (")
					.append(annotationMetaEntity.importType("jakarta.persistence.PersistenceException"))
					.append(" exception) {\n")
					.append("\t\tthrow new ")
					.append(annotationMetaEntity.importType("jakarta.data.exceptions.DataException"))
					.append("(exception);\n")
					.append("\t}\n");
		}
	}

	abstract boolean singleResult();

	static void closingBrace(StringBuilder declaration) {
		declaration.append("\n}");
	}

	void unwrapQuery(StringBuilder declaration, boolean unwrapped) {
		if ( !unwrapped && isUsingEntityManager() ) {
			declaration
					.append("\t\t\t.unwrap(")
					.append(annotationMetaEntity.importType(HIB_SELECTION_QUERY))
					.append(".class)\n");
		}
	}

	static boolean isSpecialParam(String parameterType) {
		return isPageParam(parameterType)
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
			|| parameterType.startsWith(JD_PAGE_REQUEST);
	}

	static boolean isOrderParam(String parameterType) {
		return parameterType.startsWith(HIB_ORDER)
			|| parameterType.startsWith(LIST + "<" + HIB_ORDER)
			|| parameterType.startsWith(JD_SORT)
			|| parameterType.startsWith(JD_ORDER)
			|| parameterType.startsWith(JD_PAGE_REQUEST);
	}

	static boolean isJakartaKeyedSlice(@Nullable String containerType) {
		return JD_KEYED_SLICE.equals(containerType)
			|| JD_KEYED_PAGE.equals(containerType);
	}

	static boolean isJakartaSlice(@Nullable String containerType) {
		return JD_SLICE.equals(containerType)
			|| JD_PAGE.equals(containerType);
	}

	void makeKeyedPage(StringBuilder declaration, List<String> paramTypes) {
		annotationMetaEntity.staticImport("org.hibernate.query.SortDirection", "*");
		annotationMetaEntity.staticImport("org.hibernate.query.KeyedPage.KeyInterpretation", "*");
		annotationMetaEntity.staticImport("org.hibernate.query.Order", "by");
		annotationMetaEntity.staticImport("org.hibernate.query.Page", "page");
		annotationMetaEntity.staticImport(Collectors.class.getName(), "toList");
		if ( returnTypeName == null ) {
			throw new AssertionFailure("entity class cannot be null");
		}
		else {
			declaration
					.append(MAKE_KEYED_PAGE
							.replace("pageRequest",
									parameterName(JD_PAGE_REQUEST, paramTypes, paramNames))
							.replace("Entity", annotationMetaEntity.importType(returnTypeName)));
		}
	}

	static final String MAKE_KEYED_SLICE
			= "\t\tvar _cursors =\n" +
			"\t\t\t\t_results.getKeyList()\n" +
			"\t\t\t\t\t\t.stream()\n" +
			"\t\t\t\t\t\t.map(_key -> Cursor.forKeyset(_key.toArray()))\n" +
			"\t\t\t\t\t\t.collect(toList());\n" +
			"\t\tvar _page =\n" +
			"\t\t\t\tPageRequest.of(Entity.class)\n" +
			"\t\t\t\t\t\t.sortBy(pageRequest.sorts())\n" +
			"\t\t\t\t\t\t.size(pageRequest.size())\n" +
			"\t\t\t\t\t\t.page(pageRequest.page() + 1);\n" +
			"\t\treturn new KeysetAwareSliceRecord<>( _results.getResultList(), _cursors, _totalResults, pageRequest,\n" +
			"\t\t\t\t_results.isLastPage() ? null : _page.afterKeyset(_results.getNextPage().getKey().toArray()),\n" +
			"\t\t\t\t_results.isFirstPage() ? null : _page.beforeKeyset(_results.getPreviousPage().getKey().toArray()) );\n";

	static final String MAKE_KEYED_PAGE
			= "\tvar _unkeyedPage =\n" +
			"\t\t\tpage(pageRequest.size(), (int) pageRequest.page()-1)\n" +
			"\t\t\t\t\t.keyedBy(_orders);\n" +
			"\tvar _keyedPage =\n" +
			"\t\t\tpageRequest.cursor()\n" +
			"\t\t\t\t\t.map(_cursor -> {\n" +
			"\t\t\t\t\t\t@SuppressWarnings(\"unchecked\")\n" +
			"\t\t\t\t\t\tvar _elements = (List<Comparable<?>>) _cursor.elements();\n" +
			"\t\t\t\t\t\treturn switch ( pageRequest.mode() ) {\n" +
			"\t\t\t\t\t\t\tcase CURSOR_NEXT -> _unkeyedPage.withKey(_elements, KEY_OF_LAST_ON_PREVIOUS_PAGE);\n" +
			"\t\t\t\t\t\t\tcase CURSOR_PREVIOUS -> _unkeyedPage.withKey(_elements, KEY_OF_FIRST_ON_NEXT_PAGE);\n" +
			"\t\t\t\t\t\t\tdefault -> _unkeyedPage;\n" +
			"\t\t\t\t\t\t};\n" +
			"\t\t\t\t\t}).orElse(_unkeyedPage);\n";

	void createQuery(StringBuilder declaration) {}

	void setParameters(StringBuilder declaration, List<String> paramTypes) {}

	void tryReturn(StringBuilder declaration, List<String> paramTypes, @Nullable String containerType) {
		if ( isJakartaKeyedSlice(containerType) ) {
			makeKeyedPage( declaration, paramTypes );
		}
		if ( dataRepository ) {
			declaration
					.append("\ttry {\n");
		}
		if ( JD_KEYED_PAGE.equals(containerType)
				|| JD_PAGE.equals(containerType) ) {
			if ( dataRepository ) {
				declaration
						.append('\t');
			}
			totalResults(declaration, paramTypes);
		}
		if ( dataRepository ) {
			declaration
					.append('\t');
		}
		declaration
				.append('\t');
		if ( isJakartaKeyedSlice(containerType)
				|| isJakartaSlice(containerType) ) {
			if ( returnTypeName != null && isUsingEntityManager() ) {
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
			if ( !"void".equals(returnTypeName) || isReactiveSession() ) {
				declaration
						.append("return ");
			}
		}
	}

	private void totalResults(StringBuilder declaration, List<String> paramTypes) {
		declaration
				.append("\tlong _totalResults = ");
		createQuery( declaration );
		setParameters( declaration, paramTypes );
		unwrapQuery( declaration, !isUsingEntityManager() );
		declaration
				.append("\t\t\t.getResultCount();\n");
	}

	void collectOrdering(StringBuilder declaration, List<String> paramTypes) {
		if ( hasOrdering(paramTypes) ) {
			if ( returnTypeName != null ) {
				declaration
						.append("\tvar _orders = new ")
						.append(annotationMetaEntity.importType("java.util.ArrayList"))
						.append("<")
						.append(annotationMetaEntity.importType(HIB_ORDER))
						.append("<? super ")
						.append(annotationMetaEntity.importType(returnTypeName))
						.append(">>();\n");
				// static orders declared using @OrderBy must come first
				for ( OrderBy orderBy : orderBys ) {
					declaration
							.append("\t_orders.add(")
							.append(annotationMetaEntity.staticImport(HIB_ORDER, "by"))
							.append('(')
							.append(annotationMetaEntity.importType(returnTypeName))
							.append(".class, \"")
							.append(orderBy.fieldName)
							.append("\", ")
							.append(orderBy.descending ? "DESCENDING" : "ASCENDING")
							.append(", ")
							.append(orderBy.ignoreCase)
							.append("));\n");

				}
				for (int i = 0; i < paramTypes.size(); i++) {
					final String type = paramTypes.get(i);
					final String name = paramNames.get(i);
					if ( type.startsWith(HIB_ORDER) && type.endsWith("...") ) {
						declaration
								.append("\tfor (var _sort : ")
								.append(name)
								.append(") {\n")
								.append("\t\t_orders.add(_sort);\n")
								.append("\t}\n");
					}
					else if ( type.startsWith(HIB_ORDER) ) {
						declaration
								.append("\t_orders.add(")
								.append(name)
								.append(");\n");
					}
					else if ( type.startsWith(LIST + "<" + HIB_ORDER) ) {
						declaration
								.append("\t_orders.addAll(")
								.append(name)
								.append(");\n");
					}
					else if ( type.startsWith(JD_ORDER)
							|| type.startsWith(JD_PAGE_REQUEST) ) {
						declaration
								.append("\tfor (var _sort : ")
								.append(name)
								.append(".sorts()) {\n")
								.append("\t\t_orders.add(")
								.append(annotationMetaEntity.staticImport(HIB_ORDER, "by"))
								.append('(')
								.append(annotationMetaEntity.importType(returnTypeName))
								.append(".class, _sort.property(),")
								.append("\n\t\t\t\t\t\t")
								.append("_sort.isAscending() ? ASCENDING : DESCENDING,")
								.append("\n\t\t\t\t\t\t")
								.append("_sort.ignoreCase()));\n")
								.append("\t}\n");
					}
					else if ( type.startsWith(JD_SORT) && type.endsWith("...") ) {
						// almost identical
						declaration
								.append("\tfor (var _sort : ")
								.append(name)
								.append(") {\n")
								.append("\t\t_orders.add(")
								.append(annotationMetaEntity.staticImport(HIB_ORDER, "by"))
								.append('(')
								.append(annotationMetaEntity.importType(returnTypeName))
								.append(".class, _sort.property(),")
								.append("\n\t\t\t\t\t\t")
								.append("_sort.isAscending() ? ASCENDING : DESCENDING,")
								.append("\n\t\t\t\t\t\t")
								.append("_sort.ignoreCase()));\n")
								.append("\t}\n");
					}
					else if ( type.startsWith(JD_SORT) ) {
						declaration
								.append("\t_orders.add(")
								.append(annotationMetaEntity.staticImport(HIB_ORDER, "by"))
								.append('(')
								.append(annotationMetaEntity.importType(returnTypeName))
								.append(".class, ")
								.append(name)
								.append(".property(),")
								.append("\n\t\t\t\t\t\t")
								.append(name)
								.append(".isAscending() ? ASCENDING : DESCENDING,")
								.append("\n\t\t\t\t\t\t")
								.append(name)
								.append(".ignoreCase()));\n");
					}
				}
			}
		}
	}

	private boolean hasOrdering(List<String> paramTypes) {
		return paramTypes.stream().anyMatch(AbstractQueryMethod::isOrderParam)
			|| !orderBys.isEmpty();
	}

	boolean unwrapIfNecessary(StringBuilder declaration, @Nullable String containerType, boolean unwrapped) {
		if ( OPTIONAL.equals(containerType) || isJakartaKeyedSlice(containerType) ) {
			unwrapQuery( declaration, unwrapped );
			unwrapped = true;
		}
		return unwrapped;
	}

	protected void executeSelect(
			StringBuilder declaration,
			List<String> paramTypes,
			@Nullable String containerType,
			boolean unwrapped,
			boolean mustUnwrap) {
		declaration
				.append("\t\t\t");
		if ( containerType == null ) {
			declaration
					.append(".getSingleResult();");
		}
		else {
			switch (containerType) {
				case "[]":
					if ( returnTypeName== null ) {
						throw new AssertionFailure("missing return type");
					}
					else {
						declaration
								.append(".getResultList()\n\t\t\t.toArray(new ")
								.append(annotationMetaEntity.importType(returnTypeName))
								.append("[0]);");
					}
					break;
				case OPTIONAL:
					declaration
							.append(".uniqueResultOptional();");
					break;
				case STREAM:
					declaration
							.append(".getResultStream();");
					break;
				case LIST:
					declaration
							.append(".getResultList();");
					break;
				case HIB_KEYED_RESULT_LIST:
					declaration
							.append(".getKeyedResultList(")
							.append(parameterName(HIB_KEYED_PAGE, paramTypes, paramNames))
							.append(");");
					break;
				case JD_SLICE:
					declaration
							.append(".getResultList();\n")
							.append("\t\treturn new ")
							.append(implType(containerType))
							.append('(')
							.append(parameterName(JD_PAGE_REQUEST, paramTypes, paramNames))
							.append(", _results);\n");
					break;
				case JD_PAGE:
					declaration
							.append(".getResultList();\n")
							.append("\t\treturn new ")
							.append(implType(containerType))
							.append('(')
							.append(parameterName(JD_PAGE_REQUEST, paramTypes, paramNames))
							.append(", _results, _totalResults);\n");
					break;
				case JD_KEYED_SLICE:
				case JD_KEYED_PAGE:
					if ( returnTypeName == null ) {
						throw new AssertionFailure("entity class cannot be null");
					}
					else {
						declaration
								.append(".getKeyedResultList(_keyedPage);\n");
						annotationMetaEntity.importType("jakarta.data.page.PageRequest");
						annotationMetaEntity.importType("jakarta.data.page.PageRequest.Cursor");
						String fragment = MAKE_KEYED_SLICE
								.replace("pageRequest",
										parameterName(JD_PAGE_REQUEST, paramTypes, paramNames))
								.replace("Entity", annotationMetaEntity.importType(returnTypeName))
								.replace("KeysetAwareSliceRecord", implType(containerType));
						if ( JD_KEYED_SLICE.equals(containerType) ) {
							fragment = fragment.replace("_totalResults, ", "");
						}
						declaration
								.append(fragment);
					}
					break;
				default:
					if ( isUsingEntityManager() && !unwrapped && mustUnwrap ) {
						declaration
								.append("\t\t\t")
								.append(".unwrap(")
								.append(annotationMetaEntity.importType(containerType))
								.append(".class);");

					}
					else {
						declaration.append(';');
					}
			}
		}
	}

	private static String parameterName(String paramType, List<String> paramTypes, List<String> paramNames) {
		for (int i = 0; i < paramTypes.size(); i++) {
			if ( paramTypes.get(i).startsWith(paramType) ) {
				return paramNames.get(i);
			}
		}
		throw new AssertionFailure("could not find parameter");
	}

	private String implType(String containerType) {
		switch (containerType) {
			case JD_SLICE:
				return annotationMetaEntity.importType("jakarta.data.page.impl.SliceRecord");
			case JD_PAGE:
				return annotationMetaEntity.importType("jakarta.data.page.impl.PageRecord");
			case JD_KEYED_SLICE:
				return annotationMetaEntity.importType("jakarta.data.page.impl.KeysetAwareSliceRecord");
			case JD_KEYED_PAGE:
				return annotationMetaEntity.importType("jakarta.data.page.impl.KeysetAwarePageRecord");
			default:
				throw new AssertionFailure("unrecognized slice type");
		}
	}
}
