/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

import org.hibernate.jpamodelgen.Context;
import org.hibernate.jpamodelgen.ImportContextImpl;
import org.hibernate.jpamodelgen.model.ImportContext;
import org.hibernate.jpamodelgen.model.MetaAttribute;
import org.hibernate.jpamodelgen.model.MetaEntity;
import org.hibernate.jpamodelgen.util.AccessType;
import org.hibernate.jpamodelgen.util.AccessTypeInformation;
import org.hibernate.jpamodelgen.util.Constants;
import org.hibernate.jpamodelgen.util.NullnessUtil;
import org.hibernate.jpamodelgen.util.TypeUtils;

/**
 * Class used to collect meta information about an annotated type (entity, embeddable or mapped superclass).
 *
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public class AnnotationMetaEntity implements MetaEntity {

	private final ImportContext importContext;
	private final TypeElement element;
	private final Map<String, MetaAttribute> members;
	private final Context context;

	private AccessTypeInformation entityAccessTypeInfo;

	/**
	 * Whether the members of this type have already been initialized or not.
	 * <p>
	 * Embeddables and mapped super-classes need to be lazily initialized since the access type may be determined by
	 * the class which is embedding or sub-classing the entity or super-class. This might not be known until
	 * annotations are processed.
	 * <p>
	 * Also note, that if two different classes with different access types embed this entity or extend this mapped
	 * super-class, the access type of the embeddable/super-class will be the one of the last embedding/sub-classing
	 * entity processed. The result is not determined (that's ok according to the spec).
	 */
	private boolean initialized;

	/**
	 * Another meta entity for the same type which should be merged lazily with this meta entity. Doing the merge
	 * lazily is required for embeddedables and mapped supertypes to only pull in those members matching the access
	 * type as configured via the embedding entity or subclass (also see METAGEN-85).
	 */
	private MetaEntity entityToMerge;

	public AnnotationMetaEntity(TypeElement element, Context context) {
		this.element = element;
		this.context = context;
		this.members = new HashMap<>();
		this.importContext = new ImportContextImpl( getPackageName( context, element ) );
	}

	public static AnnotationMetaEntity create(TypeElement element, Context context, boolean lazilyInitialised) {
		final AnnotationMetaEntity annotationMetaEntity = new AnnotationMetaEntity( element, context );
		if ( !lazilyInitialised ) {
			annotationMetaEntity.init();
		}
		return annotationMetaEntity;
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
		return getPackageName( context, element );
	}

	private static String getPackageName(Context context, TypeElement element) {
		PackageElement packageOf = context.getElementUtils().getPackageOf( element );
		return context.getElementUtils().getName( packageOf.getQualifiedName() ).toString();
	}

	public List<MetaAttribute> getMembers() {
		if ( !initialized ) {
			init();
			if ( entityToMerge != null ) {
				mergeInMembers( entityToMerge.getMembers() );
			}
		}

		return new ArrayList<>( members.values() );
	}

	@Override
	public boolean isMetaComplete() {
		return false;
	}

	private void mergeInMembers(Collection<MetaAttribute> attributes) {
		for ( MetaAttribute attribute : attributes ) {
			// propagate types to be imported
			importType( attribute.getMetaType() );
			importType( attribute.getTypeDeclaration() );

			members.put( attribute.getPropertyName(), attribute );
		}
	}

	public void mergeInMembers(MetaEntity other) {
		// store the entity in order do the merge lazily in case of a non-initialized embeddedable or mapped superclass
		if ( !initialized ) {
			this.entityToMerge = other;
		}
		else {
			mergeInMembers( other.getMembers() );
		}
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

	public final TypeElement getTypeElement() {
		return element;
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

	protected final void init() {
		getContext().logMessage( Diagnostic.Kind.OTHER, "Initializing type " + getQualifiedName() + "." );

		TypeUtils.determineAccessTypeForHierarchy( element, context );
		AccessTypeInformation accessTypeInfo = context.getAccessTypeInfo( getQualifiedName() );
		entityAccessTypeInfo = NullnessUtil.castNonNull( accessTypeInfo );

		List<? extends Element> fieldsOfClass = ElementFilter.fieldsIn( element.getEnclosedElements() );
		addPersistentMembers( fieldsOfClass, AccessType.FIELD );

		List<? extends Element> methodsOfClass = ElementFilter.methodsIn( element.getEnclosedElements() );
		List<Element> gettersAndSettersOfClass = new ArrayList<>();

		for ( Element rawMethodOfClass: methodsOfClass ) {
			if ( isGetterOrSetter( rawMethodOfClass ) ) {
				gettersAndSettersOfClass.add( rawMethodOfClass );
			}
		}
		addPersistentMembers( gettersAndSettersOfClass, AccessType.PROPERTY );

		addAuxiliaryMembers();

		initialized = true;
	}

	private void addAuxiliaryMembers() {
		addAuxiliaryMembersForAnnotation( Constants.NAMED_QUERY, "QUERY_" );
		addAuxiliaryMembersForRepeatableAnnotation( Constants.NAMED_QUERIES, "QUERY_" );
		addAuxiliaryMembersForAnnotation( Constants.NAMED_NATIVE_QUERY, "QUERY_" );
		addAuxiliaryMembersForRepeatableAnnotation( Constants.NAMED_NATIVE_QUERIES, "QUERY_" );
		addAuxiliaryMembersForAnnotation( Constants.SQL_RESULT_SET_MAPPING, "MAPPING_" );
		addAuxiliaryMembersForRepeatableAnnotation( Constants.SQL_RESULT_SET_MAPPINGS, "MAPPING_" );
		addAuxiliaryMembersForAnnotation( Constants.NAMED_ENTITY_GRAPH, "GRAPH_" );
		addAuxiliaryMembersForRepeatableAnnotation( Constants.NAMED_ENTITY_GRAPHS, "GRAPH_" );

		addAuxiliaryMembersForAnnotation( Constants.HIB_NAMED_QUERY, "QUERY_" );
		addAuxiliaryMembersForRepeatableAnnotation( Constants.HIB_NAMED_QUERIES, "QUERY_" );
		addAuxiliaryMembersForAnnotation( Constants.HIB_NAMED_NATIVE_QUERY, "QUERY_" );
		addAuxiliaryMembersForRepeatableAnnotation( Constants.HIB_NAMED_NATIVE_QUERIES, "QUERY_" );
		addAuxiliaryMembersForAnnotation( Constants.HIB_FETCH_PROFILE, "PROFILE_" );
		addAuxiliaryMembersForRepeatableAnnotation( Constants.HIB_FETCH_PROFILES, "PROFILE_" );
	}

	private void addAuxiliaryMembersForRepeatableAnnotation(String annotationName, String prefix) {
		AnnotationMirror mirror = TypeUtils.getAnnotationMirror( element, annotationName );
		if ( mirror != null ) {
			mirror.getElementValues().forEach((key, value) -> {
				if ( key.getSimpleName().contentEquals("value") ) {
					List<? extends AnnotationMirror> values =
							(List<? extends AnnotationMirror>) value.getValue();
					for ( AnnotationMirror annotationMirror : values ) {
						addAuxiliaryMembersForMirror( annotationMirror, prefix );
					}
				}
			});
		}
	}

	private void addAuxiliaryMembersForAnnotation(String annotationName, String prefix) {
		AnnotationMirror mirror = TypeUtils.getAnnotationMirror( element, annotationName);
		if ( mirror != null ) {
			addAuxiliaryMembersForMirror( mirror, prefix );
		}
	}

	private void addAuxiliaryMembersForMirror(AnnotationMirror mirror, String prefix) {
		mirror.getElementValues().forEach((key, value) -> {
			if ( key.getSimpleName().contentEquals("name") ) {
				String name = value.getValue().toString();
				members.put( prefix + name, new NameMetaAttribute( this, name, prefix ) );
			}
		});
	}

	/**
	 * Check if method respects Java Bean conventions for getter and setters.
	 *
	 * @param methodOfClass method element
	 *
	 * @return whether method respects Java Bean conventions.
	 */
	private boolean isGetterOrSetter(Element methodOfClass) {
		ExecutableType methodType = (ExecutableType) methodOfClass.asType();
		String methodSimpleName = methodOfClass.getSimpleName().toString();
		List<? extends TypeMirror> methodParameterTypes = methodType.getParameterTypes();
		TypeMirror returnType = methodType.getReturnType();

		if(
			methodSimpleName.startsWith("set") &&
			methodParameterTypes.size() == 1 &&
			"void".equalsIgnoreCase( returnType.toString() ) ) {
			return true;
		}
		else if(
			( methodSimpleName.startsWith("get") || methodSimpleName.startsWith("is") ) &&
			methodParameterTypes.isEmpty() &&
			!"void".equalsIgnoreCase( returnType.toString() ) ) {
			return true;
		}
		else {
			return false;
		}
	}

	private void addPersistentMembers(List<? extends Element> membersOfClass, AccessType membersKind) {
		for ( Element memberOfClass : membersOfClass ) {
			AccessType forcedAccessType = TypeUtils.determineAnnotationSpecifiedAccessType( memberOfClass );
			if ( entityAccessTypeInfo.getAccessType() != membersKind && forcedAccessType == null ) {
				continue;
			}

			if ( TypeUtils.containsAnnotation( memberOfClass, Constants.TRANSIENT )
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

}
