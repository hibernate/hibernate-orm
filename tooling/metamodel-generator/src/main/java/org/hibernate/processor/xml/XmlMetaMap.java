/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.xml;

import org.hibernate.processor.model.Metamodel;

/**
 * @author Hardy Ferentschik
 */
public class XmlMetaMap extends XmlMetaCollection {

	private final String keyType;

	public XmlMetaMap(XmlMetaEntity parent, String propertyName, String type, String collectionType, String keyType) {
		super( parent, propertyName, type, collectionType );
		this.keyType = keyType;
	}

	@Override
	public String getAttributeDeclarationString() {
		final Metamodel hostingEntity = getHostingEntity();
		return new StringBuilder().append( "public static volatile " )
				.append( hostingEntity.importType( getMetaType() ) )
				.append( "<" )
				.append( hostingEntity.importType( hostingEntity.getQualifiedName() ) )
				.append( ", " )
				.append( hostingEntity.importType( keyType ) )
				.append( ", " )
				.append( hostingEntity.importType( getTypeDeclaration() ) )
				.append( "> " )
				.append( getPropertyName() )
				.append( ";" )
				.toString();
	}
}
