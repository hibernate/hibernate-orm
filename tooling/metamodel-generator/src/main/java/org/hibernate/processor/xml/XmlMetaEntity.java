/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.xml;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import org.hibernate.processor.Context;
import org.hibernate.processor.ImportContextImpl;
import org.hibernate.processor.MetaModelGenerationException;
import org.hibernate.processor.model.ImportContext;
import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;
import org.hibernate.processor.util.AccessTypeInformation;
import org.hibernate.processor.util.Constants;
import org.hibernate.processor.util.NullnessUtil;
import org.hibernate.processor.util.StringUtil;
import org.hibernate.processor.util.TypeUtils;
import org.hibernate.processor.xml.jaxb.Attributes;
import org.hibernate.processor.xml.jaxb.Basic;
import org.hibernate.processor.xml.jaxb.ElementCollection;
import org.hibernate.processor.xml.jaxb.Embeddable;
import org.hibernate.processor.xml.jaxb.EmbeddableAttributes;
import org.hibernate.processor.xml.jaxb.Embedded;
import org.hibernate.processor.xml.jaxb.EmbeddedId;
import org.hibernate.processor.xml.jaxb.Entity;
import org.hibernate.processor.xml.jaxb.Id;
import org.hibernate.processor.xml.jaxb.ManyToMany;
import org.hibernate.processor.xml.jaxb.ManyToOne;
import org.hibernate.processor.xml.jaxb.MapKeyClass;
import org.hibernate.processor.xml.jaxb.MappedSuperclass;
import org.hibernate.processor.xml.jaxb.OneToMany;
import org.hibernate.processor.xml.jaxb.OneToOne;

import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Collections.emptyList;
import static org.hibernate.processor.util.StringUtil.determineFullyQualifiedClassName;
import static org.hibernate.processor.util.TypeUtils.extractClosestRealTypeAsString;
import static org.hibernate.processor.util.TypeUtils.findMappedSuperClass;
import static org.hibernate.processor.util.TypeUtils.getElementKindForAccessType;
import static org.hibernate.processor.xml.jaxb.AccessType.*;

/**
 * Collects XML-based meta information about an annotated type (entity, embeddable or mapped superclass).
 *
 * @author Hardy Ferentschik
 */
public class XmlMetaEntity implements Metamodel {

	private final String clazzName;
	private final String packageName;
	private final String defaultPackageName;
	private final ImportContext importContext;
	private final List<MetaAttribute> members = new ArrayList<>();
	private final TypeElement element;
	private final Context context;
	private final boolean isMetaComplete;

	private @Nullable Attributes attributes;
	private @Nullable EmbeddableAttributes embeddableAttributes;
	private AccessTypeInformation accessTypeInfo;

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

	XmlMetaEntity(Entity ormEntity, String defaultPackageName, TypeElement element, Context context) {
		this( ormEntity.getClazz(), defaultPackageName, element, context, ormEntity.isMetadataComplete() );
		this.attributes = ormEntity.getAttributes();
		this.embeddableAttributes = null;
	}

	static XmlMetaEntity create(Entity ormEntity, String defaultPackageName, TypeElement element, Context context) {
		XmlMetaEntity entity = new XmlMetaEntity( ormEntity, defaultPackageName, element, context );
		// entities can be directly initialised
		entity.init();
		return entity;
	}

	XmlMetaEntity(MappedSuperclass mappedSuperclass, String defaultPackageName, TypeElement element, Context context) {
		this(
				mappedSuperclass.getClazz(),
				defaultPackageName,
				element,
				context,
				mappedSuperclass.isMetadataComplete()
		);
		this.attributes = mappedSuperclass.getAttributes();
		this.embeddableAttributes = null;
	}

	XmlMetaEntity(Embeddable embeddable, String defaultPackageName, TypeElement element, Context context) {
		this( embeddable.getClazz(), defaultPackageName, element, context, embeddable.isMetadataComplete() );
		this.attributes = null;
		this.embeddableAttributes = embeddable.getAttributes();
	}

