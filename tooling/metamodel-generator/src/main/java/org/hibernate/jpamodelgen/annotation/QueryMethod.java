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
			boolean isNative) {
		this.annotationMetaEntity = annotationMetaEntity;
		this.methodName = methodName;
		this.queryString = queryString;
		this.returnTypeName = returnTypeName;
		this.containerTypeName = containerTypeName;
		this.paramNames = paramNames;
		this.paramTypes = paramTypes;
		this.isNative = isNative;
	}

	@Override
	public boolean hasTypedAttribute() {
		return true;
	}

	@Override
	public String getAttributeDeclarationString() {
		StringBuilder declaration = new StringBuilder();
		declaration
				.append("\n/**\n * @see ")
				.append(annotationMetaEntity.getQualifiedName())
				.append("#")
				.append(methodName)
				.append("(")
				.append(join(",", paramTypes.stream().map(annotationMetaEntity::importType).toArray()))
				.append(")")
				.append("\n **/\n")
				.append("public static ");
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
				.append("(")
				.append(annotationMetaEntity.importType("jakarta.persistence.EntityManager"))
				.append(" entityManager");

		for (int i =0; i<paramNames.size(); i++) {
			declaration
					.append(", ")
					.append(annotationMetaEntity.importType(paramTypes.get(i)))
					.append(" ")
					.append(paramNames.get(i));
		}
		declaration
				.append(")")
				.append(" {")
				.append("\n    return ");
		if ( isNative && returnTypeName != null
				|| containerTypeName != null && containerTypeName.startsWith("org.hibernate") ) {
			declaration.append("(").append(type).append(") ");
		}
		declaration
				.append("entityManager.")
				.append(isNative ? "createNativeQuery" :"createQuery")
				.append("(")
				.append(getConstantName());
		if (returnTypeName != null) {
			declaration
					.append(", ")
					.append(annotationMetaEntity.importType(returnTypeName))
					.append(".class");
		}
		declaration.append(")");
		for (int i = 1; i <= paramNames.size(); i++) {
			String param = paramNames.get(i-1);
			if (queryString.contains(":" + param)) {
				declaration
						.append("\n			.setParameter(\"")
						.append(param)
						.append("\", ")
						.append(param)
						.append(")");
			}
			else if (queryString.contains("?" + i)) {
				declaration
						.append("\n			.setParameter(")
						.append(i)
						.append(", ")
						.append(param)
						.append(")");
			}
		}
		if ( containerTypeName == null) {
			declaration.append("\n			.getSingleResult()");
		}
		else if ( containerTypeName.equals("java.util.List") ) {
			declaration.append("\n			.getResultList()");
		}
		declaration.append(";\n}");
		return declaration.toString();
	}

	@Override
	public String getAttributeNameDeclarationString() {
		return new StringBuilder().append("public static final String ")
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
					paramTypes.stream().map(StringHelper::unqualify).collect(toList()));
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
