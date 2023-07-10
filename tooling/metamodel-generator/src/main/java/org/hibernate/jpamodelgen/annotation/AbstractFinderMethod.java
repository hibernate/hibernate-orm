/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.annotation;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpamodelgen.model.MetaAttribute;
import org.hibernate.jpamodelgen.model.Metamodel;
import org.hibernate.jpamodelgen.util.Constants;

import java.util.List;
import java.util.Locale;

import static org.hibernate.jpamodelgen.util.StringUtil.getUpperUnderscoreCaseFromLowerCamelCase;

/**
 * @author Gavin King
 */
public abstract class AbstractFinderMethod implements MetaAttribute {
	final Metamodel annotationMetaEntity;
	final String methodName;
	final String entity;
	final boolean belongsToDao;
	final String sessionType;
	final boolean usingEntityManager;
	private final boolean addNonnullAnnotation;
	final List<String> fetchProfiles;

	final List<String> paramNames;
	final List<String> paramTypes;

	public AbstractFinderMethod(
			Metamodel annotationMetaEntity,
			String methodName,
			String entity,
			boolean belongsToDao,
			String sessionType,
			List<String> fetchProfiles,
			List<String> paramNames,
			List<String> paramTypes,
			boolean addNonnullAnnotation) {
		this.annotationMetaEntity = annotationMetaEntity;
		this.methodName = methodName;
		this.entity = entity;
		this.belongsToDao = belongsToDao;
		this.sessionType = sessionType;
		this.fetchProfiles = fetchProfiles;
		this.paramNames = paramNames;
		this.paramTypes = paramTypes;
		this.usingEntityManager = Constants.ENTITY_MANAGER.equals(sessionType);
		this.addNonnullAnnotation = addNonnullAnnotation;
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

	abstract boolean isId();

	@Override
	public String getAttributeNameDeclarationString() {
		return new StringBuilder()
				.append("public static final String ")
				.append(constantName())
				.append(" = \"!")
				.append(annotationMetaEntity.getQualifiedName())
				.append('.')
				.append(methodName)
				.append("(")
				.append(parameterList())
				.append(")")
				.append("\";")
				.toString();
	}

	private String parameterList() {
		return paramTypes.stream()
				.map(this::strip)
				.map(annotationMetaEntity::importType)
				.reduce((x, y) -> x + ',' + y)
				.orElse("");
	}

	private String strip(String type) {
		int index = type.indexOf("<");
		String stripped = index > 0 ? type.substring(0, index) : type;
		return type.endsWith("...") ? stripped + "..." : stripped;
	}

	String constantName() {
		return getUpperUnderscoreCaseFromLowerCamelCase(methodName) + "_BY_"
				+ paramNames.stream()
				.map(StringHelper::unqualify)
				.map(name -> name.toUpperCase(Locale.ROOT))
				.reduce((x,y) -> x + "_AND_" + y)
				.orElse("");
	}

	void comment(StringBuilder declaration) {
		declaration
				.append("\n/**")
				.append("\n * Find ")
				.append("{@link ")
				.append(annotationMetaEntity.importType(entity))
				.append("} by ");
		int paramCount = paramNames.size();
		for (int i = 0; i < paramCount; i++) {
			String param = paramNames.get(i);
			if ( i>0 ) {
				if ( i + 1 == paramCount) {
					declaration
							.append(paramCount>2 ? ", and " : " and "); //Oxford comma
				}
				else {
					declaration
							.append(", ");
				}
			}
			declaration
					.append("{@link ")
					.append(annotationMetaEntity.importType(entity))
					.append('#')
					.append(param)
					.append(' ')
					.append(param)
					.append("}");
		}
		declaration
				.append('.')
				.append("\n *")
				.append("\n * @see ")
				.append(annotationMetaEntity.getQualifiedName())
				.append("#")
				.append(methodName)
				.append("(")
				.append(parameterList())
				.append(")");
//		declaration
//				.append("\n *");
//		for (String param : paramNames) {
//			declaration
//					.append("\n * @see ")
//					.append(annotationMetaEntity.importType(entity))
//					.append('#')
//					.append(param);
//		}
		declaration
				.append("\n **/\n");
	}

	void unwrapSession(StringBuilder declaration) {
		if ( usingEntityManager ) {
			declaration
					.append(".unwrap(")
					.append(annotationMetaEntity.importType(Constants.HIB_SESSION))
					.append(".class)\n\t\t\t");
		}
	}

	void enableFetchProfile(StringBuilder declaration) {
//		if ( !usingEntityManager ) {
//			declaration
//					.append("\n\t\t\t.enableFetchProfile(")
//					.append(constantName())
//					.append(")");
//		}
		// currently unused: no way to specify explicit fetch profiles
		for ( String profile : fetchProfiles ) {
			declaration
					.append("\n\t\t\t.enableFetchProfile(")
					.append(profile)
					.append(")");
		}
	}

	void preamble(StringBuilder declaration) {
		modifiers( declaration );
		declaration
				.append(annotationMetaEntity.importType(entity));
		declaration
				.append(" ")
				.append(methodName);
		parameters( declaration) ;
		declaration
				.append(" {")
				.append("\n\treturn entityManager");
	}

	void modifiers(StringBuilder declaration) {
		declaration
				.append(belongsToDao ? "@Override\npublic " : "public static ");
	}

	void parameters(StringBuilder declaration) {
		declaration
				.append("(");
		if ( !belongsToDao ) {
			notNull( declaration );
			declaration
					.append(annotationMetaEntity.importType(Constants.ENTITY_MANAGER))
					.append(" entityManager");
		}
		for ( int i = 0; i < paramNames.size(); i ++ ) {
			if ( !belongsToDao || i > 0 ) {
				declaration
						.append(", ");
			}
			if ( isId() ) {
				notNull( declaration );
			}
			declaration
					.append(annotationMetaEntity.importType(paramTypes.get(i)))
					.append(" ")
					.append(paramNames.get(i));
		}
		declaration
				.append(')');
	}

	private void notNull(StringBuilder declaration) {
		if ( addNonnullAnnotation ) {
			declaration
					.append('@')
					.append(annotationMetaEntity.importType("jakarta.annotation.Nonnull"))
					.append(' ');
		}
	}
}