	private XmlMetaEntity(String clazz, String defaultPackageName, TypeElement element, Context context, Boolean metaComplete) {
		this.defaultPackageName = defaultPackageName;
		String className = clazz;
		String pkg = defaultPackageName;
		if ( StringUtil.isFullyQualified( className ) ) {
			// if the class name is fully qualified we have to extract the package name from the fqcn.
			// default package name gets ignored
			pkg = StringUtil.packageNameFromFqcn( className );
			className = StringUtil.classNameFromFqcn( clazz );
		}
		this.clazzName = className;
		this.packageName = pkg;
		this.context = context;
		this.importContext = new ImportContextImpl( pkg );
		this.element = element;
		this.isMetaComplete = initIsMetaComplete( context, metaComplete );
	}

	private void init() {
		context.logMessage( Diagnostic.Kind.OTHER, "Initializing type " + getQualifiedName() + "." );

		this.accessTypeInfo = NullnessUtil.castNonNull( context.getAccessTypeInfo( getQualifiedName() ) );
		if ( attributes != null ) {
			parseAttributes( attributes );
		}
		else {
			parseEmbeddableAttributes( embeddableAttributes );
		}

		initialized = true;
	}

	public String getSimpleName() {
		return clazzName;
	}

	public String getQualifiedName() {
		return packageName + "." + getSimpleName();
	}

	public String getPackageName() {
		return packageName;
	}

	@Override
	public @Nullable String getSupertypeName() {
		return findMappedSuperClass( this, context );
	}

	public List<MetaAttribute> getMembers() {
		if ( !initialized ) {
			init();
		}

		return members;
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

	public TypeElement getElement() {
		return element;
	}

	@Override
	public boolean isMetaComplete() {
		return isMetaComplete;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "XmlMetaEntity" );
		sb.append( "{accessTypeInfo=" ).append( accessTypeInfo );
		sb.append( ", clazzName='" ).append( clazzName ).append( '\'' );
		sb.append( ", members=" ).append( members );
		sb.append( ", isMetaComplete=" ).append( isMetaComplete );
		sb.append( '}' );
		return sb.toString();
	}

	private static boolean initIsMetaComplete(Context context, Boolean metadataComplete) {
		return context.isFullyXmlConfigured() || Boolean.TRUE.equals( metadataComplete );
	}

	private @Nullable String @Nullable[] getCollectionTypes(String propertyName, String explicitTargetEntity, @Nullable String explicitMapKeyClass, ElementKind expectedElementKind) {
		for ( Element elem : element.getEnclosedElements() ) {
			if ( !expectedElementKind.equals( elem.getKind() ) ) {
				continue;
			}

			String elementPropertyName = elem.getSimpleName().toString();
			if ( elem.getKind().equals( ElementKind.METHOD ) ) {
				elementPropertyName = StringUtil.getPropertyName( elementPropertyName );
			}

			if ( !propertyName.equals( elementPropertyName ) ) {
				continue;
			}

			DeclaredType type = determineDeclaredType( elem );
			if ( type != null ) {
				return determineTypes( propertyName, explicitTargetEntity, explicitMapKeyClass, type );
			}
		}
		return null;
	}

	private @Nullable DeclaredType determineDeclaredType(Element elem) {
		DeclaredType type = null;
		if ( elem.asType() instanceof DeclaredType ) {
			type = ( (DeclaredType) elem.asType() );
		}
		else if ( elem.asType() instanceof ExecutableType ) {
			ExecutableType executableType = (ExecutableType) elem.asType();
			if ( executableType.getReturnType() instanceof DeclaredType ) {
				type = (DeclaredType) executableType.getReturnType();
			}
		}
		return type;
	}

	private @Nullable String[] determineTypes(String propertyName, String explicitTargetEntity, @Nullable String explicitMapKeyClass, DeclaredType type) {
		@Nullable String[] types = new String[3];
		determineTargetType( type, propertyName, explicitTargetEntity, types );
		if ( determineCollectionType( type, types ).equals( Constants.MAP_ATTRIBUTE ) ) {
			determineMapType( type, explicitMapKeyClass, types );
		}
		return types;
	}

	private void determineMapType(DeclaredType type, @Nullable String explicitMapKeyClass, @Nullable String[] types) {
		if ( explicitMapKeyClass != null ) {
			types[2] = explicitMapKeyClass;
		}
		else {
			types[2] = TypeUtils.getKeyType( type, context );
		}
	}

