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
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.type.descriptor.java.JavaType;

import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.TreeSet;

import static org.hibernate.jpamodelgen.util.StringUtil.nameToFieldName;
import static org.hibernate.jpamodelgen.validation.ProcessorSessionFactory.findEntityByUnqualifiedName;

/**
 * @author Gavin King
 */
class NamedQueryMethod implements MetaAttribute {
	private final AnnotationMeta annotationMeta;
	private final SqmSelectStatement<?> select;
	private final String name;
	private final boolean belongsToDao;
	private final boolean reactive;
	private final boolean addNonnullAnnotation;

	public NamedQueryMethod(
			AnnotationMeta annotationMeta,
			SqmSelectStatement<?> select,
			String name,
			boolean belongsToDao,
			@Nullable String sessionType,
			boolean addNonnullAnnotation) {
		this.annotationMeta = annotationMeta;
		this.select = select;
		this.name = name;
		this.belongsToDao = belongsToDao;
		this.reactive = Constants.MUTINY_SESSION.equals(sessionType);
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
	public String getAttributeDeclarationString() {
		final TreeSet<SqmParameter<?>> sortedParameters =
				new TreeSet<>( select.getSqmParameters() );
		StringBuilder declaration = new StringBuilder();
		comment( declaration );
		modifiers( declaration );
		returnType( declaration );
		parameters( sortedParameters, declaration );
		declaration
				.append(" {")
				.append("\n\treturn entityManager.createNamedQuery(")
				.append(fieldName())
				.append(")");
		for ( SqmParameter<?> param : sortedParameters ) {
			declaration
					.append("\n\t\t\t.setParameter(")
					.append(param.getName() == null ? param.getPosition() : '"' + param.getName() + '"')
					.append(", ")
					.append(param.getName() == null ? "parameter" + param.getPosition() : param.getName())
					.append(')');
		}
		declaration
				.append("\n\t\t\t.getResultList();\n}");
		return declaration.toString();
	}

	private String fieldName() {
		return "QUERY_" + nameToFieldName(name);
	}

	private String returnType() {
		final JavaType<?> javaType = select.getSelection().getJavaTypeDescriptor();
		if ( javaType != null ) {
			return javaType.getTypeName();
		}
		else {
			final List<SqmSelectableNode<?>> items =
					select.getQuerySpec().getSelectClause().getSelectionItems();
			if ( items.size() == 1 ) {
				final String typeName = items.get(0).getExpressible().getTypeName();
				final TypeElement entityType = entityType( typeName );
				return entityType == null ? typeName : entityType.getQualifiedName().toString();

			}
			else {
				return "Object[]";
			}
		}
	}

	void notNull(StringBuilder declaration) {
		if ( addNonnullAnnotation ) {
			declaration
					.append('@')
					.append(annotationMeta.importType("jakarta.annotation.Nonnull"))
					.append(' ');
		}
	}

	private void comment(StringBuilder declaration) {
		declaration
				.append("\n/**\n * Execute named query {@value #")
				.append(fieldName())
				.append("} defined by annotation of {@link ")
				.append(annotationMeta.getSimpleName())
				.append("}.\n **/\n");
	}

	private void modifiers(StringBuilder declaration) {
		declaration
				.append(belongsToDao ? "public " : "public static ");
	}

	private void returnType(StringBuilder declaration) {
		if ( reactive ) {
			declaration
					.append(annotationMeta.importType(Constants.UNI))
					.append('<');
		}
		declaration
				.append(annotationMeta.importType(Constants.LIST))
				.append('<')
				.append(annotationMeta.importType(returnType()))
				.append("> ")
				.append(name);
		if ( reactive ) {
			declaration
					.append('>');
		}
	}

	private void parameters(TreeSet<SqmParameter<?>> sortedParameters, StringBuilder declaration) {
		declaration
				.append('(');
		if ( !belongsToDao ) {
			notNull( declaration );
			declaration
					.append(annotationMeta.importType(Constants.ENTITY_MANAGER))
					.append(" entityManager");
		}
		int i = 0;
		for ( SqmParameter<?> param : sortedParameters) {
			if ( 0 < i++ || !belongsToDao ) {
				declaration
						.append(", ");
			}
			declaration
					.append(parameterType(param))
					.append(" ")
					.append(parameterName(param));
		}
		declaration
				.append(')');
	}

	private static String parameterName(SqmParameter<?> param) {
		return param.getName() == null ? "parameter" + param.getPosition() : param.getName();
	}

	private String parameterType(SqmParameter<?> param) {
		final SqmExpressible<?> expressible = param.getExpressible();
		final String paramType = expressible == null ? "unknown" : expressible.getTypeName(); //getTypeName() can return "unknown"
		return "unknown".equals(paramType) ? "Object" : annotationMeta.importType(paramType);
	}

	private @Nullable TypeElement entityType(String entityName) {
		TypeElement symbol =
				findEntityByUnqualifiedName( entityName,
						annotationMeta.getContext().getElementUtils().getModuleElement("") );
		if ( symbol != null ) {
			return symbol;
		}
		for ( ModuleElement module : annotationMeta.getContext().getElementUtils().getAllModuleElements() ) {
			symbol = findEntityByUnqualifiedName( entityName, module );
			if ( symbol != null ) {
				return symbol;
			}
		}
		return null;
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
		return name;
	}

	@Override
	public String getTypeDeclaration() {
		return Constants.LIST;
	}

	@Override
	public Metamodel getHostingEntity() {
		return annotationMeta;
	}
}
