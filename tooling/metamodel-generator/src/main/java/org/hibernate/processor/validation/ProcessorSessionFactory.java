/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.validation;

import jakarta.persistence.AccessType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.type.BasicType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.MappingContext;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.StandardLocation;
import java.beans.Introspector;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.stream;
import static org.hibernate.internal.util.StringHelper.qualify;
import static org.hibernate.internal.util.StringHelper.root;
import static org.hibernate.internal.util.StringHelper.split;
import static org.hibernate.internal.util.StringHelper.unroot;
import static org.hibernate.processor.util.Constants.JAVA_OBJECT;

/**
 * Implementation of the {@code Mock} objects based on standard
 * annotation processor APIs. Note that alternative implementations
 * exist in the Query Validator project.
 *
 * @author Gavin King
 */
@SuppressWarnings("nullness")
public abstract class ProcessorSessionFactory extends MockSessionFactory {

	public static MockSessionFactory create(
			ProcessingEnvironment environment,
			Map<String,String> entityNameMappings,
			Map<String, Set<String>> enumTypesByValue,
			boolean indexing) {
		return instance.make(environment, indexing, entityNameMappings, enumTypesByValue);
	}

	static final Mocker<ProcessorSessionFactory> instance = Mocker.variadic(ProcessorSessionFactory.class);
	private static final Mocker<Component> component = Mocker.variadic(Component.class);
	private static final Mocker<ToManyAssociationPersister> toManyPersister = Mocker.variadic(ToManyAssociationPersister.class);
	private static final Mocker<ElementCollectionPersister> collectionPersister = Mocker.variadic(ElementCollectionPersister.class);
	private static final Mocker<EntityPersister> entityPersister = Mocker.variadic(EntityPersister.class);

	private static final CharSequence jakartaPersistence = new StringBuilder("jakarta").append('.').append("persistence");
	private static final CharSequence javaxPersistence = new StringBuilder("javax").append('.').append("persistence");

	private final Elements elementUtil;
	private final Types typeUtil;
	private final Filer filer;
	private final boolean indexing;
	private final Map<String, String> entityNameMappings;
	private final Map<String, Set<String>> enumTypesByValue;

	public ProcessorSessionFactory(
			ProcessingEnvironment processingEnvironment,
			boolean indexing,
			Map<String,String> entityNameMappings,
			Map<String, Set<String>> enumTypesByValue) {
		elementUtil = processingEnvironment.getElementUtils();
		typeUtil = processingEnvironment.getTypeUtils();
		filer = processingEnvironment.getFiler();
		this.indexing = indexing;
		this.entityNameMappings = entityNameMappings;
		this.enumTypesByValue = enumTypesByValue;
	}

	@Override
	MockEntityPersister createMockEntityPersister(String entityName) {
		final TypeElement type = findEntityClass(entityName);
		return type == null ? null : entityPersister.make(entityName, type, this);
	}

	@Override
	MockCollectionPersister createMockCollectionPersister(String role) {
		final String entityName = root(role); //only works because entity names don't contain dots
		final String propertyPath = unroot(role);
		final TypeElement entityClass = findEntityClass(entityName);
		final AccessType defaultAccessType = getDefaultAccessType(entityClass);
		final Element property = findPropertyByPath(entityClass, propertyPath, defaultAccessType);
		final CollectionType collectionType = collectionType(memberType(property), role);
		if (isToManyAssociation(property)) {
			return toManyPersister.make(role, collectionType,
					getToManyTargetEntityName(property), this);
		}
		else if (isElementCollectionProperty(property)) {
			final Element elementType = asElement(getElementCollectionElementType(property));
			return collectionPersister.make(role, collectionType,
					elementType, propertyPath, defaultAccessType, this);
		}
		else {
			return null;
		}
	}

	@Override
	Type propertyType(String typeName, String propertyPath) {
		final TypeElement type = findClassByQualifiedName(typeName);
		final AccessType accessType = getAccessType(type, AccessType.FIELD);
		final Element propertyByPath = findPropertyByPath(type, propertyPath, accessType);
		return propertyByPath == null ? null
				: propertyType(propertyByPath, typeName, propertyPath, accessType);
	}

	private static Element findPropertyByPath(TypeElement type,
			String propertyPath,
			AccessType defaultAccessType) {
		return stream(split(".", propertyPath))
				.reduce((Element) type,
						(symbol, segment) -> dereference( defaultAccessType, symbol, segment ),
						(last, current) -> current);
	}

	private static Element dereference(AccessType defaultAccessType, Element symbol, String segment) {
		if (symbol == null) {
			return null;
		}
		else {
			return asElement(symbol.asType()) instanceof TypeElement element
					? findProperty(element, segment, defaultAccessType)
					: null;
		}
	}

