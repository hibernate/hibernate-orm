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
package org.hibernate.jpamodelgen.annotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.tools.Diagnostic;

import org.hibernate.jpamodelgen.Context;
import org.hibernate.jpamodelgen.ImportContext;
import org.hibernate.jpamodelgen.ImportContextImpl;
import org.hibernate.jpamodelgen.MetaAttribute;
import org.hibernate.jpamodelgen.MetaEntity;
import org.hibernate.jpamodelgen.TypeUtils;

/**
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public class AnnotationMetaEntity implements MetaEntity {

	final TypeElement element;
	final protected ProcessingEnvironment pe;

	final ImportContext importContext;
	private Context context;
	//used to propagate the access type of the root entity over to subclasses, superclasses and embeddable
	private AccessType defaultAccessTypeForHierarchy;
	private AccessType defaultAccessTypeForElement;

	public AnnotationMetaEntity(ProcessingEnvironment pe, TypeElement element, Context context) {
		this.element = element;
		this.pe = pe;
		importContext = new ImportContextImpl( getPackageName() );
		this.context = context;
	}

	public AnnotationMetaEntity(ProcessingEnvironment pe, TypeElement element, Context context, AccessType accessType) {
		this( pe, element, context );
		this.defaultAccessTypeForHierarchy = accessType;
	}

	public String getSimpleName() {
		return element.getSimpleName().toString();
	}

	public String getQualifiedName() {
		return element.getQualifiedName().toString();
	}

	public String getPackageName() {
		PackageElement packageOf = pe.getElementUtils().getPackageOf( element );
		return pe.getElementUtils().getName( packageOf.getQualifiedName() ).toString();
	}

	public List<MetaAttribute> getMembers() {
		List<MetaAttribute> membersFound = new ArrayList<MetaAttribute>();
		final AccessType elementAccessType = getAccessTypeForElement();

		List<? extends Element> fieldsOfClass = ElementFilter.fieldsIn( element.getEnclosedElements() );
		addPersistentMembers( membersFound, elementAccessType, fieldsOfClass, AccessType.FIELD );

		List<? extends Element> methodsOfClass = ElementFilter.methodsIn( element.getEnclosedElements() );
		addPersistentMembers( membersFound, elementAccessType, methodsOfClass, AccessType.PROPERTY );

		//process superclasses
		for ( TypeElement superclass = TypeUtils.getSuperclass( element );
			  superclass != null;
			  superclass = TypeUtils.getSuperclass( superclass ) ) {
			if ( superclass.getAnnotation( Entity.class ) != null ) {
				break; //will be handled or has been handled already
			}
			else if ( superclass.getAnnotation( MappedSuperclass.class ) != null ) {
				//FIXME use the class defalut access type
				context.processElement( superclass, defaultAccessTypeForHierarchy );
			}
		}
		return membersFound;
	}

	private void addPersistentMembers(
			List<MetaAttribute> membersFound,
			AccessType elementAccessType,
			List<? extends Element> membersOfClass,
			AccessType membersKind) {
		AccessType explicitAccessType;
		if ( elementAccessType == membersKind ) {
			//all membersKind considered
			explicitAccessType = null;
		}
		else {
			//use membersKind only if marked with @Access(membersKind)
			explicitAccessType = membersKind;
		}
		for ( Element memberOfClass : membersOfClass ) {

			TypeVisitor visitor = new TypeVisitor( this, explicitAccessType );
			AnnotationMetaAttribute result = memberOfClass.asType().accept( visitor, memberOfClass );
			if ( result != null ) {
				membersFound.add( result );
			}
		}
	}

	private AccessType getAccessTypeForElement() {

		//get local strategy
		AccessType accessType = getAccessTypeForClass( element );
		if ( accessType == null ) {
			accessType = this.defaultAccessTypeForHierarchy;
		}
		if ( accessType == null ) {
			//we dont' know
			//if an enity go up
			//
			//superclasses alre always treated after their entities
			//and their access type are discovered
			//FIXME is it really true if only the superclass is changed
			TypeElement superClass = element;
			do {
				superClass = TypeUtils.getSuperclass( superClass );
				if ( superClass != null ) {
					if ( superClass.getAnnotation( Entity.class ) != null
							|| superClass.getAnnotation( MappedSuperclass.class ) != null ) {
						//FIXME make it work for XML
						AccessType superClassAccessType = getAccessTypeForClass( superClass );
						//we've reach the root entity and resolved Ids
						if ( superClassAccessType != null && defaultAccessTypeForHierarchy != null ) {
							break; //we've found it
						}
					}
					else {
						break; //neither @Entity nor @MappedSuperclass
					}
				}
			}
			while ( superClass != null );
		}

		if ( accessType == null ) {
			accessType = AccessType.PROPERTY; //default to property
			this.defaultAccessTypeForElement = accessType;
		}
		//this is a subclass so caching is OK
		//this.defaultAccessTypeForHierarchy = accessType;
		context.addAccessType( this.element, accessType );
		this.defaultAccessTypeForElement = accessType;
		return accessType;
	}

	private AccessType getAccessTypeForClass(TypeElement searchedElement) {
		context.logMessage( Diagnostic.Kind.NOTE, "check class " + searchedElement );
		AccessType accessType = context.getAccessType( searchedElement );

		if ( defaultAccessTypeForHierarchy == null ) {
			this.defaultAccessTypeForHierarchy = context.getDefaultAccessTypeForHerarchy( searchedElement );
		}
		if ( accessType != null ) {
			context.logMessage( Diagnostic.Kind.NOTE, "Found in cache" + searchedElement + ":" + accessType );
			return accessType;
		}

		/**
		 * when forcing access type, we can only override the defaultAccessTypeForHierarchy
		 * if we are the entity root (identified by having @Id or @EmbeddedId
		 */
		final Access accessAnn = searchedElement.getAnnotation( Access.class );
		AccessType forcedAccessType = accessAnn != null ? accessAnn.value() : null;
		if ( forcedAccessType != null ) {
			context.logMessage( Diagnostic.Kind.NOTE, "access type " + searchedElement + ":" + forcedAccessType );
			context.addAccessType( searchedElement, forcedAccessType );
		}

		//continue nevertheless to check if we are root and if defaultAccessTypeForHierarchy
		//should be overridden
		if ( forcedAccessType == null || defaultAccessTypeForHierarchy == null ) {
			List<? extends Element> myMembers = searchedElement.getEnclosedElements();
			for ( Element subElement : myMembers ) {
				List<? extends AnnotationMirror> entityAnnotations =
						pe.getElementUtils().getAllAnnotationMirrors( subElement );

				for ( Object entityAnnotation : entityAnnotations ) {
					AnnotationMirror annotationMirror = ( AnnotationMirror ) entityAnnotation;

					final String annotationType = annotationMirror.getAnnotationType().toString();

					//FIXME consider XML
					if ( annotationType.equals( Id.class.getName() )
							|| annotationType.equals( EmbeddedId.class.getName() ) ) {
						context.logMessage( Diagnostic.Kind.NOTE, "Found id on" + searchedElement );
						final ElementKind kind = subElement.getKind();
						if ( kind == ElementKind.FIELD || kind == ElementKind.METHOD ) {
							accessType = kind == ElementKind.FIELD ? AccessType.FIELD : AccessType.PROPERTY;
							//FIXME enlever in niveau
							if ( defaultAccessTypeForHierarchy == null ) {
								this.defaultAccessTypeForHierarchy = context.getDefaultAccessTypeForHerarchy(
										searchedElement
								);
								//we've discovered the class hierarchy, let's cache it
								if ( defaultAccessTypeForHierarchy == null ) {
									this.defaultAccessTypeForHierarchy = accessType;
									context.addAccessTypeForHierarchy( searchedElement, defaultAccessTypeForHierarchy );
									//FIXME should we add
									//context.addAccessTypeForHierarchy( element, defaultAccessTypeForHierarchy );
								}
							}
							if ( forcedAccessType == null ) {
								context.addAccessType( searchedElement, accessType );
								context.logMessage(
										Diagnostic.Kind.NOTE, "access type " + searchedElement + ":" + accessType
								);
								return accessType;
							}
							else {
								return forcedAccessType;
							}
						}
					}
				}
			}
		}
		return forcedAccessType;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "MetaEntity" );
		sb.append( "{element=" ).append( element );
		sb.append( '}' );
		return sb.toString();
	}

	static Map<String, String> COLLECTIONS = new HashMap<String, String>();

	static {
		COLLECTIONS.put( "java.util.Collection", "javax.persistence.metamodel.CollectionAttribute" );
		COLLECTIONS.put( "java.util.Set", "javax.persistence.metamodel.SetAttribute" );
		COLLECTIONS.put( "java.util.List", "javax.persistence.metamodel.ListAttribute" );
		COLLECTIONS.put( "java.util.Map", "javax.persistence.metamodel.MapAttribute" );
	}

	class TypeVisitor extends SimpleTypeVisitor6<AnnotationMetaAttribute, Element> {

		AnnotationMetaEntity parent;
		//if null, process all members as implicit
		//if not null, only process members marked as @Access(explicitAccessType)
		private AccessType explicitAccessType;

		TypeVisitor(AnnotationMetaEntity parent, AccessType explicitAccessType) {
			this.parent = parent;
			this.explicitAccessType = explicitAccessType;
		}

		@Override
		protected AnnotationMetaAttribute defaultAction(TypeMirror e, Element p) {
			return super.defaultAction( e, p );
		}

		@Override
		public AnnotationMetaAttribute visitPrimitive(PrimitiveType t, Element element) {
			if ( isPersistent( element ) ) {
				return new AnnotationMetaSingleAttribute( parent, element, TypeUtils.toTypeString( t ) );
			}
			else {
				return null;
			}
		}

		@Override
		public AnnotationMetaAttribute visitArray(ArrayType t, Element element) {
			if ( isPersistent( element ) ) {
				return new AnnotationMetaSingleAttribute( parent, element, TypeUtils.toTypeString( t ) );
			}
			else {
				return null;
			}
		}

		private boolean isPersistent(Element element) {
			//FIXME consider XML
			boolean correctAccessType = false;
			if ( this.explicitAccessType == null ) {
				correctAccessType = true;
			}
			else {
				final Access accessAnn = element.getAnnotation( Access.class );
				if ( accessAnn != null && explicitAccessType.equals( accessAnn.value() ) ) {
					correctAccessType = true;
				}
			}
			return correctAccessType
					&& element.getAnnotation( Transient.class ) == null
					&& !element.getModifiers().contains( Modifier.TRANSIENT )
					&& !element.getModifiers().contains( Modifier.STATIC );

		}

		@Override
		public AnnotationMetaAttribute visitDeclared(DeclaredType t, Element element) {
			//FIXME consider XML
			if ( isPersistent( element ) ) {
				TypeElement returnedElement = ( TypeElement ) pe.getTypeUtils().asElement( t );
				String collection = COLLECTIONS.get( returnedElement.getQualifiedName().toString() ); // WARNING: .toString() is necessary here since Name equals does not compare to String

				if ( collection != null ) {
					//collection of element
					if ( element.getAnnotation( ElementCollection.class ) != null ) {
						final TypeMirror collectionType = t.getTypeArguments().get( 0 );
						final TypeElement collectionElement = ( TypeElement ) pe.getTypeUtils()
								.asElement( collectionType );
						this.parent.context.processElement(
								collectionElement,
								this.parent.defaultAccessTypeForElement
						);
					}
					if ( collection.equals( "javax.persistence.metamodel.MapAttribute" ) ) {
						return new AnnotationMetaMap(
								parent, element, collection, getKeyType( t ), getElementType( t )
						);
					}
					else {
						return new AnnotationMetaCollection( parent, element, collection, getElementType( t ) );
					}
				}
				else {
					//FIXME Consider XML
					if ( element.getAnnotation( Embedded.class ) != null
							|| returnedElement.getAnnotation( Embeddable.class ) != null ) {
						this.parent.context.processElement(
								returnedElement,
								this.parent.defaultAccessTypeForElement
						);
					}
					return new AnnotationMetaSingleAttribute(
							parent, element, returnedElement.getQualifiedName().toString()
					);
				}
			}
			else {
				return null;
			}
		}

		@Override
		public AnnotationMetaAttribute visitExecutable(ExecutableType t, Element p) {
			String string = p.getSimpleName().toString();

			// TODO: implement proper property get/is/boolean detection
			if ( string.startsWith( "get" ) || string.startsWith( "is" ) ) {
				TypeMirror returnType = t.getReturnType();

				return returnType.accept( this, p );
			}
			else {
				return null;
			}
		}
	}

	public String generateImports() {
		return importContext.generateImports();
	}

	public String importType(String fqcn) {
		return importContext.importType( fqcn );
	}

	public String staticImport(String fqcn, String member) {
		return importContext.staticImport( fqcn, member );
	}

	public String importType(Name qualifiedName) {
		return importType( qualifiedName.toString() );
	}

	public TypeElement getTypeElement() {
		return element;
	}

	private String getKeyType(DeclaredType t) {
		return t.getTypeArguments().get( 0 ).toString();
	}

	private String getElementType(DeclaredType declaredType) {
		if ( declaredType.getTypeArguments().size() == 1 ) {
			return declaredType.getTypeArguments().get( 0 ).toString();
		}
		else {
			return declaredType.getTypeArguments().get( 1 ).toString();
		}
	}
}
