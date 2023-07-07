/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpamodelgen.model.MetaAttribute;
import org.hibernate.jpamodelgen.model.Metamodel;
import org.hibernate.query.Order;
import org.hibernate.query.Page;
import org.hibernate.query.SelectionQuery;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hibernate.internal.util.StringHelper.join;
import static org.hibernate.jpamodelgen.util.StringUtil.getUpperUnderscoreCaseFromLowerCamelCase;

/**
 * @author Gavin King
 */
public class QueryMethod implements MetaAttribute {
	private final Metamodel annotationMetaEntity;
	private final String methodName;
	private final String queryString;
	private final @Nullable String returnTypeName;
	private final @Nullable String containerTypeName;
	private final List<String> paramNames;
	private final List<String> paramTypes;
	private final boolean isNative;
	private final boolean belongsToDao;

	public QueryMethod(
			Metamodel annotationMetaEntity,
			String methodName,
			String queryString,
			@Nullable
			String returnTypeName,
			@Nullable
			String containerTypeName,
			List<String> paramNames,
			List<String> paramTypes,
			boolean isNative,
			boolean belongsToDao) {
		this.annotationMetaEntity = annotationMetaEntity;
		this.methodName = methodName;
		this.queryString = queryString;
		this.returnTypeName = returnTypeName;
		this.containerTypeName = containerTypeName;
		this.paramNames = paramNames;
		this.paramTypes = paramTypes;
		this.isNative = isNative;
		this.belongsToDao = belongsToDao;
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
	public String getAttributeDeclarationString() {
		List<String> paramTypes = this.paramTypes.stream()
				.map(ptype->isOrderParam(ptype) && ptype.endsWith("[]") ? ptype.substring(0, ptype.length()-2) + "..." : ptype)
				.collect(toList());
		StringBuilder declaration = new StringBuilder();
		declaration
				.append("\n/**\n * @see ")
				.append(annotationMetaEntity.getQualifiedName())
				.append("#")
				.append(methodName)
				.append("(")
				.append(join(",", paramTypes.stream().map(this::strip).map(annotationMetaEntity::importType).toArray()))
				.append(")")
				.append("\n **/\n");
		boolean hasVarargs = paramTypes.stream().anyMatch(ptype -> ptype.endsWith("..."));
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
		StringBuilder type = new StringBuilder();
		if (containerTypeName != null) {
			type.append(annotationMetaEntity.importType(containerTypeName));
			if (returnTypeName != null) {
				type.append("<").append(annotationMetaEntity.importType(returnTypeName)).append(">");
			}
		}
		else if (returnTypeName != null)  {
			type.append(annotationMetaEntity.importType(returnTypeName));
		}
		declaration
				.append(type)
				.append(" ")
				.append(methodName)
				.append("(");
		if ( !belongsToDao ) {
			declaration
					.append(annotationMetaEntity.importType("jakarta.persistence.EntityManager"))
					.append(" entityManager");
		}

		for (int i = 0; i<paramNames.size(); i++ ) {
			String ptype = paramTypes.get(i);
			String param = paramNames.get(i);
			String rptype = returnTypeName != null
					? ptype.replace(returnTypeName, annotationMetaEntity.importType(returnTypeName))
					: ptype;

			if ( !belongsToDao || i>0 ) {
				declaration
						.append(", ");
			}

			declaration
					.append(annotationMetaEntity.importType(rptype))
					.append(" ")
					.append(param);
		}
		declaration
				.append(")")
				.append(" {")
				.append("\n\treturn ");
		if ( isNative && returnTypeName != null
				|| containerTypeName != null && containerTypeName.startsWith("org.hibernate") ) {
			declaration.append("(").append(type).append(") ");
		}
		declaration
				.append("entityManager.")
				.append(isNative ? "createNativeQuery" : "createQuery")
				.append("(")
				.append(getConstantName());
		if (returnTypeName != null) {
			declaration
					.append(", ")
					.append(annotationMetaEntity.importType(returnTypeName))
					.append(".class");
		}
		declaration.append(")");
		boolean unwrapped = false;
		for (int i = 1; i <= paramNames.size(); i++) {
			String param = paramNames.get(i-1);
			String ptype = paramTypes.get(i-1);
			if (queryString.contains(":" + param)) {
				declaration
						.append("\n\t\t\t.setParameter(\"")
						.append(param)
						.append("\", ")
						.append(param)
						.append(")");
			}
			else if (queryString.contains("?" + i)) {
				declaration
						.append("\n\t\t\t.setParameter(")
						.append(i)
						.append(", ")
						.append(param)
						.append(")");
			}
			else if (isPageParam(ptype)) {
				unwrap( declaration, unwrapped );
				unwrapped = true;
				declaration
						.append("\n\t\t\t.setPage(")
						.append(param)
						.append(")");
			}
			else if (isOrderParam(ptype)) {
				unwrap( declaration, unwrapped );
				unwrapped = true;
				if (ptype.endsWith("...")) {
					declaration
							.append("\n\t\t\t.setOrder(")
							.append(annotationMetaEntity.importType(List.class.getName()))
							.append(".of(")
							.append(param)
							.append("))");
				}
				else {
					declaration
							.append("\n\t\t\t.setOrder(")
							.append(param)
							.append(")");
				}
			}
		}
		if ( containerTypeName == null) {
			declaration.append("\n\t\t\t.getSingleResult()");
		}
		else if ( containerTypeName.equals("java.util.List") ) {
			declaration.append("\n\t\t\t.getResultList()");
		}
		declaration.append(";\n}");
		return declaration.toString();
	}

	private String strip(String type) {
		int index = type.indexOf("<");
		String stripped = index > 0 ? type.substring(0, index) : type;
		return type.endsWith("...") ? stripped + "..." : stripped;
	}

	static boolean isPageParam(String ptype) {
		return Page.class.getName().equals(ptype);
	}

	static boolean isOrderParam(String ptype) {
		return ptype.startsWith(Order.class.getName())
			|| ptype.startsWith(List.class.getName() + "<" + Order.class.getName());
	}

	private void unwrap(StringBuilder declaration, boolean unwrapped) {
		if ( !unwrapped ) {
			declaration
					.append("\n\t\t\t.unwrap(")
					.append(annotationMetaEntity.importType(SelectionQuery.class.getName()))
					.append(".class)");
		}
	}

	@Override
	public String getAttributeNameDeclarationString() {
		return new StringBuilder()
				.append("public static final String ")
				.append(getConstantName())
				.append(" = \"")
				.append(queryString)
				.append("\";")
				.toString();
	}

	private String getConstantName() {
		String stem = getUpperUnderscoreCaseFromLowerCamelCase(methodName);
		if ( paramTypes.isEmpty() ) {
			return stem;
		}
		else {
			return stem + "_" + StringHelper.join("_",
					paramTypes.stream()
							.filter(name -> !isPageParam(name) && !isOrderParam(name))
							.map(StringHelper::unqualify).collect(toList()));
		}
	}

	@Override
	public String getMetaType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getPropertyName() {
		return methodName;
	}

	@Override
	public String getTypeDeclaration() {
		return "jakarta.persistence.Query";
	}

	@Override
	public Metamodel getHostingEntity() {
		return annotationMetaEntity;
	}
}