	private Type propertyType(Element member, String entityName, String path, AccessType defaultAccessType) {
		final TypeMirror memberType = memberType(member);
		if (isEmbeddedProperty(member)) {
			return componentType( entityName, path, defaultAccessType, memberType );
		}
		else if (isToOneAssociation(member)) {
			return new ManyToOneType(getTypeConfiguration(), getToOneTargetEntity(member));
		}
		else if (isToManyAssociation(member)) {
			return collectionType(memberType, qualify(entityName, path));
		}
		else if (isElementCollectionProperty(member)) {
			return collectionType(memberType, qualify(entityName, path));
		}
		else if (isEnumProperty(member)) {
			return enumType(member, memberType);
		}
		else {
			return getTypeConfiguration().getBasicTypeRegistry()
					.getRegisteredType(qualifiedName(memberType));
		}
	}

	private Component componentType(String entityName, String path, AccessType defaultAccessType, TypeMirror memberType) {
		return component.make( asElement( memberType ), entityName, path, defaultAccessType, this );
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static BasicType<?> enumType(Element member, TypeMirror memberType) {
		final Class<Enum> enumClass = Enum.class; // because we can't load the real enum class!
		return enumType( member, qualifiedName( memberType ), enumClass );
	}

	private static <T extends Enum<T>> BasicType<T> enumType(Element member, String typeName, Class<T> enumClass) {
		final EnumJavaType<T> javaType = new EnumJavaType<>( enumClass ) {
			@Override
			public String getTypeName() {
				return typeName;
			}
		};
		return new BasicTypeImpl<>( javaType, enumJdbcType(member) ) {
			@Override
			public String getTypeName() {
				return typeName;
			}
		};
	}

	private static JdbcType enumJdbcType(Element member) {
		final VariableElement mapping = (VariableElement)
				getAnnotationMember(getAnnotation(member,"Enumerated"), "value");
		return mapping != null && mapping.getSimpleName().contentEquals("STRING")
				? VarcharJdbcType.INSTANCE
				: IntegerJdbcType.INSTANCE;
	}

	// dupe of HibernateProcessor.ENTITY_INDEX for reasons of modularity
	public static final String ENTITY_INDEX = "entity.index";

	@Override @Nullable
	Set<String> getEnumTypesForValue(String value) {
		final Set<String> result = enumTypesByValue.get(value);
		if ( result != null ) {
			return result;
		}
		if ( indexing ) {
			final Set<String> indexed = getIndexedEnumTypesByValue(value);
			if ( indexed != null ) {
				enumTypesByValue.put(value, indexed);
				return indexed;
			}
		}
		//TODO: else do a full scan like in findEntityByUnqualifiedName()
		return null;
	}

	private @Nullable Set<String> getIndexedEnumTypesByValue(String value) {
		try (Reader reader = filer.getResource( StandardLocation.SOURCE_OUTPUT, ENTITY_INDEX, value )
				.openReader( true ); BufferedReader buffered = new BufferedReader( reader )) {
			return Set.of( split( " ", buffered.readLine() ) );
		}
		catch (IOException ignore) {
		}
		try (Reader reader = filer.getResource( StandardLocation.CLASS_PATH, ENTITY_INDEX, '.' + value )
				.openReader( true ); BufferedReader buffered = new BufferedReader( reader )) {
			return Set.of( split( " ", buffered.readLine() ) );
		}
		catch (IOException ignore) {
		}
		return null;
	}

	private static Type elementCollectionElementType(TypeElement elementType,
			String role, String path,
			AccessType defaultAccessType,
			MockSessionFactory factory) {
		if (isEmbeddableType(elementType)) {
			return component.make(elementType, role, path, defaultAccessType, factory);
		}
		else {
			return factory.getTypeConfiguration().getBasicTypeRegistry()
					.getRegisteredType(qualifiedName(elementType.asType()));
		}
	}

	private static CollectionType collectionType(TypeMirror type, String role) {
		return createCollectionType(role, simpleName(type));
	}

	public static abstract class Component implements CompositeType {
		private final String[] propertyNames;
		private final Type[] propertyTypes;

		TypeElement type;

		public Component(TypeElement type,
				String entityName, String path,
				AccessType defaultAccessType,
				ProcessorSessionFactory factory) {
			this.type = type;

			final List<String> names = new ArrayList<>();
			final List<Type> types = new ArrayList<>();

			while (type!=null) {
				if (isMappedClass(type)) { //ignore unmapped intervening classes
					final AccessType accessType = getAccessType(type, defaultAccessType);
					for (Element member: type.getEnclosedElements()) {
						if (isPersistable(member, accessType)) {
							final String name = propertyName(member);
							final Type propertyType =
									factory.propertyType(member, entityName,
											qualify(path, name), defaultAccessType);
							if (propertyType != null) {
								names.add(name);
								types.add(propertyType);
							}
						}
					}
				}
				type = (TypeElement) asElement(type.getSuperclass());
			}

			propertyNames = names.toArray(new String[0]);
			propertyTypes = types.toArray(new Type[0]);
		}

		@Override
		public int getPropertyIndex(String name) {
			final String[] names = getPropertyNames();
			for ( int i = 0, max = names.length; i < max; i++ ) {
				if ( names[i].equals( name ) ) {
					return i;
				}
			}
			throw new PropertyNotFoundException(
					"Could not resolve attribute '" + name + "' of '" + getName() + "'"
			);
		}

		@Override
		public String getName() {
			return type.getSimpleName().toString();
		}

		@Override
		public String getReturnedClassName() {
			return type.getQualifiedName().toString();
		}

		@Override
		public boolean isComponentType() {
			return true;
		}

		@Override
		public String[] getPropertyNames() {
			return propertyNames;
		}

		@Override
		public Type[] getSubtypes() {
			return propertyTypes;
		}

		@Override
		public boolean[] getPropertyNullability() {
			return new boolean[propertyNames.length];
		}

		@Override
		public int getColumnSpan(MappingContext mapping) {
			return propertyNames.length;
		}
	}

	public static abstract class EntityPersister extends MockEntityPersister {
		private final TypeElement type;
		private final Types typeUtil;
		private final ProcessorSessionFactory factory;

		public EntityPersister(String entityName, TypeElement type, ProcessorSessionFactory factory) {
			super(entityName, getDefaultAccessType(type), factory);
			this.type = type;
			this.typeUtil = factory.typeUtil;
			this.factory = factory;
			initSubclassPersisters();
		}

		@Override
		public String getRootEntityName() {
			TypeElement result = type;
			TypeMirror superclass = type.getSuperclass();
			while ( superclass!=null && superclass.getKind() == TypeKind.DECLARED ) {
				final DeclaredType declaredType = (DeclaredType) superclass;
				final TypeElement typeElement = (TypeElement) declaredType.asElement();
				if ( hasAnnotation(typeElement, "Entity") ) {
					result = typeElement;
				}
				superclass = typeElement.getSuperclass();
			}
			return getHibernateEntityName(result);
		}

		@Override
		boolean isSamePersister(MockEntityPersister entityPersister) {
			final EntityPersister persister = (EntityPersister) entityPersister;
			return typeUtil.isSameType( persister.type.asType(), type.asType() );
		}

		@Override
		boolean isSubclassPersister(MockEntityPersister entityPersister) {
			final EntityPersister persister = (EntityPersister) entityPersister;
			return typeUtil.isSubtype( persister.type.asType(), type.asType() );
		}

		@Override
		Type createPropertyType(String propertyPath) {
			final Element symbol = findPropertyByPath(type, propertyPath, defaultAccessType);
			return symbol == null ? null :
					factory.propertyType(symbol, getEntityName(), propertyPath, defaultAccessType);
		}

		@Override
		public String identifierPropertyName() {
			for (Element element : type.getEnclosedElements()) {
				if ( hasAnnotation(element, "Id") || hasAnnotation(element, "EmbeddedId") ) {
					return element.getSimpleName().toString();
				}
			}
			return "id";
		}

		@Override
		public Type identifierType() {
			if (hasAnnotation( type, "IdClass" )) {
				final TypeMirror annotationMember = (TypeMirror)getAnnotationMember( getAnnotation( type, "IdClass" ), "value" );
				if (annotationMember != null) {
					return factory.componentType( getEntityName(), EntityIdentifierMapping.ID_ROLE_NAME, defaultAccessType, annotationMember );
				}
			}
			for (Element element : type.getEnclosedElements()) {
				if ( hasAnnotation(element, "Id")|| hasAnnotation(element, "EmbeddedId") ) {
					return factory.propertyType(element, getEntityName(), EntityIdentifierMapping.ID_ROLE_NAME, defaultAccessType);
				}
			}
			return null;
		}

		@Override
		public BasicType<?> versionType() {
			for (Element element : type.getEnclosedElements()) {
				if ( hasAnnotation(element, "Version") ) {
					return (BasicType<?>) factory.propertyType(element, getEntityName(),
							EntityVersionMapping.VERSION_ROLE_NAME, defaultAccessType);
				}
			}
			return null;
		}
	}

	public abstract static class ToManyAssociationPersister extends MockCollectionPersister {
		public ToManyAssociationPersister(String role, CollectionType collectionType, String targetEntityName, ProcessorSessionFactory that) {
			super(role, collectionType,
					new ManyToOneType(that.getTypeConfiguration(), targetEntityName),
					that);
		}

		@Override
		Type getElementPropertyType(String propertyPath) {
			return getElementPersister().getPropertyType(propertyPath);
		}
	}

	public abstract static class ElementCollectionPersister extends MockCollectionPersister {
		private final TypeElement elementType;
		private final AccessType defaultAccessType;
		private final ProcessorSessionFactory factory;

		public ElementCollectionPersister(String role,
				CollectionType collectionType,
				TypeElement elementType,
				String propertyPath,
				AccessType defaultAccessType,
				ProcessorSessionFactory factory) {
			super(role, collectionType,
					elementCollectionElementType(elementType, role,
							propertyPath, defaultAccessType,
							factory),
					factory);
			this.elementType = elementType;
			this.defaultAccessType = defaultAccessType;
			this.factory = factory;
		}

		@Override
		Type getElementPropertyType(String propertyPath) {
			final Element symbol = findPropertyByPath(elementType, propertyPath, defaultAccessType);
			return symbol == null ? null :
					factory.propertyType(symbol, getOwnerEntityName(), propertyPath, defaultAccessType);
		}
	}

	@Override
	boolean isEntityDefined(String jpaEntityName) {
		return findEntityByUnqualifiedName(jpaEntityName) != null;
	}

	@Override
	String qualifyName(String jpaEntityName) {
		final TypeElement entityClass = findEntityByUnqualifiedName(jpaEntityName);
		return entityClass == null ? null : entityClass.getQualifiedName().toString();
	}

	@Override
	boolean isAttributeDefined(String entityName, String fieldName) {
		final TypeElement entityClass = findEntityClass(entityName);
		return entityClass != null
			&& (findPropertyByPath(entityClass, fieldName, getDefaultAccessType(entityClass)) != null
				|| "id".equals( fieldName ) && hasAnnotation( entityClass, "IdClass" ));
	}

	public TypeElement findEntityClass(String entityName) {
		return entityName == null ? null : findEntityByQualifiedName( entityName );
	}

	private TypeElement findEntityByQualifiedName(String entityName) {
		final TypeElement type = findClassByQualifiedName(entityName);
		return type != null && isEntity(type) ? type : null;
	}

	//Needed only for ECJ
	private final Map<String,TypeElement> entityCache = new HashMap<>();

	private TypeElement findEntityByUnqualifiedName(String entityName) {
		final TypeElement cached = entityCache.get(entityName);
		if ( cached != null ) {
			return cached;
		}

		if ( indexing ) {
			final TypeElement indexedEntity = findIndexedEntityByUnqualifiedName( entityName );
			if ( indexedEntity != null ) {
				entityCache.put(entityName, indexedEntity);
				return indexedEntity;
			}
		}

		TypeElement symbol =
				findEntityByUnqualifiedName(entityName,
						elementUtil.getModuleElement(""));
		if (symbol!=null) {
			entityCache.put(entityName, symbol);
			return symbol;
		}
		for (ModuleElement module: elementUtil.getAllModuleElements()) {
			symbol = findEntityByUnqualifiedName(entityName, module);
			if (symbol!=null) {
				entityCache.put(entityName, symbol);
				return symbol;
			}
		}
		return null;
	}

	private @Nullable TypeElement findIndexedEntityByUnqualifiedName(String entityName) {
		final String qualifiedName = entityNameMappings.get(entityName);
		if ( qualifiedName != null ) {
			return elementUtil.getTypeElement(qualifiedName);
		}
		try (Reader reader = filer.getResource( StandardLocation.SOURCE_OUTPUT, ENTITY_INDEX, entityName)
				.openReader(true); BufferedReader buffered = new BufferedReader(reader) ) {
			return elementUtil.getTypeElement(buffered.readLine());
		}
		catch (IOException ignore) {
		}
		try (Reader reader = filer.getResource(StandardLocation.CLASS_PATH, ENTITY_INDEX, entityName)
				.openReader(true); BufferedReader buffered = new BufferedReader(reader) ) {
			return elementUtil.getTypeElement(buffered.readLine());
		}
		catch (IOException ignore) {
		}
		return null;
	}

	private static @Nullable TypeElement findEntityByUnqualifiedName(String entityName, ModuleElement module) {
		for (Element element: module.getEnclosedElements()) {
			if (element.getKind() == ElementKind.PACKAGE) {
				final PackageElement pack = (PackageElement) element;
				try {
					for (Element member : pack.getEnclosedElements()) {
						if (isMatchingEntity(member, entityName)) {
							return (TypeElement) member;
						}
					}
				}
				catch (Exception ignore) {
				}
			}
		}
		return null;
	}

	private static boolean isMatchingEntity(Element symbol, String entityName) {
		if (symbol.getKind() == ElementKind.CLASS) {
			final TypeElement type = (TypeElement) symbol;
			return isEntity(type)
				&& ( getJpaEntityName(type).equals(entityName)
					|| type.getQualifiedName().contentEquals(entityName) );
		}
		else {
			return false;
		}
	}

	private static Element findProperty(TypeElement type, String propertyName, AccessType defaultAccessType) {
		//iterate up the superclass hierarchy
		while (type!=null) {
			if (isMappedClass(type)) { //ignore unmapped intervening classes
				final AccessType accessType = getAccessType(type, defaultAccessType);
				for (Element member: type.getEnclosedElements()) {
					if (isMatchingProperty(member, propertyName, accessType)) {
						return member;
					}
				}
			}
			type = (TypeElement) asElement(type.getSuperclass());
		}
		return null;
	}

	private static boolean isMatchingProperty(Element symbol, String propertyName, AccessType accessType) {
		return isPersistable(symbol, accessType)
			&& propertyName.equals(propertyName(symbol));
	}

	private static boolean isGetterMethod(ExecutableElement method) {
		if (!method.getParameters().isEmpty()) {
			return false;
		}
		else {
			Name methodName = method.getSimpleName();
			TypeMirror returnType = method.getReturnType();
			return methodName.subSequence(0,3).toString().equals("get") && returnType.getKind() != TypeKind.VOID
				|| methodName.subSequence(0,2).toString().equals("is") && returnType.getKind() == TypeKind.BOOLEAN;
		}
	}

	private static boolean hasAnnotation(TypeMirror type, String annotationName) {
		return type.getKind() == TypeKind.DECLARED
			&& getAnnotation(((DeclaredType) type).asElement(), annotationName)!=null;
	}

	private static boolean hasAnnotation(Element member, String annotationName) {
		return getAnnotation(member, annotationName)!=null;
	}

	private static AnnotationMirror getAnnotation(Element member, String annotationName) {
		for (AnnotationMirror mirror : member.getAnnotationMirrors()) {
			final TypeElement annotationType = (TypeElement) mirror.getAnnotationType().asElement();
			if ( annotationType.getSimpleName().contentEquals(annotationName)
					&& annotationType.getNestingKind() == NestingKind.TOP_LEVEL ) {
				final PackageElement pack = (PackageElement) annotationType.getEnclosingElement();
				final Name packageName = pack.getQualifiedName();
				if (packageName.contentEquals(jakartaPersistence)
						|| packageName.contentEquals(javaxPersistence)) {
					return mirror;
				}
			}
		}
		return null;
	}

	private static Object getAnnotationMember(AnnotationMirror annotation, String memberName) {
		if ( annotation == null ) {
			return null;
		}
		for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
				annotation.getElementValues().entrySet()) {
			if (entry.getKey().getSimpleName().contentEquals(memberName)) {
				return entry.getValue().getValue();
			}
		}
		return null;
	}

