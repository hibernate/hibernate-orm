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
package org.hibernate.jpamodelgen.annotation;

import javax.lang.model.element.Element;

/**
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public class AnnotationMetaMap extends AnnotationMetaCollection {

	private final String keyType;

	public AnnotationMetaMap(AnnotationMetaEntity parent, Element element, String collectionType,
			String keyType, String elementType) {
		super( parent, element, collectionType, elementType );
		this.keyType = keyType;
	}

	public String getDeclarationString() {
		return "public static volatile " + getHostingEntity().importType( getMetaType() )
				+ "<" + getHostingEntity().importType( getHostingEntity().getQualifiedName() )
				+ ", " + getHostingEntity().importType( keyType ) + ", "
				+ getHostingEntity().importType( getTypeDeclaration() ) + "> "
				+ getPropertyName() + ";";
	}
}
