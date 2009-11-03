// $Id$
/*
* JBoss, Home of Professional Open Source
* Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.hibernate.jpamodelgen;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import javax.lang.model.element.TypeElement;
import javax.persistence.AccessType;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;

import org.hibernate.jpamodelgen.annotation.AnnotationMetaEntity;

/**
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public class Context {
	//used to cache access types
	private Map<TypeElement, AccessTypeHolder> accessTypes = new HashMap<TypeElement, AccessTypeHolder>();
	private Set<String> elementsAlreadyProcessed = new HashSet<String>();
	private ProcessingEnvironment pe;
	private final Map<String, MetaEntity> metaEntitiesToProcess = new HashMap<String, MetaEntity>();
	private final Map<String, MetaEntity> metaSuperclassAndEmbeddableToProcess = new HashMap<String, MetaEntity>();

	private static class AccessTypeHolder {
		public AccessType elementAccessType;
		public AccessType hierarchyAccessType;
	}

	public Context(ProcessingEnvironment pe) {
		this.pe = pe;
	}

	public Map<String, MetaEntity> getMetaEntitiesToProcess() {
		return metaEntitiesToProcess;
	}

	public Map<String, MetaEntity> getMetaSuperclassAndEmbeddableToProcess() {
		return metaSuperclassAndEmbeddableToProcess;
	}

	public void addAccessType(TypeElement element, AccessType accessType) {
		AccessTypeHolder typeHolder = accessTypes.get( element );
		if ( typeHolder == null ) {
			typeHolder = new AccessTypeHolder();
			accessTypes.put( element, typeHolder );
		}
		typeHolder.elementAccessType = accessType;
	}

	public void addAccessTypeForHierarchy(TypeElement element, AccessType accessType) {
		AccessTypeHolder typeHolder = accessTypes.get( element );
		if ( typeHolder == null ) {
			typeHolder = new AccessTypeHolder();
			accessTypes.put( element, typeHolder );
		}
		typeHolder.hierarchyAccessType = accessType;
	}

	public AccessType getAccessType(TypeElement element) {
		final AccessTypeHolder typeHolder = accessTypes.get( element );
		return typeHolder != null ? typeHolder.elementAccessType : null;
	}

	public AccessType getDefaultAccessTypeForHerarchy(TypeElement element) {
		final AccessTypeHolder typeHolder = accessTypes.get( element );
		return typeHolder != null ? typeHolder.hierarchyAccessType : null;
	}

	public Set<String> getElementsAlreadyProcessed() {
		return elementsAlreadyProcessed;
	}

	//only process Embeddable or Superclass
	//does not work for Entity (risk of circularity)
	public void processElement(TypeElement element, AccessType defaultAccessTypeForHierarchy) {
		if ( elementsAlreadyProcessed.contains( element.getQualifiedName().toString() ) ) {
			pe.getMessager().printMessage( Diagnostic.Kind.WARNING, "Element already processed (ignoring): " + element );
			return;
		}

		ClassWriter.writeFile( new AnnotationMetaEntity( pe, element, this, defaultAccessTypeForHierarchy ), pe, this );
		elementsAlreadyProcessed.add( element.getQualifiedName().toString() );
	}
}