	static boolean isMappedClass(TypeElement type) {
		return hasAnnotation(type, "Entity")
			|| hasAnnotation(type, "Embeddable")
			|| hasAnnotation(type, "MappedSuperclass");
	}

	@Override
	protected boolean isEntity(String entityName) {
		return isEntity(elementUtil.getTypeElement(entityName));
	}

	static boolean isEntity(TypeElement member) {
		return member.getKind() == ElementKind.CLASS
//			&& member.getAnnotation(entityAnnotation)!=null;
			&& hasAnnotation(member, "Entity");
	}

	private static boolean isId(Element member) {
		return hasAnnotation(member, "Id");
	}

	private static boolean isStatic(Element member) {
		return member.getModifiers().contains(Modifier.STATIC);
	}

	private static boolean isTransient(Element member) {
		return hasAnnotation(member, "Transient")
			|| member.getModifiers().contains(Modifier.TRANSIENT);
	}

	private static boolean isEnumProperty(Element member) {
		if (hasAnnotation(member, "Enumerated")) {
			return true;
		}
		else {
			final TypeMirror type = member.asType();
			if (type.getKind() == TypeKind.DECLARED) {
				final DeclaredType declaredType = (DeclaredType) type;
				final TypeElement typeElement = (TypeElement) declaredType.asElement();
				return typeElement.getKind() == ElementKind.ENUM;
			}
			else {
				return false;
			}
		}
	}

