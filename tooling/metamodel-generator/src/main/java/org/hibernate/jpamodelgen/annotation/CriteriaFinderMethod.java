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

import java.util.List;

import static org.hibernate.internal.util.StringHelper.join;

/**
 * @author Gavin King
 */
public class CriteriaFinderMethod implements MetaAttribute {
	private final Metamodel annotationMetaEntity;
	private final String methodName;
	private final String entity;
	private final @Nullable String containerType;
	private final List<String> paramNames;
	private final List<String> paramTypes;
	private final boolean belongsToDao;

	public CriteriaFinderMethod(
			Metamodel annotationMetaEntity,
			String methodName, String entity,
			@Nullable String containerType,
			List<String> paramNames, List<String> paramTypes,
			boolean belongsToDao) {
		this.annotationMetaEntity = annotationMetaEntity;
		this.methodName = methodName;
		this.entity = entity;
		this.containerType = containerType;
		this.paramNames = paramNames;
		this.paramTypes = paramTypes;
		this.belongsToDao = belongsToDao;
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
				.append(join(",", paramTypes.stream().map(this::strip).map(annotationMetaEntity::importType).toArray()))
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
		StringBuilder type = new StringBuilder();
		if ( containerType != null ) {
			type.append(annotationMetaEntity.importType(containerType)).append('<');
		}
		type.append(annotationMetaEntity.importType(entity));
		if ( containerType != null ) {
			type.append('>');
		}
		declaration
				.append(type)
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
				.append("\n\tvar builder = entityManager.getEntityManagerFactory().getCriteriaBuilder();")
				.append("\n\tvar query = builder.createQuery(")
				.append(annotationMetaEntity.importType(entity))
				.append(".class);")
				.append("\n\tvar entity = query.from(")
				.append(annotationMetaEntity.importType(entity))
				.append(".class);")
				.append("\n\tquery.where(");
		for ( int i = 0; i < paramNames.size(); i ++ ) {
			if ( i>0 ) {
				declaration
						.append(", ");
			}
			declaration
					.append("\n\t\t\tbuilder.equal(entity.get(")
					.append(annotationMetaEntity.importType(entity+'_'))
					.append('.')
					.append(paramNames.get(i))
					.append("), ")
					//TODO: only safe if we are binding literals as parameters!!!
					.append(paramNames.get(i))
					.append(")");
		}

		declaration
				.append("\n\t);")
				.append("\n\treturn ");
		if ( containerType != null && containerType.startsWith("org.hibernate") ) {
			declaration
					.append("(")
					.append(type)
					.append(") ");
		}
		declaration
				.append("entityManager.createQuery(query)");
		if ( containerType == null) {
			declaration
					.append("\n\t\t\t.getSingleResult()");
		}
		else if ( containerType.equals(Constants.LIST) ) {
			declaration
					.append("\n\t\t\t.getResultList()");
		}
		declaration
				.append(";\n}");
		return declaration.toString();
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
