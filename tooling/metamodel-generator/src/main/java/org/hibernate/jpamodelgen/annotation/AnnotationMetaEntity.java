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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.persistence.AccessType;
import javax.persistence.Transient;

import org.hibernate.jpamodelgen.AccessTypeInformation;
import org.hibernate.jpamodelgen.Context;
import org.hibernate.jpamodelgen.ImportContextImpl;
import org.hibernate.jpamodelgen.model.ImportContext;
import org.hibernate.jpamodelgen.model.MetaAttribute;
import org.hibernate.jpamodelgen.model.MetaEntity;
import org.hibernate.jpamodelgen.util.TypeUtils;

/**
 * Class used to collect meta information about an annotated entity.
 *
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public class AnnotationMetaEntity implements MetaEntity {

	private final ImportContext importContext;
	private final TypeElement element;
	private final Map<String, MetaAttribute> members;
	private Context context;

	private AccessTypeInformation entityAccessTypeInfo;

	public AnnotationMetaEntity(TypeElement element, Context context) {
		this( element, context, false );
	}

	protected AnnotationMetaEntity(TypeElement element, Context context, boolean lazilyInitialised) {
		this.element = element;
		this.context = context;
		this.members = new HashMap<String, MetaAttribute>();
		this.importContext = new ImportContextImpl( getPackageName() );
		if ( !lazilyInitialised ) {
			init();
		}
	}

	public AccessTypeInformation getEntityAccessTypeInfo() {
		return entityAccessTypeInfo;
	}

	public final Context getContext() {
		return context;
	}

	public final String getSimpleName() {
		return element.getSimpleName().toString();
	}

	public final String getQualifiedName() {
		return element.getQualifiedName().toString();
	}

	public final String getPackageName() {
		PackageElement packageOf = context.getElementUtils().getPackageOf( element );
		return context.getElementUtils().getName( packageOf.getQualifiedName() ).toString();
	}

	public List<MetaAttribute> getMembers() {
		return new ArrayList<MetaAttribute>( members.values() );
	}

	@Override
	public boolean isMetaComplete() {
		return false;
	}

	public void mergeInMembers(Collection<MetaAttribute> attributes) {
		for ( MetaAttribute attribute : attributes ) {
			members.put( attribute.getPropertyName(), attribute );
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "AnnotationMetaEntity" );
		sb.append( "{element=" ).append( element );
		sb.append( ", members=" ).append( members );
		sb.append( '}' );
		return sb.toString();
	}

	protected TypeElement getElement() {
		return element;
	}

	private void addPersistentMembers(List<? extends Element> membersOfClass, AccessType membersKind) {
		for ( Element memberOfClass : membersOfClass ) {
			AccessType forcedAccessType = TypeUtils.determineAnnotationSpecifiedAccessType( memberOfClass );
			if ( entityAccessTypeInfo.getAccessType() != membersKind && forcedAccessType == null ) {
				continue;
			}

			if ( TypeUtils.containsAnnotation( memberOfClass, Transient.class )
					|| memberOfClass.getModifiers().contains( Modifier.TRANSIENT )
					|| memberOfClass.getModifiers().contains( Modifier.STATIC ) ) {
				continue;
			}

			MetaAttributeGenerationVisitor visitor = new MetaAttributeGenerationVisitor( this, context );
			AnnotationMetaAttribute result = memberOfClass.asType().accept( visitor, memberOfClass );
			if ( result != null ) {
				members.put( result.getPropertyName(), result );
			}
		}
	}

	protected final void init() {
		TypeUtils.determineAccessTypeForHierarchy( element, context );
		entityAccessTypeInfo = context.getAccessTypeInfo( getQualifiedName() );

		List<? extends Element> fieldsOfClass = ElementFilter.fieldsIn( element.getEnclosedElements() );
		addPersistentMembers( fieldsOfClass, AccessType.FIELD );

		List<? extends Element> methodsOfClass = ElementFilter.methodsIn( element.getEnclosedElements() );
		addPersistentMembers( methodsOfClass, AccessType.PROPERTY );
	}

	public final String generateImports() {
		return importContext.generateImports();
	}

	public final String importType(String fqcn) {
		return importContext.importType( fqcn );
	}

	public final String staticImport(String fqcn, String member) {
		return importContext.staticImport( fqcn, member );
	}

	public final String importType(Name qualifiedName) {
		return importType( qualifiedName.toString() );
	}

	public final TypeElement getTypeElement() {
		return element;
	}
}
