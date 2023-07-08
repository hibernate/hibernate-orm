/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.annotation;

import org.hibernate.jpamodelgen.model.MetaAttribute;
import org.hibernate.jpamodelgen.model.Metamodel;
import org.hibernate.jpamodelgen.util.Constants;

import java.util.List;

/**
 * @author Gavin King
 */
public class NaturalIdFinderMethod implements MetaAttribute {
	private final Metamodel annotationMetaEntity;
	private final String methodName;
	private final String entity;
	private final List<String> paramNames;
	private final List<String> paramTypes;
	private final boolean belongsToDao;
	private final String sessionType;

	public NaturalIdFinderMethod(
			Metamodel annotationMetaEntity,
			String methodName, String entity,
			List<String> paramNames, List<String> paramTypes,
			boolean belongsToDao,
			String sessionType) {
		this.annotationMetaEntity = annotationMetaEntity;
		this.methodName = methodName;
		this.entity = entity;
		this.paramNames = paramNames;
		this.paramTypes = paramTypes;
		this.belongsToDao = belongsToDao;
		this.sessionType = sessionType;
	}

	@Override
	public boolean hasTypedAttribute() {
		return true;
	}

	@Override
	public boolean hasStringAttribute() {
		return false;
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
				.append(parameterList())
				.append(")")
				.append("\n **/\n");
		if ( belongsToDao ) {
			declaration
					.append("@Override\npublic ");
		}
		else {
			declaration
					.append("public static ");
		}
		declaration
				.append(annotationMetaEntity.importType(entity));
		declaration
				.append(" ")
				.append(methodName)
				.append("(");
		if ( !belongsToDao ) {
			declaration
					.append(annotationMetaEntity.importType(Constants.ENTITY_MANAGER))
					.append(" entityManager");
		}
		for ( int i = 0; i < paramNames.size(); i ++ ) {
			if ( !belongsToDao || i > 0 ) {
				declaration
						.append(", ");
			}
			declaration
					.append(annotationMetaEntity.importType(paramTypes.get(i)))
					.append(" ")
					.append(paramNames.get(i));
		}
		declaration
				.append(") {")
				.append("\n\treturn entityManager");
		if ( Constants.ENTITY_MANAGER.equals(sessionType) ) {
			declaration
					.append(".unwrap(")
					.append(annotationMetaEntity.importType(Constants.HIB_SESSION))
					.append(".class)\n\t\t\t");
		}
		declaration
				.append(".byNaturalId(")
				.append(annotationMetaEntity.importType(entity))
				.append(".class)");
		for ( int i = 0; i < paramNames.size(); i ++ ) {
			declaration
					.append("\n\t\t\t.using(")
					.append(annotationMetaEntity.importType(entity+'_'))
					.append('.')
					.append(paramNames.get(i))
					.append(", ")
					.append(paramNames.get(i))
					.append(")");
		}
		declaration
				.append("\n\t\t\t.load();\n}");
		return declaration.toString();
	}

	private String parameterList() {
		return paramTypes.stream()
				.map(this::strip)
				.map(annotationMetaEntity::importType)
				.reduce((x, y) -> x + y)
				.orElse("");
	}

	private String strip(String type) {
		int index = type.indexOf("<");
		String stripped = index > 0 ? type.substring(0, index) : type;
		return type.endsWith("...") ? stripped + "..." : stripped;
	}

	@Override
	public String getAttributeNameDeclarationString() {
		throw new UnsupportedOperationException();
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
		return entity;
	}

	@Override
	public Metamodel getHostingEntity() {
		return annotationMetaEntity;
	}
}
