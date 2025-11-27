/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.processor.Context;
import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;
import org.hibernate.processor.util.Constants;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.type.descriptor.java.JavaType;

import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.List;
import java.util.TreeSet;

import static org.hibernate.processor.util.StringUtil.nameToFieldName;
import static org.hibernate.processor.validation.ProcessorSessionFactory.findEntityByUnqualifiedName;

/**
 * @author Gavin King
 */
class NamedQueryMethod implements MetaAttribute {
	private final AnnotationMeta annotationMeta;
	private final SqmSelectStatement<?> select;
	private final String name;
	private final boolean belongsToRepository;
	private final boolean reactive;
	private final String sessionVariableName;
	private final boolean addNonnullAnnotation;

	public NamedQueryMethod(
			AnnotationMeta annotationMeta,
			SqmSelectStatement<?> select,
			String name,
			boolean belongsToRepository,
			@Nullable String sessionType,
			String sessionVariableName,
			boolean addNonnullAnnotation) {
		this.annotationMeta = annotationMeta;
		this.select = select;
		this.name = name;
		this.belongsToRepository = belongsToRepository;
		this.reactive = Constants.MUTINY_SESSION.equals(sessionType);
		this.sessionVariableName = sessionVariableName;
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
		final TreeSet<SqmParameter<?>> sortedParameters = new TreeSet<>( SqmParameter.COMPARATOR );
		sortedParameters.addAll( select.getSqmParameters() );
		StringBuilder declaration = new StringBuilder();
		comment( declaration );
		modifiers( declaration );
		returnType( declaration );
		parameters( sortedParameters, declaration );
		declaration
				.append(" {")
				.append("\n\treturn ")
				.append(sessionVariableName)
				.append(".createNamedQuery(")
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
			final SqmExpressible<?> expressible;
			if ( items.size() == 1 && ( expressible = items.get( 0 ).getExpressible() ) != null ) {
				final String typeName = expressible.getTypeName();
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
				.append(belongsToRepository ? "public " : "public static ");
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
		if ( !belongsToRepository) {
			notNull( declaration );
			declaration
					.append(annotationMeta.importType(Constants.ENTITY_MANAGER))
					.append(" ")
					.append(sessionVariableName);
		}
		int i = 0;
		for ( SqmParameter<?> param : sortedParameters) {
			if ( 0 < i++ || !belongsToRepository) {
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
		final Context context = annotationMeta.getContext();
		final Elements elementUtils = context.getElementUtils();
		final String qualifiedName = context.qualifiedNameForEntityName(entityName);
		if ( qualifiedName != null ) {
			return elementUtils.getTypeElement(qualifiedName);
		}
		TypeElement symbol =
				findEntityByUnqualifiedName( entityName,
						elementUtils.getModuleElement("") );
		if ( symbol != null ) {
			return symbol;
		}
		for ( ModuleElement module : elementUtils.getAllModuleElements() ) {
			symbol = findEntityByUnqualifiedName( entityName, module );
			if ( symbol != null ) {
				return symbol;
			}
		}
		return null;
	}

	@Override
	public String getAttributeNameDeclarationString() {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public String getMetaType() {
		throw new UnsupportedOperationException("operation not supported");
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
