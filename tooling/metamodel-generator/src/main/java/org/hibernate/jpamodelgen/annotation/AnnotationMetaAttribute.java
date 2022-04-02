/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.annotation;

import java.beans.Introspector;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

import org.hibernate.jpamodelgen.model.MetaAttribute;
import org.hibernate.jpamodelgen.model.MetaEntity;
import org.hibernate.jpamodelgen.util.StringUtil;

/**
 * Captures all information about an annotated persistent attribute.
 *
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public abstract class AnnotationMetaAttribute implements MetaAttribute {

	private final Element element;
	private final AnnotationMetaEntity parent;
	private final String type;

	public AnnotationMetaAttribute(AnnotationMetaEntity parent, Element element, String type) {
		this.element = element;
		this.parent = parent;
		this.type = type;
	}

	public String getType(){
		return element.asType().toString();
	}

	public String getTypedAttributeDeclarationString(MetaEntity entityForImports, String mtype, List<? extends TypeMirror> toImport) {
		return "public static volatile " +
				entityForImports.importType(getMetaType()) +
				"<" +
				entityForImports.importType(parent.getQualifiedName()) +
				getNonGenericTypesFromTypeMirror(entityForImports, toImport) +
				", " +
				entityForImports.importType(mtype) +
				"> " +
				getPropertyName() +
				";";
	}

	protected String getNonGenericTypesFromTypeMirror(MetaEntity entityForImports, List<? extends TypeMirror>list) {
		return getFilteredTypesString(entityForImports, list, it -> it.getKind() != TypeKind.TYPEVAR, TypeMirror::toString);
	}

	protected String getNonGenericTypesFromTypeParameter(MetaEntity entityForImports, List<? extends TypeParameterElement> list) {
		return getFilteredTypesString(entityForImports, list, it -> it.getKind() != ElementKind.TYPE_PARAMETER, it -> it.getSimpleName().toString());
	}

	protected <T> String getFilteredTypesString(MetaEntity entityForImports, List<T> types, Predicate<T> predicate, Function<T, String> mapper) {
		List<T> filtered = types.stream()
				.filter(predicate)
				.collect(Collectors.toList());
		if(filtered.isEmpty() || filtered.size() < types.size()){
			return "";
		}
		return "<" + types.stream()
				.map(mapper)
				.map(entityForImports::importType)
				.collect(Collectors.joining(",")) + ">";
	}

	public String getAttributeDeclarationString(List<? extends TypeParameterElement> toImport) {
		return "public static volatile " +
				parent.importType(getMetaType()) +
				"<" +
				parent.importType(parent.getQualifiedName()) +
				getNonGenericTypesFromTypeParameter(parent, toImport) +
				", " +
				parent.importType(getTypeDeclaration()) +
				"> " +
				getPropertyName() +
				";";
	}

	@Override
	public String getAttributeDeclarationString() {
		return new StringBuilder().append( "public static volatile " )
				.append( parent.importType( getMetaType() ) )
				.append( "<" )
				.append( parent.importType( parent.getQualifiedName() ) )
				.append( ", " )
				.append( parent.importType( getTypeDeclaration() ) )
				.append( "> " )
				.append( getPropertyName() )
				.append( ";" )
				.toString();
	}

	@Override
	public String getAttributeNameDeclarationString(){
		return new StringBuilder().append("public static final ")
				.append(parent.importType(String.class.getName()))
				.append(" ")
				.append(StringUtil.getUpperUnderscoreCaseFromLowerCamelCase(getPropertyName()))
				.append(" = ")
				.append("\"")
				.append(getPropertyName())
				.append("\"")
				.append(";")
				.toString();
	}

	public String getPropertyName() {
		Elements elementsUtil = parent.getContext().getElementUtils();
		if ( element.getKind() == ElementKind.FIELD ) {
			return element.getSimpleName().toString();
		}
		else if ( element.getKind() == ElementKind.METHOD ) {
			String name = element.getSimpleName().toString();
			if ( name.startsWith( "get" ) ) {
				return elementsUtil.getName( Introspector.decapitalize( name.substring( "get".length() ) ) ).toString();
			}
			else if ( name.startsWith( "is" ) ) {
				return ( elementsUtil.getName( Introspector.decapitalize( name.substring( "is".length() ) ) ) ).toString();
			}
			return elementsUtil.getName( Introspector.decapitalize( name ) ).toString();
		}
		else {
			return elementsUtil.getName( element.getSimpleName() + "/* " + element.getKind() + " */" ).toString();
		}
	}

	public MetaEntity getHostingEntity() {
		return parent;
	}

	public abstract String getMetaType();

	public String getTypeDeclaration() {
		return type;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "AnnotationMetaAttribute" );
		sb.append( "{element=" ).append( element );
		sb.append( ", type='" ).append( type ).append( '\'' );
		sb.append( '}' );
		return sb.toString();
	}
}