	private String determineCollectionType(DeclaredType type, @Nullable String[] types) {
		return NullnessUtil.castNonNull( types[1] = Constants.COLLECTIONS.get( type.asElement().toString() ) );
	}

	private void determineTargetType(DeclaredType type, String propertyName, String explicitTargetEntity, @Nullable String[] types) {
		List<? extends TypeMirror> typeArguments = type.getTypeArguments();

		if ( typeArguments.isEmpty() && explicitTargetEntity == null ) {
			throw new MetaModelGenerationException( "Unable to determine target entity type for " + clazzName + "." + propertyName + "." );
		}

		if ( explicitTargetEntity == null ) {
			types[0] = extractClosestRealTypeAsString( typeArguments.get( 0 ), context );
		}
		else {
			types[0] = explicitTargetEntity;
		}
	}

	/**
	 * Returns the entity type for a property.
	 *
	 * @param propertyName The property name
	 * @param explicitTargetEntity The explicitly specified target entity type or {@code null}.
	 * @param expectedElementKind Determines property vs field access type
	 *
	 * @return The entity type for this property  or {@code null} if the property with the name and the matching access
	 *         type does not exist.
	 */
	private @Nullable String getType(String propertyName, @Nullable String explicitTargetEntity, ElementKind expectedElementKind) {
		for ( Element elem : element.getEnclosedElements() ) {
			if ( !expectedElementKind.equals( elem.getKind() ) ) {
				continue;
			}

			String name = elem.getSimpleName().toString();
			final TypeMirror mirror;
			if ( ElementKind.METHOD.equals( elem.getKind() ) ) {
				name = StringUtil.getPropertyName( name );
				mirror = ( (ExecutableElement) elem ).getReturnType();
			}
			else {
				mirror = elem.asType();
			}

			if ( name == null || !name.equals( propertyName ) ) {
				continue;
			}

			if ( explicitTargetEntity != null ) {
				// TODO should there be a check of the target entity class and if it is loadable?
				return explicitTargetEntity;
			}

			switch ( mirror.getKind() ) {
				case INT: {
					return "java.lang.Integer";
				}
				case LONG: {
					return "java.lang.Long";
				}
				case BOOLEAN: {
					return "java.lang.Boolean";
				}
				case BYTE: {
					return "java.lang.Byte";
				}
				case SHORT: {
					return "java.lang.Short";
				}
				case CHAR: {
					return "java.lang.Char";
				}
				case FLOAT: {
					return "java.lang.Float";
				}
				case DOUBLE: {
					return "java.lang.Double";
				}
				case DECLARED: {
					return ((DeclaredType) mirror).asElement().asType().toString();
				}
				case TYPEVAR: {
					return mirror.toString();
				}
				default: {
				}
			}
		}

		context.logMessage(
				Diagnostic.Kind.WARNING,
				"Unable to determine type for property " + propertyName + " of class " + getQualifiedName()
						+ " using access type " + accessTypeInfo.getDefaultAccessType()
		);
		return null;
	}

	private void parseAttributes(Attributes attributes) {
		XmlMetaSingleAttribute attribute;
		for ( Id id : attributes.getId() ) {
			ElementKind elementKind = getElementKind( id.getAccess() );
			String type = getType( id.getName(), null, elementKind );
			if ( type != null ) {
				attribute = new XmlMetaSingleAttribute( this, id.getName(), type );
				members.add( attribute );
			}
		}

		if ( attributes.getEmbeddedId() != null ) {
			EmbeddedId embeddedId = attributes.getEmbeddedId();
			ElementKind elementKind = getElementKind( embeddedId.getAccess() );
			String type = getType( embeddedId.getName(), null, elementKind );
			if ( type != null ) {
				attribute = new XmlMetaSingleAttribute( this, embeddedId.getName(), type );
				members.add( attribute );
			}
		}

		for ( Basic basic : attributes.getBasic() ) {
			parseBasic( basic );
		}

		for ( ManyToOne manyToOne : attributes.getManyToOne() ) {
			parseManyToOne( manyToOne );
		}

		for ( OneToOne oneToOne : attributes.getOneToOne() ) {
			parseOneToOne( oneToOne );
		}

		for ( ManyToMany manyToMany : attributes.getManyToMany() ) {
			if ( parseManyToMany( manyToMany ) ) {
				break;
			}
		}

		for ( OneToMany oneToMany : attributes.getOneToMany() ) {
			if ( parseOneToMany( oneToMany ) ) {
				break;
			}
		}

		for ( ElementCollection collection : attributes.getElementCollection() ) {
			if ( parseElementCollection( collection ) ) {
				break;
			}
		}

		for ( Embedded embedded : attributes.getEmbedded() ) {
			parseEmbedded( embedded );
		}
	}