	@Override
	boolean isEnum(String className) {
		final TypeElement typeElement = elementUtil.getTypeElement( className );
		return typeElement != null && typeElement.getKind() == ElementKind.ENUM;
	}

	@Override
	boolean isEnumConstant(String className, String terminal) {
		final TypeElement typeElement = elementUtil.getTypeElement(className);
		if (typeElement == null || typeElement.getKind() != ElementKind.ENUM) {
			return false;
		}
		return typeElement.getEnclosedElements()
				.stream()
				.filter(e -> terminal.equals(e.getSimpleName().toString()))
				.anyMatch(e -> e.getKind() == ElementKind.ENUM_CONSTANT);
	}

	@Override
	Class<?> javaConstantType(String className, String fieldName) {
		final TypeElement typeElement = elementUtil.getTypeElement( className );
		if ( typeElement == null ) {
			return null;
		}
		final TypeMirror typeMirror =
				typeElement.getEnclosedElements()
						.stream()
						.filter( e -> fieldName.equals( e.getSimpleName().toString() ) )
						.filter( ProcessorSessionFactory::isStaticFinalField )
						.findFirst().map( Element::asType )
						.orElse( null );
		if ( typeMirror == null ) {
			return null;
		}
		try {
			return switch ( typeMirror.getKind() ) {
				case BYTE -> byte.class;
				case SHORT -> short.class;
				case INT -> int.class;
				case LONG -> long.class;
				case FLOAT -> float.class;
				case DOUBLE -> double.class;
				case BOOLEAN -> boolean.class;
				case CHAR -> char.class;
				default -> Class.forName( typeMirror.toString() );
			};
		}
		catch (ClassNotFoundException ignored) {
			return null;
		}
	}

