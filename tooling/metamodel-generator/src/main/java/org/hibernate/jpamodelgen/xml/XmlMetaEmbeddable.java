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

// $Id$

package org.hibernate.jpamodelgen.xml;

import java.util.List;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import org.hibernate.jpamodelgen.Context;
import org.hibernate.jpamodelgen.model.MetaAttribute;
import org.hibernate.jpamodelgen.xml.jaxb.Embeddable;

/**
 * @author Hardy Ferentschik
 */
public class XmlMetaEmbeddable extends XmlMetaEntity {
	// Embeddables needs to be lazily initialized since the access type be determined by the class which is embedding
	// the entity. This might not be known until annotations are processed.
	// Also note, that if two different classes with different access types embed this entity the access type of the
	// embeddable will be the one of the last embedding entity processed. The result is not determined (that's ok
	// according to the spec)
	private boolean initialized;

	public XmlMetaEmbeddable(Embeddable embeddable, String packageName, TypeElement element, Context context) {
		super( embeddable, packageName, element, context );
	}

	public List<MetaAttribute> getMembers() {
		if ( !initialized ) {
			context.logMessage( Diagnostic.Kind.OTHER, "Entity " + getQualifiedName() + "was lazily initialised." );
			init();
			initialized = true;
		}
		return members;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "XmlMetaEmbeddable" );
		sb.append( "{accessTypeInfo=" ).append( accessTypeInfo );
		sb.append( ", clazzName='" ).append( clazzName ).append( '\'' );
		sb.append( ", members=" );
		if ( initialized ) {
			sb.append( members );
		}
		else {
			sb.append( "[un-initalized]" );
		}
		sb.append( ", isMetaComplete=" ).append( isMetaComplete );
		sb.append( '}' );
		return sb.toString();
	}
}