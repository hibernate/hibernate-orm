/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.AssertionFailure;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static org.hibernate.processor.util.Constants.BOXED_VOID;
import static org.hibernate.processor.util.Constants.COLLECTORS;
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
import static org.hibernate.processor.util.Constants.JD_LIMIT;
import static org.hibernate.processor.util.Constants.JD_ORDER;
import static org.hibernate.processor.util.Constants.JD_PAGE;
import static org.hibernate.processor.util.Constants.JD_PAGE_REQUEST;
import static org.hibernate.processor.util.Constants.JD_SORT;
import static org.hibernate.processor.util.Constants.LIST;
import static org.hibernate.processor.util.Constants.NONNULL;
import static org.hibernate.processor.util.Constants.OPTIONAL;
import static org.hibernate.processor.util.Constants.QUERY;
import static org.hibernate.processor.util.Constants.SESSION_TYPES;
import static org.hibernate.processor.util.Constants.STREAM;
import static org.hibernate.processor.util.Constants.TYPED_QUERY;
import static org.hibernate.processor.util.TypeUtils.getGeneratedClassFullyQualifiedName;
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

	boolean initiallyUnwrapped() {
		return !isUsingEntityManager() // a TypedQuery from EntityManager is not a SelectionQuery
			|| isUsingSpecification() && !isReactive(); // SelectionSpecification.createQuery() returns SelectionQuery
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

	String strip(final String fullType) {
		String type = fullType;
		// strip off type annotations
		while ( type.charAt(0) == '@' ) {
			int startIndex = type.lastIndexOf( ' ' );
			if ( startIndex > 0 ) {
				type = type.substring(startIndex+1);
			}
		}
		// strip off type arguments
		int endIndex = type.indexOf("<");
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
		final String paramType = paramTypes.get(i);
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
		final boolean jakartaLimit = JD_LIMIT.equals(paramType);
		final boolean jakartaPageRequest = JD_PAGE_REQUEST.equals(paramType);
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

	void handlePageParameters(
			StringBuilder declaration, List<String> paramTypes,
			@Nullable String containerType) {
		if ( !isJakartaCursoredPage(containerType) ) {
			for ( int i = 0; i < paramNames.size(); i ++ ) {
				final String paramName = paramNames.get(i);
				final String paramType = paramTypes.get(i);
				if ( isPageParam(paramType) ) {
					setPage( declaration, paramName, paramType );
				}
			}
		}
	}

	void handleRestrictionParameters(
			StringBuilder declaration, List<String> paramTypes) {
		for ( int i = 0; i < paramNames.size(); i ++ ) {
			final String paramName = paramNames.get(i);
			final String paramType = paramTypes.get(i);
			if ( isRestrictionParam(paramType) ) {
				if ( paramType.startsWith(LIST) || paramType.endsWith("[]") ) {
					declaration
							.append( "\t_spec.restrict(" )
							.append( annotationMetaEntity.importType(HIB_RESTRICTION) )
							.append( ".all(" )
							.append( paramName )
							.append( "));\n" );

				}
				else {
					declaration
							.append( "\t_spec.restrict(" )
							.append( paramName )
							.append( ");\n" );
				}
			}
			else if ( isRangeParam(paramType) && returnTypeName!= null ) {
				final TypeElement entityElement =
						annotationMetaEntity.getContext().getElementUtils()
								.getTypeElement( returnTypeName );
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
		if ( !unwrapped && isUsingEntityManager() ) {
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
			|| parameterType.startsWith(LIST + "<" + HIB_RESTRICTION);
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
			= "\tvar _unkeyedPage =\n" +
			"\t\t\tpage(pageRequest.size(), (int) pageRequest.page()-1)\n" +
			"\t\t\t\t\t.keyedBy(_orders);\n" +
			"\tvar _keyedPage =\n" +
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

	void createQuery(StringBuilder declaration) {}

	void createSpecification(StringBuilder declaration) {}

	void setParameters(StringBuilder declaration, List<String> paramTypes, String indent) {}

	void tryReturn(StringBuilder declaration, List<String> paramTypes, @Nullable String containerType) {
		if ( isJakartaCursoredPage(containerType) ) {
			makeKeyedPage( declaration, paramTypes );
		}
		if ( dataRepository && !isReactive() ) {
			declaration
					.append("\ttry {\n");
		}
		if ( JD_CURSORED_PAGE.equals(containerType)
				|| JD_PAGE.equals(containerType) ) {
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
			if ( !"void".equals(returnTypeName) || isReactiveSessionAccess() ) {
				declaration
						.append("return ");
			}
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
			createQuery( declaration );
			setParameters( declaration, paramTypes, "\t\t\t\t\t");
			if ( isUsingEntityManager() ) {
				declaration
						.append("\t\t\t\t\t");
			}
			unwrapQuery( declaration, !isUsingEntityManager() );
			declaration
					.append("\t\t\t\t\t\t\t\t.getResultCount()\n\t\t\t\t\t\t: -1;\n");
		}
	}

	void collectOrdering(StringBuilder declaration, List<String> paramTypes, @Nullable String containerType) {
		if ( hasOrdering(paramTypes) ) {
			if ( returnTypeName != null ) {
				final boolean cursoredPage = isJakartaCursoredPage( containerType );
				final String add;
				if ( cursoredPage ) {
					// we need to collect them together in a List
					declaration
							.append("\tvar _orders = new ")
							.append(annotationMetaEntity.importType("java.util.ArrayList"))
							.append("<")
							.append(annotationMetaEntity.importType(HIB_ORDER))
							.append("<? super ")
							.append(annotationMetaEntity.importType(returnTypeName))
							.append(">>();\n");
					add = "_orders.add";
				}
				else {
					add = "_spec.sort";
				}

				// static orders declared using @OrderBy must come first
				for ( OrderBy orderBy : orderBys ) {
					annotationMetaEntity.staticImport(HIB_SORT_DIRECTION, "*");
					declaration
							.append("\t")
							.append(add)
							.append('(')
							.append(annotationMetaEntity.staticImport(HIB_ORDER, orderBy.descending  ? "desc" : "asc"))
							.append('(')
							.append(annotationMetaEntity.importType(returnTypeName))
							.append(".class, \"")
							.append(orderBy.fieldName)
							.append("\")");
					if ( orderBy.ignoreCase ) {
						declaration
								.append("\n\t.ignoringCase()");
					}
					declaration
							.append(");\n");

				}
				for (int i = 0; i < paramTypes.size(); i++) {
					final String type = paramTypes.get(i);
					final String name = paramNames.get(i);
					if ( type.startsWith(HIB_ORDER) && type.endsWith("...") ) {
						declaration
								.append("\tfor (var _sort : ")
								.append(name)
								.append(") {\n")
								.append("\t\t")
								.append(add)
								.append("(_sort);\n")
								.append("\t}\n");
					}
					else if ( type.startsWith(HIB_ORDER) ) {
						declaration
								.append("\t")
								.append(add)
								.append('(')
								.append(name)
								.append(");\n");
					}
					else if ( type.startsWith(LIST + "<" + HIB_ORDER) ) {
						if ( cursoredPage ) {
							declaration
									.append("\t_orders.addAll(")
									.append(name)
									.append(");\n");
						}
						else {
							declaration
									.append("\tfor (var _sort : ")
									.append(name)
									.append(") {\n")
									.append("\t\t")
									.append(add)
									.append("(_sort);\n")
									.append("\t}\n");
						}
					}
					else if ( type.startsWith(JD_ORDER) ) {
						annotationMetaEntity.staticImport(HIB_SORT_DIRECTION, "*");
						declaration
								.append("\tfor (var _sort : ")
								.append(name)
								.append(".sorts()) {\n")
								.append("\t\t")
								.append(add)
								.append('(')
								.append(annotationMetaEntity.staticImport(HIB_ORDER, "asc"))
								.append('(')
								.append(annotationMetaEntity.importType(returnTypeName))
								.append(".class, _sort.property())")
								.append("\n\t\t\t\t\t")
								.append(".reversedIf(_sort.isDescending())")
								.append("\n\t\t\t\t\t")
								.append(".ignoringCaseIf(_sort.ignoreCase()));\n")
								.append("\t}\n");
					}
					else if ( type.startsWith(JD_SORT) && type.endsWith("...") ) {
						// almost identical
						annotationMetaEntity.staticImport(HIB_SORT_DIRECTION, "*");
						declaration
								.append("\tfor (var _sort : ")
								.append(name)
								.append(") {\n")
								.append("\t\t")
								.append(add)
								.append('(')
								.append(annotationMetaEntity.staticImport(HIB_ORDER, "asc"))
								.append('(')
								.append(annotationMetaEntity.importType(returnTypeName))
								.append(".class, _sort.property())")
								.append("\n\t\t\t\t\t")
								.append(".reversedIf(_sort.isDescending())")
								.append("\n\t\t\t\t\t")
								.append(".ignoringCaseIf(_sort.ignoreCase()));\n")
								.append("\t}\n");
					}
					else if ( type.startsWith(JD_SORT) ) {
						annotationMetaEntity.staticImport(HIB_SORT_DIRECTION, "*");
						declaration
								.append("\t")
								.append(add)
								.append('(')
								.append(annotationMetaEntity.staticImport(HIB_ORDER, "asc"))
								.append('(')
								.append(annotationMetaEntity.importType(returnTypeName))
								.append(".class, ")
								.append(name)
								.append(".property())")
								.append("\n\t\t\t\t\t")
								.append(".reversedIf(")
								.append(name)
								.append(".isDescending())")
								.append("\n\t\t\t\t\t")
								.append(".ignoringCaseIf(")
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
				unwrapQuery(declaration, unwrapped);
				declaration
						.append("\t\t\t.getSingleResultOrNull()");
			}
			else {
				declaration
						.append("\t\t\t.getSingleResult()");
			}
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
					break;
				case OPTIONAL:
					unwrapQuery(declaration, unwrapped);
					declaration
							.append("\t\t\t.uniqueResultOptional()");
					break;
				case STREAM:
					declaration
							.append("\t\t\t.getResultStream()");
					break;
				case LIST:
					declaration
							.append("\t\t\t.getResultList()");
					break;
				case HIB_KEYED_RESULT_LIST:
					unwrapQuery(declaration, unwrapped);
					declaration
							.append("\t\t\t.getKeyedResultList(")
							.append(parameterName(HIB_KEYED_PAGE, paramTypes, paramNames))
							.append(")");
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
								.append("\t\treturn ");
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
						String fragment = MAKE_KEYED_SLICE
								.replace("pageRequest",
										parameterName(JD_PAGE_REQUEST, paramTypes, paramNames))
								.replace("Entity",
										annotationMetaEntity.importType(returnTypeName));
						declaration
								.append(fragment);
					}
					break;
				default:
					if ( isUsingEntityManager() && !unwrapped && mustUnwrap ) {
						declaration
								.append("\t\t\t.unwrap(")
								.append(annotationMetaEntity.importType(containerType))
								.append(".class)");

					}
					else {
						final int lastIndex = declaration.length() - 1;
						if ( declaration.charAt(lastIndex) == '\n' )  {
							declaration.setLength(lastIndex);
						}
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