	private static boolean isStaticFinalField(Element e) {
		return e.getKind() == ElementKind.FIELD
			&& e.getModifiers().contains( Modifier.STATIC )
			&& e.getModifiers().contains( Modifier.FINAL );
	}

	private static boolean isEmbeddableType(TypeElement type) {
		return hasAnnotation(type, "Embeddable");
	}

	private static boolean isEmbeddedProperty(Element member) {
		if (hasAnnotation(member, "Embedded")) {
			return true;
		}
		else {
			final TypeMirror type = member.asType();
			return type.getKind() == TypeKind.DECLARED
				&& hasAnnotation(type, "Embeddable");
		}
	}

	private static boolean isElementCollectionProperty(Element member) {
		return hasAnnotation(member, "ElementCollection");
	}

	private static boolean isToOneAssociation(Element member) {
		return hasAnnotation(member, "ManyToOne")
			|| hasAnnotation(member, "OneToOne");
	}

	private static boolean isToManyAssociation(Element member) {
		return hasAnnotation(member, "ManyToMany")
			|| hasAnnotation(member, "OneToMany");
	}

	private static AnnotationMirror toOneAnnotation(Element member) {
		final AnnotationMirror manyToOne =
				getAnnotation(member, "ManyToOne");
		if (manyToOne!=null) {
			return manyToOne;
		}
		final AnnotationMirror oneToOne =
				getAnnotation(member, "OneToOne");
		if (oneToOne!=null) {
			return oneToOne;
		}
		return null;
	}

