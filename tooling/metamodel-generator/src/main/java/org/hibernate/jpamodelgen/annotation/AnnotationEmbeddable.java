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

import java.util.List;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import org.hibernate.jpamodelgen.Context;
import org.hibernate.jpamodelgen.model.MetaAttribute;

/**
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public class AnnotationEmbeddable extends AnnotationMetaEntity {

	// Embeddables needs to be lazily initialized since the access type be determined by the class which is embedding
	// the entity. This might not be known until annotations are processed.
	// Also note, that if two different classes with different access types embed this entity the access type of the
	// embeddable will be the one of the last embedding entity processed. The result is not determined (that's ok
	// according to the spec)
	private boolean initialized;

	public AnnotationEmbeddable(TypeElement element, Context context) {
		super( element, context, true );
	}

	public List<MetaAttribute> getMembers() {
		if ( !initialized ) {
			getContext().logMessage( Diagnostic.Kind.OTHER, "Entity " + getQualifiedName() + " was lazily initialised." );
			init();
			initialized = true;
		}
		return super.getMembers();
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "AnnotationEmbeddable" );
		sb.append( "{element=" ).append( getElement() );
		sb.append( ", members=" );
		if ( initialized ) {
			sb.append( getMembers() );
		}
		else {
			sb.append( "[un-initalized]" );
		}
		sb.append( '}' );
		return sb.toString();
	}
}