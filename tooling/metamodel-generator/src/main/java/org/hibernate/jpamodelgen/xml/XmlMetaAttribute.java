/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.xml;

import org.hibernate.jpamodelgen.model.MetaAttribute;
import org.hibernate.jpamodelgen.model.MetaEntity;
import org.hibernate.jpamodelgen.util.StringUtil;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Hardy Ferentschik
 */
public abstract class XmlMetaAttribute implements MetaAttribute {

	private final XmlMetaEntity hostingEntity;
	private final String propertyName;
	private final String type;

	XmlMetaAttribute(XmlMetaEntity parent, String propertyName, String type) {
		this.hostingEntity = parent;
		this.propertyName = propertyName;
		this.type = type;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public String getTypedAttributeDeclarationString(MetaEntity entityForImports, String mtype, List<? extends TypeMirror> toImport) {
		return "public static volatile " + hostingEntity.importType( getMetaType() )
				+ "<" + hostingEntity.importType( hostingEntity.getQualifiedName() )
				+ getNonGenericTypesFromTypeMirror(hostingEntity, toImport)
				+ ", " + hostingEntity.importType(mtype)
				+ "> " + getPropertyName() + ";";
	}

	@Override
	public String getAttributeDeclarationString(List<? extends TypeParameterElement> typeParameters) {
		return "public static volatile " + hostingEntity.importType( getMetaType() )
				+ "<" + hostingEntity.importType( hostingEntity.getQualifiedName() )
				+ getNonGenericTypesFromTypeParameter(hostingEntity,typeParameters)
				+ ", " + hostingEntity.importType( getTypeDeclaration() )
				+ "> " + getPropertyName() + ";";
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

	@Override
	public String getAttributeDeclarationString() {
		return "public static volatile " + hostingEntity.importType( getMetaType() )
				+ "<" + hostingEntity.importType( hostingEntity.getQualifiedName() )
				+ ", " + hostingEntity.importType( getTypeDeclaration() )
				+ "> " + getPropertyName() + ";";
	}

	@Override
	public String getAttributeNameDeclarationString(){
		return new StringBuilder().append("public static final ")
				.append(hostingEntity.importType(String.class.getName()))
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
		return propertyName;
	}

	public String getTypeDeclaration() {
		return type;
	}

	public MetaEntity getHostingEntity() {
		return hostingEntity;
	}

	@Override
	public abstract String getMetaType();

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "XmlMetaAttribute" );
		sb.append( "{propertyName='" ).append( propertyName ).append( '\'' );
		sb.append( ", type='" ).append( type ).append( '\'' );
		sb.append( '}' );
		return sb.toString();
	}
}