	private static AnnotationMirror toManyAnnotation(Element member) {
		final AnnotationMirror manyToMany =
				getAnnotation(member, "ManyToMany");
		if (manyToMany!=null) {
			return manyToMany;
		}
		final AnnotationMirror oneToMany =
				getAnnotation(member, "OneToMany");
		if (oneToMany!=null) {
			return oneToMany;
		}
		return null;
	}

	private static String simpleName(TypeMirror type) {
		return type.getKind() == TypeKind.DECLARED
				? simpleName(asElement(type))
				: type.toString();
	}

	private static String qualifiedName(TypeMirror type) {
		return type.getKind() == TypeKind.DECLARED
				? qualifiedName(asElement(type))
				: type.toString();
	}

	private static String simpleName(Element type) {
		return type.getSimpleName().toString();
	}

	private static String qualifiedName(Element type) {
		if ( type instanceof PackageElement packageElement ) {
			return packageElement.getQualifiedName().toString();
		}
		else if ( type instanceof TypeElement typeElement ) {
			return typeElement.getQualifiedName().toString();
		}
		else {
			final Element enclosingElement = type.getEnclosingElement();
			return enclosingElement != null
					? qualifiedName(enclosingElement) + '.' + simpleName(type)
					: simpleName(type);
		}
	}

