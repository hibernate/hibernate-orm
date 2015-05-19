/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.annotation;

import java.beans.Introspector;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.util.Elements;

import org.hibernate.jpamodelgen.model.MetaAttribute;
import org.hibernate.jpamodelgen.model.MetaEntity;

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

	public String getDeclarationString() {
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