	private void parseEmbeddableAttributes(@Nullable EmbeddableAttributes attributes) {
		if ( attributes == null ) {
			return;
		}
		for ( Basic basic : attributes.getBasic() ) {
			parseBasic( basic );
		}

		for ( ManyToOne manyToOne : attributes.getManyToOne() ) {
			parseManyToOne( manyToOne );
		}

		for ( OneToOne oneToOne : attributes.getOneToOne() ) {
			parseOneToOne( oneToOne );
		}

		for ( ManyToMany manyToMany : attributes.getManyToMany() ) {
			if ( parseManyToMany( manyToMany ) ) {
				break;
			}
		}

		for ( OneToMany oneToMany : attributes.getOneToMany() ) {
			if ( parseOneToMany( oneToMany ) ) {
				break;
			}
		}

		for ( ElementCollection collection : attributes.getElementCollection() ) {
			if ( parseElementCollection( collection ) ) {
				break;
			}
		}
	}

	private boolean parseElementCollection(ElementCollection collection) {
		@Nullable String @Nullable[] types;
		XmlMetaCollection metaCollection;
		ElementKind elementKind = getElementKind( collection.getAccess() );
		String explicitTargetClass = determineExplicitTargetEntity( collection.getTargetClass() );
		String explicitMapKey = determineExplicitMapKeyClass( collection.getMapKeyClass() );
		try {
			types = getCollectionTypes(
					collection.getName(), explicitTargetClass, explicitMapKey, elementKind
			);
		}
		catch ( MetaModelGenerationException e ) {
			logMetaModelException( collection.getName(), e );
			return true;
		}
		if ( types != null ) {
			final String type = NullnessUtil.castNonNull( types[0] );
			final String collectionType = NullnessUtil.castNonNull( types[1] );
			final String keyType = types[2];
			if ( keyType == null ) {
				metaCollection = new XmlMetaCollection( this, collection.getName(), type, collectionType );
			}
			else {
				metaCollection = new XmlMetaMap( this, collection.getName(), type, collectionType, keyType );
			}
			members.add( metaCollection );
		}
		return false;
	}

	private void parseEmbedded(Embedded embedded) {
		XmlMetaSingleAttribute attribute;
		ElementKind elementKind = getElementKind( embedded.getAccess() );
		String type = getType( embedded.getName(), null, elementKind );
		if ( type != null ) {
			attribute = new XmlMetaSingleAttribute( this, embedded.getName(), type );
			members.add( attribute );
		}
	}

	private String determineExplicitTargetEntity(String targetClass) {
		String explicitTargetClass = targetClass;
		if ( explicitTargetClass != null ) {
			explicitTargetClass = determineFullyQualifiedClassName( defaultPackageName, targetClass );
		}
		return explicitTargetClass;
	}

	private @Nullable String determineExplicitMapKeyClass(MapKeyClass mapKeyClass) {
		String explicitMapKey = null;
		if ( mapKeyClass != null ) {
			explicitMapKey = determineFullyQualifiedClassName( defaultPackageName, mapKeyClass.getClazz() );
		}
		return explicitMapKey;
	}

	private boolean parseOneToMany(OneToMany oneToMany) {
		@Nullable String @Nullable [] types;
		XmlMetaCollection metaCollection;
		ElementKind elementKind = getElementKind( oneToMany.getAccess() );
		String explicitTargetClass = determineExplicitTargetEntity( oneToMany.getTargetEntity() );
		String explicitMapKey = determineExplicitMapKeyClass( oneToMany.getMapKeyClass() );
		try {
			types = getCollectionTypes( oneToMany.getName(), explicitTargetClass, explicitMapKey, elementKind );
		}
		catch ( MetaModelGenerationException e ) {
			logMetaModelException( oneToMany.getName(), e );
			return true;
		}
		if ( types != null ) {
			final String type = NullnessUtil.castNonNull( types[0] );
			final String collectionType = NullnessUtil.castNonNull( types[1] );
			final String keyType = types[2];
			if ( keyType == null ) {
				metaCollection = new XmlMetaCollection( this, oneToMany.getName(), type, collectionType );
			}
			else {
				metaCollection = new XmlMetaMap( this, oneToMany.getName(), type, collectionType, keyType );
			}
			members.add( metaCollection );
		}
		return false;
	}

