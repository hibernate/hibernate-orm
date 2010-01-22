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
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
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
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
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

	static Map<String, String> COLLECTIONS = new HashMap<String, String>();

	static {
		COLLECTIONS.put( "java.util.Collection", "javax.persistence.metamodel.CollectionAttribute" );
		COLLECTIONS.put( "java.util.Set", "javax.persistence.metamodel.SetAttribute" );
		COLLECTIONS.put( "java.util.List", "javax.persistence.metamodel.ListAttribute" );
		COLLECTIONS.put( "java.util.Map", "javax.persistence.metamodel.MapAttribute" );
	}

	private final TypeElement element;
	private final ImportContext importContext;
	private Context context;
	//used to propagate the access type of the root entity over to subclasses, superclasses and embeddable
	private AccessType defaultAccessTypeForHierarchy;
	private AccessType defaultAccessTypeForElement;

	public AnnotationMetaEntity(TypeElement element, Context context) {
		this.element = element;
		this.context = context;
		importContext = new ImportContextImpl( getPackageName() );
	}

	public AnnotationMetaEntity(TypeElement element, Context context, AccessType accessType) {
		this( element, context );
		this.defaultAccessTypeForHierarchy = accessType;
	}

	public Context getContext() {
		return context;
	}

	public String getSimpleName() {
		return element.getSimpleName().toString();
	}

	public String getQualifiedName() {
		return element.getQualifiedName().toString();
	}

	public String getPackageName() {
		PackageElement packageOf = context.getProcessingEnvironment().getElementUtils().getPackageOf( element );
		return context.getProcessingEnvironment().getElementUtils().getName( packageOf.getQualifiedName() ).toString();
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
			//we don't know if an entity go up
			//
			//superclasses are always treated after their entities
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
						context.getProcessingEnvironment().getElementUtils().getAllAnnotationMirrors( subElement );

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
				TypeElement returnedElement = ( TypeElement ) context.getProcessingEnvironment()
						.getTypeUtils()
						.asElement( t );
				String fqElementName = returnedElement.getQualifiedName()
						.toString();  // WARNING: .toString() is necessary here since Name equals does not compare to String
				String collection = COLLECTIONS.get( fqElementName );
				final List<? extends AnnotationMirror> annotations = element.getAnnotationMirrors();
				String targetEntity = getTargetEntity( annotations );
				if ( collection != null ) {
					if ( containsAnnotation( annotations, ElementCollection.class ) ) {
						//FIXME I don't understand why this code is different between Elementcollection and a regular collection but it needs to take targetClass into account
						TypeMirror collectionElementType = getCollectionElementType( t, fqElementName );
						final TypeElement collectionElement = ( TypeElement ) context.getProcessingEnvironment()
								.getTypeUtils()
								.asElement( collectionElementType );
						this.parent.context.processElement(
								collectionElement,
								this.parent.defaultAccessTypeForElement
						);
					}
					if ( collection.equals( "javax.persistence.metamodel.MapAttribute" ) ) {
						return new AnnotationMetaMap(
								//FIXME support targetEntity for map's key @MapKeyClass
								parent, element, collection, getKeyType( t ), getElementType( t, targetEntity )
						);
					}
					else {
						return new AnnotationMetaCollection( parent, element, collection, getElementType( t, targetEntity ) );
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

		private TypeMirror getCollectionElementType(DeclaredType t, String fqElementName) {
			TypeMirror collectionElementType;
			if ( Map.class.getCanonicalName().equals( fqElementName ) ) {
				collectionElementType = t.getTypeArguments().get( 1 );
			}
			else {
				collectionElementType = t.getTypeArguments().get( 0 );
			}
			return collectionElementType;
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

	private boolean containsAnnotation(List<? extends AnnotationMirror> annotations, Class<?> annotation) {
		String annString = annotation.getName();
		for ( AnnotationMirror mirror : annotations ) {
			if ( annString.equals( mirror.getAnnotationType().toString() ) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns targetEntity or null if no targetEntity is here or if equals to void
	 */
	private String getTargetEntity(List<? extends AnnotationMirror> annotations) {
		final String elementCollection = ElementCollection.class.getName();

		for ( AnnotationMirror mirror : annotations ) {
			final String annotation = mirror.getAnnotationType().toString();
			if ( elementCollection.equals( annotation ) ) {
				final String targetEntity = getTargetEntity( mirror );
				if (targetEntity != null) return targetEntity;
			}
			else if ( OneToMany.class.getName().equals( annotation ) ) {
				final String targetEntity = getTargetEntity( mirror );
				if (targetEntity != null) return targetEntity;
			}
			else if ( ManyToMany.class.getName().equals( annotation ) ) {
				final String targetEntity = getTargetEntity( mirror );
				if (targetEntity != null) return targetEntity;
			}
			else if ( ManyToOne.class.getName().equals( annotation ) ) {
				final String targetEntity = getTargetEntity( mirror );
				if (targetEntity != null) return targetEntity;
			}
			else if ( OneToOne.class.getName().equals( annotation ) ) {
				final String targetEntity = getTargetEntity( mirror );
				if (targetEntity != null) return targetEntity;
			}
		}
		return null;
	}

	private String getTargetEntity(AnnotationMirror mirror) {
		String targetEntity = null;
		final Map<? extends ExecutableElement, ? extends AnnotationValue> attributes = mirror.getElementValues();
		for ( Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : attributes.entrySet() ) {
			final String simpleName = entry.getKey().getSimpleName().toString();
			if ( "targetEntity".equals( simpleName ) ) {
				targetEntity = entry.getValue().toString();
				break;
			}
		}
		targetEntity = ( "void.class".equals( targetEntity ) ) ? null : targetEntity;
		return targetEntity != null && targetEntity.endsWith( ".class" ) ?
				targetEntity.substring( 0, targetEntity.length() - 6 ) : null;
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
		return TypeUtils.extractClosestRealTypeAsString( t.getTypeArguments().get( 0 ), context );
	}

	private String getElementType(DeclaredType declaredType, String targetEntity) {
		if (targetEntity != null) return targetEntity;
		final List<? extends TypeMirror> mirrors = declaredType.getTypeArguments();
		if ( mirrors.size() == 1 ) {
			final TypeMirror type = mirrors.get( 0 );
			return TypeUtils.extractClosestRealTypeAsString( type, context );
		}
		else if ( mirrors.size() == 2 ) {
			return TypeUtils.extractClosestRealTypeAsString( mirrors.get( 1 ), context );
		}
		else {
			//for 0 or many
			//0 is expected, many is not
			if ( mirrors.size() > 2) {
				context.logMessage( Diagnostic.Kind.WARNING, "Unable to find the closest solid type" + declaredType );
			}
			return "?";
		}
	}


}