	private static AccessType getAccessType(TypeElement type, AccessType defaultAccessType) {
		final AnnotationMirror annotation =
				getAnnotation(type, "Access");
		if (annotation==null) {
			return defaultAccessType;
		}
		else {
			final VariableElement member = (VariableElement)
					getAnnotationMember(annotation, "value");
			if (member==null) {
				return defaultAccessType; //does not occur
			}
			return switch (member.getSimpleName().toString()) {
				case "PROPERTY" -> AccessType.PROPERTY;
				case "FIELD" -> AccessType.FIELD;
				default -> throw new IllegalStateException();
			};
		}
	}

	@Override
	protected String getJpaEntityName(String typeName) {
		return getJpaEntityName(findClassByQualifiedName(typeName));
	}

	static String getJpaEntityName(TypeElement type) {
		if ( type == null ) {
			return null;
		}
		final AnnotationMirror entityAnnotation =
				getAnnotation(type, "Entity");
		if (entityAnnotation==null) {
			//not an entity!
			return null;
		}
		else {
			final String name = (String)
					getAnnotationMember(entityAnnotation, "name");
			//JPA entity names are unqualified class names
			return name==null ? simpleName(type) : name;
		}
	}

	static String getHibernateEntityName(TypeElement type) {
		if ( type == null ) {
			return null;
		}
		final AnnotationMirror entityAnnotation =
				getAnnotation(type, "Entity");
		if (entityAnnotation==null) {
			//not an entity!
			return null;
		}
		else {
			//entity names are qualified class names
			return qualifiedName(type);
		}
	}

	private TypeMirror getCollectionElementType(Element property) {
		final DeclaredType declaredType = (DeclaredType) memberType(property);
		final List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
		final TypeMirror elementType = typeArguments.get(typeArguments.size()-1);
		return elementType==null
				? elementUtil.getTypeElement(JAVA_OBJECT).asType()
				: elementType;
	}

	private static String getToOneTargetEntity(Element property) {
		final AnnotationMirror annotation = toOneAnnotation(property);
		final TypeMirror classType = (TypeMirror)
				getAnnotationMember(annotation, "targetEntity");
		final TypeMirror targetType =
				classType == null || classType.getKind() == TypeKind.VOID
						? memberType(property)
						: classType;
		final Element element = asElement(targetType);
		return element != null && element.getKind() == ElementKind.CLASS
				//entity names are qualified class names
				? getHibernateEntityName((TypeElement) element)
				: null;
	}

	private String getToManyTargetEntityName(Element property) {
		final AnnotationMirror annotation = toManyAnnotation(property);
		final TypeMirror classType = (TypeMirror)
				getAnnotationMember(annotation, "targetEntity");
		final TypeMirror targetType =
				classType == null || classType.getKind() == TypeKind.VOID
						? getCollectionElementType(property)
						: classType;
		final Element element = asElement(targetType);
		return element != null && element.getKind() == ElementKind.CLASS
				//entity names are qualified class names
				? getHibernateEntityName((TypeElement) element)
				: null;
	}

	private TypeMirror getElementCollectionElementType(Element property) {
		final AnnotationMirror annotation = getAnnotation(property, "ElementCollection");
		final TypeMirror classType = (TypeMirror)
				getAnnotationMember(annotation, "getElementCollectionClass");
		return classType == null
			|| classType.getKind() == TypeKind.VOID
				? getCollectionElementType(property)
				: classType;
	}

	@Override
	protected String getSupertype(String entityName) {
		return asElement(findEntityClass(entityName).getSuperclass())
				.getSimpleName().toString();
	}

	@Override
	protected boolean isSubtype(String entityName, String subtypeEntityName) {
		return typeUtil.isSubtype( findEntityClass(entityName).asType(),
				findEntityClass(subtypeEntityName).asType());
	}

	@Override
	boolean isClassDefined(String qualifiedName) {
		return findClassByQualifiedName(qualifiedName)!=null;
	}

	@Override
	boolean isFieldDefined(String qualifiedClassName, String fieldName) {
		final TypeElement type = findClassByQualifiedName(qualifiedClassName);
		return type != null
			&& type.getEnclosedElements().stream()
				.anyMatch(element -> element.getKind() == ElementKind.FIELD
						&& element.getSimpleName().contentEquals(fieldName));
	}

