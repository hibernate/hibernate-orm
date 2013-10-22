/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.jpamodelgen.xml;

import org.hibernate.jpamodelgen.model.MetaAttribute;
import org.hibernate.jpamodelgen.model.MetaEntity;

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
	public String getDeclarationString() {
		return "public static volatile " + hostingEntity.importType( getMetaType() )
				+ "<" + hostingEntity.importType( hostingEntity.getQualifiedName() )
				+ ", " + hostingEntity.importType( getTypeDeclaration() )
				+ "> " + getPropertyName() + ";";
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
