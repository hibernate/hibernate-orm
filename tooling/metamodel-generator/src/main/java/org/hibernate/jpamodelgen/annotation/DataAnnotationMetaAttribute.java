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

import javax.lang.model.element.Element;

import static org.hibernate.jpamodelgen.util.TypeUtils.propertyName;

/**
 * Captures all information about an annotated persistent attribute.
 *
 * @author Gavin King
 */
public class DataAnnotationMetaAttribute implements MetaAttribute {

	final Element element;
	final AnnotationMetaEntity parent;
	private final String type;

	public DataAnnotationMetaAttribute(AnnotationMetaEntity parent, Element element, String type) {
		this.element = element;
		this.parent = parent;
		this.type = type;
	}

	@Override
	public boolean hasTypedAttribute() {
		return true;
	}

	@Override
	public boolean hasStringAttribute() {
		return false;
	}

	private boolean isTextual() {
		return String.class.getName().equals(type);
	}

	@Override
	public String getAttributeDeclarationString() {
		final String className = parent.importType( parent.getQualifiedName() );
		final String memberName = element.getSimpleName().toString();
		return new StringBuilder()
				.append("\n/**\n * @see ")
				.append(className)
				.append( "#")
				.append(memberName)
				.append( "\n **/\n" )
				.append( "public static volatile " )
				.append( parent.importType( getMetaType() ) )
				.append( "<" )
				.append( className )
				.append( "> " )
				.append( getPropertyName() )
				.append(" = ")
				.append( getMetaImpl(className, memberName) )
				.append( ";" )
				.toString();
	}

	private String getMetaImpl(String className, String memberName) {
		parent.importType(Constants.JD_SORT);
		return (isTextual() ? TEXT_IMPL : SORTABLE_IMPL)
				.replace("Entity", className )
				.replace( "\"name\"", "\"" + memberName + "\"" );
	}

	private static final String TEXT_IMPL =
			"new TextAttribute<>() {\n" +
			"\t\t@Override\n" +
			"\t\tpublic Sort<Entity> ascIgnoreCase() {\n" +
			"\t\t\treturn Sort.ascIgnoreCase(name());\n" +
			"\t\t}\n" +
			"\n" +
			"\t\t@Override\n" +
			"\t\tpublic Sort<Entity> descIgnoreCase() {\n" +
			"\t\t\treturn Sort.descIgnoreCase(name());\n" +
			"\t\t}\n" +
			"\n" +
			"\t\t@Override\n" +
			"\t\tpublic Sort<Entity> asc() {\n" +
			"\t\t\treturn Sort.asc(name());\n" +
			"\t\t}\n" +
			"\n" +
			"\t\t@Override\n" +
			"\t\tpublic Sort<Entity> desc() {\n" +
			"\t\t\treturn Sort.desc(name());\n" +
			"\t\t}\n" +
			"\n" +
			"\t\t@Override\n" +
			"\t\tpublic String name() {\n" +
			"\t\t\treturn \"name\";\n" +
			"\t\t}\n" +
			"\t}";

	private static final String SORTABLE_IMPL =
			"new SortableAttribute<>() {\n" +
			"\t\t@Override\n" +
			"\t\tpublic Sort<Entity> asc() {\n" +
			"\t\t\treturn Sort.asc(name());\n" +
			"\t\t}\n" +
			"\n" +
			"\t\t@Override\n" +
			"\t\tpublic Sort<Entity> desc() {\n" +
			"\t\t\treturn Sort.desc(name());\n" +
			"\t\t}\n" +
			"\n" +
			"\t\t@Override\n" +
			"\t\tpublic String name() {\n" +
			"\t\t\treturn \"name\";\n" +
			"\t\t}\n" +
			"\t}";

	@Override
	public String getAttributeNameDeclarationString(){
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public String getPropertyName() {
		return propertyName( parent, element );
	}

	@Override
	public Metamodel getHostingEntity() {
		return parent;
	}

	@Override
	public String getMetaType() {
		return isTextual()
				? "jakarta.data.metamodel.TextAttribute"
				: "jakarta.data.metamodel.SortableAttribute";
	}

	@Override
	public String getTypeDeclaration() {
		return type;
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append( "DataAnnotationMetaAttribute" )
				.append( "{element=" ).append( element )
				.append( ", type='" ).append( type ).append( '\'' )
				.append( '}' ).toString();
	}
}