	private boolean parseManyToMany(ManyToMany manyToMany) {
		@Nullable String @Nullable [] types;
		XmlMetaCollection metaCollection;
		ElementKind elementKind = getElementKind( manyToMany.getAccess() );
		String explicitTargetClass = determineExplicitTargetEntity( manyToMany.getTargetEntity() );
		String explicitMapKey = determineExplicitMapKeyClass( manyToMany.getMapKeyClass() );
		try {
			types = getCollectionTypes(
					manyToMany.getName(), explicitTargetClass, explicitMapKey, elementKind
			);
		}
		catch ( MetaModelGenerationException e ) {
			logMetaModelException( manyToMany.getName(), e );
			return true;
		}
		if ( types != null ) {
			final String type = NullnessUtil.castNonNull( types[0] );
			final String collectionType = NullnessUtil.castNonNull( types[1] );
			final String keyType = types[2];
			if ( keyType == null ) {
				metaCollection = new XmlMetaCollection( this, manyToMany.getName(), type, collectionType );
			}
			else {
				metaCollection = new XmlMetaMap( this, manyToMany.getName(), type, collectionType, keyType );
			}
			members.add( metaCollection );
		}
		return false;
	}

	private void parseOneToOne(OneToOne oneToOne) {
		XmlMetaSingleAttribute attribute;
		ElementKind elementKind = getElementKind( oneToOne.getAccess() );
		String type = getType( oneToOne.getName(), oneToOne.getTargetEntity(), elementKind );
		if ( type != null ) {
			attribute = new XmlMetaSingleAttribute( this, oneToOne.getName(), type );
			members.add( attribute );
		}
	}

	private void parseManyToOne(ManyToOne manyToOne) {
		XmlMetaSingleAttribute attribute;
		ElementKind elementKind = getElementKind( manyToOne.getAccess() );
		String type = getType( manyToOne.getName(), manyToOne.getTargetEntity(), elementKind );
		if ( type != null ) {
			attribute = new XmlMetaSingleAttribute( this, manyToOne.getName(), type );
			members.add( attribute );
		}
	}

	private void parseBasic(Basic basic) {
		XmlMetaSingleAttribute attribute;
		ElementKind elementKind = getElementKind( basic.getAccess() );
		String type = getType( basic.getName(), null, elementKind );
		if ( type != null ) {
			attribute = new XmlMetaSingleAttribute( this, basic.getName(), type );
			members.add( attribute );
		}
	}

	private void logMetaModelException(String name, MetaModelGenerationException e) {
		StringBuilder builder = new StringBuilder();
		builder.append( "Error processing xml for " );
		builder.append( clazzName );
		builder.append( "." );
		builder.append( name );
		builder.append( ". Error message: " );
		builder.append( e.getMessage() );
		context.logMessage(
				Diagnostic.Kind.WARNING,
				builder.toString()
		);
	}

	private ElementKind getElementKind(org.hibernate.processor.xml.jaxb.AccessType accessType) {
		// if no explicit access type was specified in xml we use the entity access type
		if ( accessType == null ) {
			return getElementKindForAccessType( accessTypeInfo.getAccessType() );
		}
		return FIELD.equals( accessType ) ? ElementKind.FIELD : ElementKind.METHOD;
	}

	@Override
	public Context getContext() {
		return context;
	}

	@Override
	public boolean isImplementation() {
		return false;
	}

	@Override
	public boolean isInjectable() {
		return false;
	}

	@Override
	public String scope() {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public boolean isJakartaDataStyle() {
		return false;
	}

	@Override
	public List<AnnotationMirror> inheritedAnnotations() {
		return emptyList();
	}
}