	@Override
	boolean isConstructorDefined(String qualifiedClassName, List<Type> argumentTypes) {
		final TypeElement symbol = findClassByQualifiedName(qualifiedClassName);
		if (symbol==null) {
			return false;
		}
		for (Element cons: symbol.getEnclosedElements()) {
			if ( cons.getKind() == ElementKind.CONSTRUCTOR ) {
				final ExecutableElement constructor = (ExecutableElement) cons;
				final List<? extends VariableElement> parameters = constructor.getParameters();
				if (parameters.size()==argumentTypes.size()) {
					boolean argumentsCheckOut = true;
					for (int i=0; i<argumentTypes.size(); i++) {
						final Type type = argumentTypes.get(i);
						final VariableElement param = parameters.get(i);
						if (param.asType().getKind().isPrimitive()) {
							final Class<?> primitive;
							try {
								primitive = toPrimitiveClass( type.getReturnedClass() );
							}
							catch (Exception e) {
								continue;
							}
							if (!toPrimitiveClass(param).equals(primitive)) {
								argumentsCheckOut = false;
								break;
							}
						}
						else {
							final TypeElement typeClass;
							if ( type instanceof EntityType entityType ) {
								typeClass = findEntityClass(entityType.getAssociatedEntityName());
							}
							//TODO:
	//						else if (type instanceof CompositeCustomType) {
	//							typeClass = ((Component) ((CompositeCustomType) type).getUserType()).type;
	//						}
							else if (type instanceof BasicType) {
								final String className;
								//TODO: custom impl of getReturnedClassName()
								//      for many more Hibernate types!
								try {
									className = type.getReturnedClassName();
								}
								catch (Exception e) {
									continue;
								}
								typeClass = findClassByQualifiedName(className);
							}
							else {
								//TODO: what other Hibernate Types do we
								//	  need to consider here?
								continue;
							}
							if (typeClass != null
									&& !typeUtil.isSubtype( typeClass.asType(), param.asType() ) ) {
								argumentsCheckOut = false;
								break;
							}
						}
					}
					if (argumentsCheckOut) {
						return true; //matching constructor found!
					}
				}
			}
		}
		return false;
	}

	private static Class<?> toPrimitiveClass(VariableElement param) {
		return switch ( param.asType().getKind() ) {
			case BOOLEAN -> boolean.class;
			case CHAR -> char.class;
			case INT -> int.class;
			case SHORT -> short.class;
			case BYTE -> byte.class;
			case LONG -> long.class;
			case FLOAT -> float.class;
			case DOUBLE -> double.class;
			default -> Object.class;
		};
	}

	private TypeElement findClassByQualifiedName(String path) {
		return path == null ? null : elementUtil.getTypeElement(path);
	}

	private static AccessType getDefaultAccessType(TypeElement type) {
		//iterate up the superclass hierarchy
		while (type!=null) {
			for (Element member: type.getEnclosedElements()) {
				if (isId(member)) {
					return member instanceof ExecutableElement
							? AccessType.PROPERTY
							: AccessType.FIELD;
				}
			}
			type = (TypeElement) asElement(type.getSuperclass());
		}
		return AccessType.FIELD;
	}

	private static String propertyName(Element symbol) {
		String name = symbol.getSimpleName().toString();
		if (symbol.getKind() == ElementKind.METHOD) {
			if (name.startsWith("get")) {
				name = name.substring(3);
			}
			else if (name.startsWith("is")) {
				name = name.substring(2);
			}
			return Introspector.decapitalize(name);
		}
		else {
			return name;
		}
	}

	private static boolean isPersistable(Element member, AccessType accessType) {
		if (isStatic(member) || isTransient(member)) {
			return false;
		}
		else if (member.getKind() == ElementKind.FIELD) {
			return accessType == AccessType.FIELD
//				|| member.getAnnotation( accessAnnotation ) != null;
				|| hasAnnotation(member, "Access");
		}
		else if (member.getKind() == ElementKind.METHOD) {
			return isGetterMethod((ExecutableElement) member)
				&& (accessType == AccessType.PROPERTY
//					|| member.getAnnotation( accessAnnotation ) != null);
					|| hasAnnotation(member, "Access"));
		}
		else {
			return false;
		}
	}

	private static TypeMirror memberType(Element member) {
		if (member instanceof ExecutableElement executableElement) {
			return executableElement.getReturnType();
		}
		else if (member instanceof VariableElement) {
			return member.asType();
		}
		else {
			throw new IllegalArgumentException("Not a member");
		}
	}

	public static Element asElement(TypeMirror type) {
		if ( type == null ) {
			return null;
		}
		else {
			return switch ( type.getKind() ) {
				case DECLARED -> ((DeclaredType) type).asElement();
				case TYPEVAR -> ((TypeVariable) type).asElement();
				default -> null;
			};
		}
	}
}
