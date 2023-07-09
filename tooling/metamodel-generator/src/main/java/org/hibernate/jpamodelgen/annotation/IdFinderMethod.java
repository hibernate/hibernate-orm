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
public class IdFinderMethod implements MetaAttribute {
	private final Metamodel annotationMetaEntity;
	private final String methodName;
	private final String entity;
	private final String paramName;
	private final String paramType;
	private final boolean belongsToDao;
	private final String sessionType;
	private final List<String> fetchProfiles;

	public IdFinderMethod(
			Metamodel annotationMetaEntity,
			String methodName, String entity,
			String paramName, String paramType,
			boolean belongsToDao,
			String sessionType,
			List<String> fetchProfiles) {
		this.annotationMetaEntity = annotationMetaEntity;
		this.methodName = methodName;
		this.entity = entity;
		this.paramName = paramName;
		this.paramType = paramType;
		this.belongsToDao = belongsToDao;
		this.sessionType = sessionType;
		this.fetchProfiles = fetchProfiles;
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
		final boolean usingEntityManager = Constants.ENTITY_MANAGER.equals(sessionType);

		StringBuilder declaration = new StringBuilder();
		declaration
				.append("\n/**\n * @see ")
				.append(annotationMetaEntity.getQualifiedName())
				.append("#")
				.append(methodName)
				.append("(")
				.append(annotationMetaEntity.importType(paramType))
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
					.append(" entityManager, ");
		}
		declaration
				.append(annotationMetaEntity.importType(paramType))
				.append(" ")
				.append(paramName)
				.append(") {")
				.append("\n\treturn entityManager");
		if ( fetchProfiles.isEmpty() ) {
			declaration
					.append(Constants.HIB_STATELESS_SESSION.equals(sessionType) ? ".get(" : ".find(")
					.append(annotationMetaEntity.importType(entity))
					.append(".class, ")
					.append(paramName)
					.append(");")
					.append("\n}");
		}
		else {
			if ( usingEntityManager ) {
				declaration
						.append(".unwrap(")
						.append(annotationMetaEntity.importType(Constants.HIB_SESSION))
						.append(".class)\n\t\t\t");
			}
			declaration
					.append(".byId(")
					.append(annotationMetaEntity.importType(entity))
					.append(".class)");
			for ( String profile : fetchProfiles ) {
				declaration
						.append("\n\t\t\t.enableFetchProfile(")
						.append(profile)
						.append(")");
			}
			declaration
					.append("\n\t\t\t.load(")
					.append(paramName)
					.append(");\n}");

		}
		return declaration.toString();
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
