/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.antlr.v4.runtime.Token;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.AssertionFailure;
import org.hibernate.grammars.hql.HqlLexer;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.processor.Context;
import org.hibernate.processor.ImportContextImpl;
import org.hibernate.processor.ProcessLaterException;
import org.hibernate.processor.model.ImportContext;
import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;
import org.hibernate.processor.util.AccessTypeInformation;
import org.hibernate.processor.util.Constants;
import org.hibernate.processor.util.TypeUtils;
import org.hibernate.processor.validation.ProcessorSessionFactory;
import org.hibernate.processor.validation.Validation;
import org.hibernate.query.criteria.JpaEntityJoin;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.hql.internal.HqlParseTreeBuilder;
import org.hibernate.query.sql.internal.ParameterParser;
import org.hibernate.query.sql.spi.ParameterRecognizer;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import jakarta.persistence.AccessType;

import static java.beans.Introspector.decapitalize;
import static java.lang.Boolean.FALSE;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static org.hibernate.grammars.hql.HqlLexer.FROM;
import static org.hibernate.grammars.hql.HqlLexer.GROUP;
import static org.hibernate.grammars.hql.HqlLexer.HAVING;
import static org.hibernate.grammars.hql.HqlLexer.ORDER;
import static org.hibernate.grammars.hql.HqlLexer.WHERE;
import static org.hibernate.internal.util.StringHelper.qualify;
import static org.hibernate.internal.util.StringHelper.unqualify;
import static org.hibernate.metamodel.mapping.EntityIdentifierMapping.ID_ROLE_NAME;
import static org.hibernate.processor.annotation.AbstractQueryMethod.isRangeParam;
import static org.hibernate.processor.annotation.AbstractQueryMethod.isRestrictionParam;
import static org.hibernate.processor.annotation.AbstractQueryMethod.isSessionParameter;
import static org.hibernate.processor.annotation.AbstractQueryMethod.isSpecialParam;
import static org.hibernate.processor.annotation.QueryMethod.isOrderParam;
import static org.hibernate.processor.annotation.QueryMethod.isPageParam;
import static org.hibernate.processor.util.Constants.*;
import static org.hibernate.processor.util.NullnessUtil.castNonNull;
import static org.hibernate.processor.util.StringUtil.removeDollar;
import static org.hibernate.processor.util.TypeUtils.containsAnnotation;
import static org.hibernate.processor.util.TypeUtils.determineAccessTypeForHierarchy;
import static org.hibernate.processor.util.TypeUtils.determineAnnotationSpecifiedAccessType;
import static org.hibernate.processor.util.TypeUtils.extendsClass;
import static org.hibernate.processor.util.TypeUtils.findMappedSuperElement;
import static org.hibernate.processor.util.TypeUtils.getAnnotationMirror;
import static org.hibernate.processor.util.TypeUtils.getAnnotationValue;
import static org.hibernate.processor.util.TypeUtils.getGeneratedClassFullyQualifiedName;
import static org.hibernate.processor.util.TypeUtils.getInheritedAnnotationMirror;
import static org.hibernate.processor.util.TypeUtils.hasAnnotation;
import static org.hibernate.processor.util.TypeUtils.implementsInterface;
import static org.hibernate.processor.util.TypeUtils.primitiveClassMatchesKind;
import static org.hibernate.processor.util.TypeUtils.propertyName;

/**
 * Class used to collect meta information about an annotated type (entity, embeddable or mapped superclass).
 * Also repurposed for any type with "auxiliary" annotations like {@code @NamedQuery}, {@code @FetchProfile},
 * {@code @Find}, or {@code @HQL}. We do not distinguish these two kinds of thing, since an entity class may
 * {@code @NamedQuery} or {@code @FetchProfile} annotations. Entities may not, however, have methods annotated
 * {@code @Find} or {@code @HQL}, since entity classes are usually concrete classes.
 *
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 * @author Gavin King
 * @author Yanming Zhou
 */
public class AnnotationMetaEntity extends AnnotationMeta {

	private static final String ID_CLASS_MEMBER_NAME = "<ID_CLASS>";

	private final ImportContext importContext;
	private final TypeElement element;
	private final Map<String, MetaAttribute> members;
	private final Context context;
	private final boolean managed;
	private boolean jakartaDataRepository;
	private final boolean quarkusInjection;
	private final boolean springInjection;
	private String qualifiedName;
	private final boolean jakartaDataStaticModel;

	private AccessTypeInformation entityAccessTypeInfo;

	/**
	 * Whether the members of this type have already been initialized or not.
	 * <p>
	 * Embeddables and mapped superclasses need to be lazily initialized since the access type may be determined
	 * by the class which is embedding or subclassing the entity or superclass. This might not be known until
	 * annotations are processed.
	 * <p>
	 * Also note, that if two different classes with different access types embed this entity or extend this mapped
	 * superclass, the access type of the embeddable/superclass will be the one of the last embedding/subclassing
	 * entity processed. The result is not determined (that's ok according to the spec).
	 */
	private boolean initialized;

	/**
	 * Another meta entity for the same type which should be merged lazily with this meta entity. Doing the merge
	 * lazily is required for embeddables and mapped supertypes, to only pull in those members matching the access
	 * type as configured via the embedding entity or subclass (also see METAGEN-85).
	 */
	private Metamodel entityToMerge;

	/**
	 * True if this "metamodel class" is actually an instantiable DAO-style repository.
	 */
	private boolean repository = false;

	/**
	 * The type of the "session getter" method of a DAO-style repository.
	 */
	private String sessionType = ENTITY_MANAGER;

	/**
	 * The field or method call to obtain the session
	 */
	private String sessionGetter = "entityManager";

	private final Map<String,String> memberTypes = new HashMap<>();

	/**
	 * The primary entity type for a repository
	 */
	private @Nullable TypeElement primaryEntity;

	public AnnotationMetaEntity(
			TypeElement element, Context context, boolean managed,
			boolean jakartaDataStaticMetamodel,
			@Nullable AnnotationMeta parent,
			@Nullable TypeElement primaryEntity) {
		this.element = element;
		this.context = context;
		this.managed = managed;
		this.members = new LinkedHashMap<>();
		this.quarkusInjection = context.isQuarkusInjection();
		this.springInjection = context.isSpringInjection();
		this.importContext = parent != null ? parent : new ImportContextImpl( getPackageName( context, element ) );
		jakartaDataStaticModel = jakartaDataStaticMetamodel;
		this.primaryEntity = primaryEntity;
		importContext.importType(
				getGeneratedClassFullyQualifiedName( element, getPackageName( context, element ),
						jakartaDataStaticModel ) );
		if ( !element.getQualifiedName().toString().endsWith( "$" ) ) {
			importContext.importType( element.getQualifiedName().toString() );
		}
	}

	public static AnnotationMetaEntity create(TypeElement element, Context context, @Nullable AnnotationMetaEntity parent,
			@Nullable TypeElement primaryEntity) {
		return create( element,context, false, false, false, parent, primaryEntity );
	}

	public static AnnotationMetaEntity create(TypeElement element, Context context, @Nullable AnnotationMetaEntity parent) {
		return create( element,context, false, false, false, parent, null );
	}

	public static AnnotationMetaEntity create(
			TypeElement element, Context context,
			boolean lazilyInitialised, boolean managed,
			boolean jakartaData,
			@Nullable AnnotationMetaEntity parent,
			@Nullable TypeElement primaryEntity) {
		final AnnotationMetaEntity annotationMetaEntity =
				new AnnotationMetaEntity( element, context, managed, jakartaData, parent, primaryEntity );
		if ( parent != null ) {
			parent.addInnerClass( annotationMetaEntity );
		}
		if ( !lazilyInitialised ) {
			annotationMetaEntity.init();
		}
		return annotationMetaEntity;
	}

	public @Nullable String getMemberType(String entityType, String memberName) {
		return memberTypes.get( qualify(entityType, memberName) );
	}

	public AccessTypeInformation getEntityAccessTypeInfo() {
		return entityAccessTypeInfo;
	}

	@Override
	public final Context getContext() {
		return context;
	}

	@Override
	public boolean isImplementation() {
		return repository;
	}

	@Override
	public boolean isJakartaDataStyle() {
		return jakartaDataStaticModel;
	}

	public boolean isInitialized() {
		return initialized;
	}

	@Override
	public final String getSimpleName() {
		return removeDollar( element.getSimpleName().toString() );
	}

	private String getConstructorName() {
		return getSimpleName() + '_';
	}

	@Override
	public final String getQualifiedName() {
		if ( qualifiedName == null ) {
			qualifiedName = element.getQualifiedName().toString();
		}
		return qualifiedName;
	}

	@Override
	public @Nullable Element getSuperTypeElement() {
		return repository ? null : findMappedSuperElement( this, context );
	}

	@Override
	public String getPackageName() {
		return getPackageName( context, element );
	}

	private static String getPackageName(Context context, TypeElement element) {
		return context.getElementUtils().getPackageOf( element ).getQualifiedName().toString();
	}

	@Override
	public List<MetaAttribute> getMembers() {
		if ( !initialized ) {
			init();
			if ( entityToMerge != null ) {
				mergeInMembers( entityToMerge.getMembers() );
			}
		}
		return new ArrayList<>( members.values() );
	}

	public void addInnerClass(AnnotationMetaEntity metaEntity) {
		putMember( "INNER_" + metaEntity.getQualifiedName(),
				new InnerClassMetaAttribute( metaEntity ) );
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

	public void mergeInMembers(Metamodel other) {
		// store the entity in order do the merge lazily in case of
		// an uninitialized embeddedable or mapped superclass
		if ( !initialized ) {
			entityToMerge = other;
		}
		else {
			mergeInMembers( other.getMembers() );
		}
	}

	@Override
	public final String generateImports() {
		return importContext.generateImports();
	}

	@Override
	public final String importType(String fqcn) {
		return importContext.importType( fqcn );
	}

	@Override
	public final String staticImport(String fqcn, String member) {
		return importContext.staticImport( fqcn, member );
	}

	@Override
	public final TypeElement getElement() {
		return element;
	}

	@Override
	void putMember(String name, MetaAttribute nameMetaAttribute) {
		members.put( name, nameMetaAttribute );
	}

	@Override
	boolean isRepository() {
		return repository;
	}

	boolean isJakartaDataRepository() {
		return jakartaDataRepository;
	}

	@Override
	String getSessionType() {
		return sessionType;
	}

	@Override
	public boolean isInjectable() {
		return repository;
	}

	@Override
	public String scope() {
		// @TransactionScoped doesn't work here because repositories
		// are supposed to be able to demarcate transactions, which
		// means they should be injectable when there is no active tx
		// @RequestScoped doesn't work because Arc folks think this
		// scope should only be active during a HTTP request, which
		// is simply wrong according to me, but whatever
		// @ApplicationScoped could work in principle, but buys us
		// nothing additional, since repositories are stateless
		return "jakarta.enterprise.context.Dependent";
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append( "AnnotationMetaEntity" )
				.append( "{element=" )
				.append( element )
				.append( ", members=" )
				.append( members )
				.append( '}' )
				.toString();
	}

	protected void init() {
		getContext().logMessage( Diagnostic.Kind.OTHER, "Initializing type '" + getQualifiedName() + "'" );

		setupSession();

		final List<ExecutableElement> queryMethods = new ArrayList<>();
		final List<ExecutableElement> lifecycleMethods = new ArrayList<>();

		if ( repository ) {
			final List<ExecutableElement> methodsOfClass =
					methodsIn( context.getAllMembers(element) );
			for ( ExecutableElement method: methodsOfClass ) {
				if ( containsAnnotation( method, HQL, SQL, JD_QUERY, FIND, JD_FIND ) ) {
					queryMethods.add( method );
				}
				else if ( containsAnnotation( method, JD_INSERT, JD_UPDATE, JD_SAVE ) ) {
					lifecycleMethods.add( method );
				}
				else if ( hasAnnotation( method, JD_DELETE ) ) {
					if ( isDeleteLifecycle(method) ) {
						lifecycleMethods.add( method );
					}
					else {
						queryMethods.add( method );
					}
				}
				else if ( method.getEnclosingElement().getKind().isInterface()
						&& !method.isDefault()
						&& !method.getModifiers().contains(Modifier.PRIVATE)
						&& !isSessionGetter(method) ) {
					final String companionClassName = element.getQualifiedName().toString() + '$';
					if ( context.getElementUtils().getTypeElement(companionClassName) == null ) {
						message( method, "repository method cannot be implemented (skipping whole repository)",
								Diagnostic.Kind.WARNING );
					}
					// NOTE EARLY EXIT with initialized = false
					return;
				}
			}

			final boolean hibernateRepo = isExplicitlyHibernateRepository();
			if ( primaryEntity == null ) {
				primaryEntity = primaryEntity( lifecycleMethods );
			}
			if ( !checkEntity( primaryEntity, hibernateRepo )
					|| !checkEntities( lifecycleMethods, hibernateRepo ) ) {
				// NOTE EARLY EXIT with initialized = false
				return;
			}

			if ( !lifecycleMethods.isEmpty() ) {
				validateStatelessSessionType();
			}
		}
		else {
			determineAccessTypeForHierarchy( element, context );
			entityAccessTypeInfo = castNonNull( context.getAccessTypeInfo( getQualifiedName() ) );

			final List<VariableElement> fieldsOfClass = fieldsIn( element.getEnclosedElements() );
			final List<ExecutableElement> methodsOfClass = methodsIn( element.getEnclosedElements() );
			final List<ExecutableElement> gettersAndSettersOfClass = new ArrayList<>();
			for ( ExecutableElement method: methodsOfClass ) {
				if ( isGetterOrSetter( method ) ) {
					gettersAndSettersOfClass.add( method );
				}
				else if ( element.getTypeParameters().isEmpty()
						&& containsAnnotation( method, HQL, SQL, FIND ) ) {
					queryMethods.add( method );
				}
			}

			if ( managed && !jakartaDataStaticModel ) {
				putMember( "class", new AnnotationMetaType(this) );
			}

			validatePersistentMembers( fieldsOfClass );
			validatePersistentMembers( gettersAndSettersOfClass );

			addPersistentMembers( fieldsOfClass, AccessType.FIELD );
			addPersistentMembers( gettersAndSettersOfClass, AccessType.PROPERTY );

			addIdClassIfNeeded( fieldsOfClass, gettersAndSettersOfClass );

			if( hasAnnotation( element, ENTITY) && isPanache2Type(element) && !jakartaDataStaticModel ) {
				addRepositoryMembers( element );
			}
		}

		addAuxiliaryMembers();

		checkNamedQueries();

		addLifecycleMethods( lifecycleMethods );

		addQueryMethods( queryMethods );

		initialized = true;
	}

	private boolean checkEntity(@Nullable TypeElement entity, boolean hibernateRepo) {
		if ( entity != null && !hasAnnotation( entity, ENTITY ) ) {
			if ( hibernateRepo ) {
				context.message( element,
						"unrecognized primary entity type: " + entity.getQualifiedName(),
						Diagnostic.Kind.ERROR );
			}
			return false;
		}
		return true;
	}

	private boolean isExplicitlyHibernateRepository() {
		final AnnotationMirror repository = getAnnotationMirror( element, JD_REPOSITORY );
		if ( repository != null ) {
			final AnnotationValue provider = getAnnotationValue( repository, "provider" );
			return provider != null && provider.getValue().toString().equalsIgnoreCase( "hibernate" );
		}
		else {
			return false;
		}
	}

	/**
	 * Creates a generated id class named {@code Entity_.Id} if the
	 * entity has multiple {@code @Id} fields, but no {@code @IdClass}
	 * annotation.
	 */
	private void addIdClassIfNeeded(List<VariableElement> fields, List<ExecutableElement> methods) {
		if ( hasAnnotation( element, ID_CLASS ) ) {
			checkIdMembers( fields, methods );
		}
		else {
			final List<MetaAttribute> components = getIdMemberNames( fields, methods );
			if ( components.size() >= 2 ) {
				putMember( ID_CLASS_MEMBER_NAME, new IdClassMetaAttribute( this, components ) );
			}
		}
	}

	private void addRepositoryMembers(TypeElement element) {
		Element managedBlockingRepository = null;
		Element statelessBlockingRepository = null;
		Element managedReactiveRepository = null;
		Element statelessReactiveRepository = null;
		for ( Element enclosedElement : element.getEnclosedElements() ) {
			if ( enclosedElement.getKind() == ElementKind.INTERFACE ) {
				members.put( enclosedElement.getSimpleName().toString(), new CDIAccessorMetaAttribute( this, enclosedElement ) );
				if ( implementsInterface( (TypeElement) enclosedElement, Constants.PANACHE2_MANAGED_BLOCKING_REPOSITORY_BASE ) ) {
					managedBlockingRepository = enclosedElement;
				}
				else if ( implementsInterface( (TypeElement) enclosedElement, Constants.PANACHE2_STATELESS_BLOCKING_REPOSITORY_BASE ) ) {
					statelessBlockingRepository = enclosedElement;
				}
				else if ( implementsInterface( (TypeElement) enclosedElement, Constants.PANACHE2_MANAGED_REACTIVE_REPOSITORY_BASE ) ) {
					managedReactiveRepository = enclosedElement;
				}
				else if ( implementsInterface( (TypeElement) enclosedElement, Constants.PANACHE2_STATELESS_REACTIVE_REPOSITORY_BASE ) ) {
					statelessReactiveRepository = enclosedElement;
				}
			}
		}
		if ( quarkusInjection ) {
			// FIXME: perhaps import id type?
			TypeMirror idType = findIdType();
			addAccessors(managedBlockingRepository, idType, "managedBlocking", PANACHE2_MANAGED_BLOCKING_REPOSITORY_BASE);
			addAccessors(statelessBlockingRepository, idType, "statelessBlocking", PANACHE2_STATELESS_BLOCKING_REPOSITORY_BASE);
			// Only add those if HR is in the classpath, otherwise it causes a compilation issue
			if( context.usesQuarkusReactiveCommon() ) {
				addAccessors(managedReactiveRepository, idType, "managedReactive", PANACHE2_MANAGED_REACTIVE_REPOSITORY_BASE);
				addAccessors(statelessReactiveRepository, idType, "statelessReactive", PANACHE2_STATELESS_REACTIVE_REPOSITORY_BASE);
			}
		}
	}

	private List<MetaAttribute> getIdMemberNames(List<VariableElement> fields, List<ExecutableElement> methods) {
		final List<MetaAttribute> components = new ArrayList<>();
		for ( var field : fields ) {
			if ( isIdField( field ) ) {
				final String propertyName = propertyName( field );
				if ( members.containsKey( propertyName ) ) {
					components.add( members.get( propertyName ) );
				}
			}
		}
		for ( var method : methods ) {
			if ( isIdProperty( method ) ) {
				final String propertyName = propertyName( method );
				if ( members.containsKey( propertyName ) ) {
					components.add( members.get( propertyName ) );
				}
			}
		}
		return components;
	}

	private void checkIdMembers(List<VariableElement> fields, List<ExecutableElement> methods) {
		final AnnotationMirror annotationMirror = getAnnotationMirror( element, ID_CLASS );
		if ( annotationMirror != null ) {
			final AnnotationValue annotationValue = getAnnotationValue( annotationMirror );
			if ( annotationValue != null && annotationValue.getValue() instanceof DeclaredType declaredType ) {
				final TypeElement idClass = (TypeElement) declaredType.asElement();
				if ( fields.stream().filter( this::isIdField ).count()
					+ methods.stream().filter( this::isIdProperty ).count()
							== 1 ) {
					for ( var field : fields ) {
						if ( isIdField( field ) ) {
							if ( hasAnnotation( field, ONE_TO_ONE ) ) {
								// Special case for @Id @OneToOne to an associated entity with an @IdClass
								//TODO: check the id type of the associated entity is the same as idClass
								return;
							}
						}
					}
					for ( var method : methods ) {
						if ( isIdProperty( method ) ) {
							if ( hasAnnotation( method, ONE_TO_ONE ) ) {
								// Special case for @Id @OneToOne to an associated entity with an @IdClass
								//TODO: check the id type of the associated entity is the same as idClass
								return;
							}
						}
					}
				}
				else {
					for ( var field : fields ) {
						if ( isIdField( field ) ) {
							memberStream( idClass )
									.filter( element -> element.getKind() == ElementKind.FIELD
														&& element.getSimpleName().contentEquals( field.getSimpleName() ) )
									.findAny()
									.ifPresentOrElse( match -> {
												if ( !isMatchingIdType( field, field.asType(), match.asType() ) ) {
													context.message( match,
															"id field should be of type '" + field.asType() + "'",
															Diagnostic.Kind.ERROR );
												}
											},
											() -> context.message( field,
													"no matching field in id class '" + idClass.getSimpleName() + "'",
													Diagnostic.Kind.ERROR ) );
						}
					}
					for ( var method : methods ) {
						if ( isIdProperty( method ) ) {
							memberStream( idClass )
									.filter( element -> element.getKind() == ElementKind.METHOD
														&& element.getSimpleName().contentEquals( method.getSimpleName() )
														&& isMatchingIdType( method, method.getReturnType(),
																		((ExecutableElement) element).getReturnType() ) )
									.findAny()
									.ifPresentOrElse( match -> {
											},
											() -> context.message( method,
													"no matching property in id class '" + idClass.getSimpleName() + "'",
													Diagnostic.Kind.ERROR ) );
						}
					}
				}
			}
		}
	}

	private boolean isIdProperty(ExecutableElement method) {
		return hasAnnotation( method, ID )
			&& isPersistent( method, AccessType.PROPERTY );
	}

	private boolean isIdField(VariableElement field) {
		return hasAnnotation( field, ID )
			&& isPersistent( field, AccessType.FIELD );
	}

	private static Stream<? extends Element> memberStream(TypeElement idClass) {
		Stream<? extends Element> result = idClass.getEnclosedElements().stream();
		TypeMirror superclass = idClass.getSuperclass();
		while ( superclass.getKind() == TypeKind.DECLARED ) {
			final DeclaredType declaredType = (DeclaredType) superclass;
			final TypeElement typeElement = (TypeElement) declaredType.asElement();
			result = Stream.concat( result, typeElement.getEnclosedElements().stream() );
			superclass = typeElement.getSuperclass();
		}
		return result;
	}

	private boolean isMatchingIdType(Element id, TypeMirror type, TypeMirror match) {
		return isSameType( type, match )
			|| isEquivalentPrimitiveType( type, match )
			|| isEquivalentPrimitiveType( match, type )
			//TODO: check the id type of the associated entity
			|| hasAnnotation( id, MANY_TO_ONE, ONE_TO_ONE );
	}

	private boolean isSameType(TypeMirror type, TypeMirror match) {
		return context.getTypeUtils().isSameType( type, match );
	}

	private boolean isEquivalentPrimitiveType(TypeMirror type, TypeMirror match) {
		return type.getKind().isPrimitive()
			&& isSameType( context.getTypeUtils().boxedClass( ((PrimitiveType) type) ).asType(), match );
	}

	private void addAccessors(@Nullable Element repositoryType, @Nullable TypeMirror idType,
							String repositoryAccessor, String repositorySuperType) {
		TypeElement finalPrimaryEntity = primaryEntity;
		if ( repositoryType != null ) {
			members.put( repositoryAccessor, new CDIAccessorMetaAttribute( this, repositoryAccessor,  repositoryType.getSimpleName().toString() ) );
		}
		else if ( idType != null && finalPrimaryEntity != null ) {
			String repositoryTypeName = "Panache"+repositoryAccessor.substring(0,1).toUpperCase()+repositoryAccessor.substring(1)+"Repository";
			members.put( repositoryAccessor, new CDIAccessorMetaAttribute( this, repositoryAccessor,  repositoryTypeName ) );
			members.put( repositoryAccessor + "Repository", new CDITypeMetaAttribute( this, repositoryTypeName, repositorySuperType +"<"+ finalPrimaryEntity.getSimpleName()+", "+ idType.toString()+">" ) );
		}
	}

	private @Nullable TypeMirror findIdType() {
		TypeElement primaryEntityForTest = primaryEntity;
		if ( primaryEntityForTest == null ) {
			return null;
		}
		AnnotationMirror idClass = getInheritedAnnotationMirror( this.context.getElementUtils(), primaryEntityForTest, ID_CLASS );
		if ( idClass != null ) {
			AnnotationValue value = getAnnotationValue(idClass, "value" );
			// I don't think this can have a null value
			if ( value != null ) {
				return (TypeMirror) value.getValue();
			}
		}
		Element idMember = findIdMember();
		if ( idMember != null ) {
			TypeMirror typedIdMember = this.context.getTypeUtils().asMemberOf((DeclaredType) primaryEntityForTest.asType(), idMember);
			return switch(typedIdMember.getKind()) {
				case ARRAY, DECLARED, BOOLEAN, BYTE, CHAR, SHORT, INT, LONG, FLOAT, DOUBLE -> typedIdMember;
				case EXECUTABLE -> ((ExecutableType) typedIdMember).getReturnType();
				default -> {
					message( element,
							"Unhandled id member kind: "+typedIdMember+" for id "+idMember,
							Diagnostic.Kind.ERROR );
					yield null;
				}
			};
		}
		return null;
	}

	private @Nullable Element findIdMember() {
		if ( primaryEntity == null ) {
			message( element,
					"No primary entity defined to find id member",
					Diagnostic.Kind.ERROR );
			return null;
		}
		for ( Element member : context.getAllMembers( primaryEntity ) ) {
			if ( hasAnnotation( member, ID, EMBEDDED_ID ) ) {
				return member;
			}
		}
		message( element,
				"Could not find any member annotated with @Id or @EmbeddedId",
				Diagnostic.Kind.ERROR );
		return null;
	}

	private boolean checkEntities(List<ExecutableElement> lifecycleMethods, boolean hibernateRepo) {
		boolean foundPersistenceEntity = false;
		VariableElement nonPersistenceParameter = null;
		for (ExecutableElement lifecycleMethod : lifecycleMethods) {
			final List<? extends VariableElement> parameters = lifecycleMethod.getParameters();
			if ( !parameters.isEmpty() ) {
				final VariableElement parameter = parameters.get(0);
				final TypeMirror declaredType = parameter.asType();
				final TypeMirror parameterType = parameterType(parameter);
				final DeclaredType type = entityType(parameterType);
				if ( type != null ) {
					if ( hasAnnotation( type.asElement(), ENTITY ) ) {
						foundPersistenceEntity = true;
					}
					else if ( declaredType == parameterType
							&& nonPersistenceParameter==null ) {
						nonPersistenceParameter = parameter;
					}
				}
			}
		}
		if ( foundPersistenceEntity && nonPersistenceParameter != null ) {
			message(nonPersistenceParameter,
					"parameter type '" + nonPersistenceParameter.asType()
							+ "' is not a Jakarta Persistence entity class (skipping entire repository)",
					hibernateRepo ? Diagnostic.Kind.ERROR : Diagnostic.Kind.WARNING);
		}
		return nonPersistenceParameter == null;
	}

	private void validateStatelessSessionType() {
		if ( !isStatelessSession() ) {
			message( element,
					"repository must be backed by a 'StatelessSession'",
					Diagnostic.Kind.ERROR );
		}
	}

	private static @Nullable TypeElement primaryEntity(TypeElement element) {
		for (TypeMirror typeMirror : element.getInterfaces()) {
			final DeclaredType declaredType = (DeclaredType) typeMirror;
			final TypeElement typeElement = (TypeElement) declaredType.asElement();
			final Name name = typeElement.getQualifiedName();
			if ( declaredType.getTypeArguments().size() == 2
					&& (name.contentEquals(BASIC_REPOSITORY)
						|| name.contentEquals(CRUD_REPOSITORY)
						|| name.contentEquals(DATA_REPOSITORY)) ) {
				final TypeMirror entityType = declaredType.getTypeArguments().get(0);
				if ( entityType.getKind() == TypeKind.DECLARED ) {
					final DeclaredType entityDeclared = (DeclaredType) entityType;
					return (TypeElement) entityDeclared.asElement();
				}
			}
			else {
				final TypeElement primaryEntity = primaryEntity(typeElement);
				if ( primaryEntity != null ) {
					return primaryEntity;
				}
			}
		}
		return null;
	}

	private @Nullable TypeElement primaryEntity(List<ExecutableElement> lifecycleMethods) {
		// first see if we are a subtype of DataRepository
		// in that case, the first type argument determines
		// the primary entity type
		TypeElement result = primaryEntity(element);
		if ( result != null ) {
			return result;
		}
		// otherwise, we have to check all the lifecycle
		// methods and determine if they all agree on the
		// entity type; if so, it is the primary
		final Types types = context.getTypeUtils();
		for ( ExecutableElement element : lifecycleMethods ) {
			if ( element.getParameters().size()==1 ) {
				final VariableElement param = element.getParameters().get(0);
				final DeclaredType declaredType = entityType( parameterType(param) );
				if ( declaredType != null ) {
					if ( result == null ) {
						result = (TypeElement) declaredType.asElement();
					}
					else {
						if ( !types.isSameType( result.asType(), declaredType ) ) {
							return null;
						}
					}
				}
			}
		}
		return result;
	}

	private void addDefaultConstructor() {
		final String sessionVariableName = getSessionVariableName(sessionType);
		putMember("_", new DefaultConstructor(
				this,
				getConstructorName(),
				sessionVariableName,
				sessionType,
				sessionVariableName,
				dataStore(),
				context.addInjectAnnotation()
		));
	}

	private @Nullable String dataStore() {
		final AnnotationMirror repo = getAnnotationMirror( element, JD_REPOSITORY );
		if ( repo != null ) {
			final AnnotationValue dataStoreValue = getAnnotationValue( repo, "dataStore" );
			if (dataStoreValue != null) {
				final String dataStore = dataStoreValue.getValue().toString();
				if ( !dataStore.isEmpty() ) {
					return dataStore;
				}
			}
		}
		return null;
	}

	private void setupSession() {
		if ( element.getTypeParameters().isEmpty() ) {
			jakartaDataRepository = hasAnnotation( element, JD_REPOSITORY );
			final ExecutableElement getter = findSessionGetter( element );
			if ( getter != null ) {
				// Never make a DAO for Panache subtypes
				if ( !isPanacheType( element ) && !isPanache2Type( element ) ) {
					repository = true;
					sessionType = addDaoConstructor( getter );
				}
				else if ( ! isPanache2Repository( element ) && !isPanache2Type( element ) ) {
					// For Panache 1 subtypes, we look at the session type, but no DAO,
					// we want static methods
					sessionType = fullReturnType(getter);
				}
				else {
					// For Panache 2 repositories we want a repository
					repository = true;
					sessionType =  setupQuarkusDaoConstructor( getter, element );
				}
			}
			else if ( element.getKind() == ElementKind.INTERFACE
					&& !jakartaDataRepository
					&& ( context.usesQuarkusOrm() || context.usesQuarkusReactive() || context.usesQuarkusPanache2() ) ) {
				// if we don't have a getter, and not a JD repository, but we're in Quarkus,
				// we know how to find the default sessions
				repository = true;
				sessionType = setupQuarkusDaoConstructor( null, element );
			}
			if ( !repository && jakartaDataRepository ) {
				repository = true;
				// Jakarta Data defaults to StatelessSession, not EntityManager
				sessionType = HIB_STATELESS_SESSION;
				// If it's Spring, we wrap the StatelessSession in ObjectProvider
				sessionType = addDaoConstructor( null );
			}
			if ( needsDefaultConstructor() ) {
				addDefaultConstructor();
			}
			if ( needsEventBus() ) {
				addEventBus();
			}
		}
	}

	private boolean needsEventBus() {
		return jakartaDataRepository
			&& !isReactive() // non-reactive
			&& context.isDataEventPackageAvailable() // events
			&& context.addInjectAnnotation() // @nject
			&& context.addDependentAnnotation(); // CDI
	}

	void addEventBus() {
		putMember("_event", new EventField(this) );
	}

	/**
	 * For usage with CDI, but outside Quarkus, Jakarta Data
	 * repositories use {@code @PersistenceUnit} to obtain an
	 * {@code EntityManagerFactory} via field injection. So in
	 * that case we will need a {@link DefaultConstructor default
	 * constructor}. We don't do this in Quarkus, because there
	 * we can just inject the {@code StatelessSession} directly,
	 * and so in Quarkus we don't need the default constructor
	 * at all.
	 */
	boolean needsDefaultConstructor() {
		return jakartaDataRepository
			&& !quarkusInjection
			&& !springInjection
			&& context.addDependentAnnotation();
	}

	private @Nullable ExecutableElement findSessionGetter(TypeElement type) {
		if ( !hasAnnotation( type, ENTITY, MAPPED_SUPERCLASS, EMBEDDABLE )
				|| isPanacheType( type ) ) {
			for ( ExecutableElement method : methodsIn( type.getEnclosedElements() ) ) {
				if ( isSessionGetter( method ) ) {
					return method;
				}
			}
			final TypeMirror superclass = type.getSuperclass();
			if ( superclass.getKind() == TypeKind.DECLARED ) {
				final DeclaredType declaredType = (DeclaredType) superclass;
				final ExecutableElement ret = findSessionGetter( (TypeElement) declaredType.asElement() );
				if ( ret != null ) {
					return ret;
				}
			}
			for ( TypeMirror superinterface : type.getInterfaces() ) {
				if ( superinterface.getKind() == TypeKind.DECLARED ) {
					final DeclaredType declaredType = (DeclaredType) superinterface;
					final ExecutableElement ret = findSessionGetter( (TypeElement) declaredType.asElement() );
					if ( ret != null ) {
						return ret;
					}
				}
			}
		}
		return null;
	}

	public boolean isStatelessSession() {
		return usingStatelessSession(sessionType);
	}

	public boolean isReactive() {
		return usingReactiveSession(sessionType);
	}

	public boolean isReactiveSessionAccess() {
		return usingReactiveSessionAccess(sessionType);
	}

	public boolean isProvidedSessionAccess() {
		return sessionType.startsWith(SPRING_OBJECT_PROVIDER);
	}

	private boolean isPanacheType(TypeElement type) {
		return context.usesQuarkusOrm() && isOrmPanacheType( type )
			|| context.usesQuarkusReactive() && isReactivePanacheType( type );
	}

	private boolean isOrmPanacheType(TypeElement type) {
		return implementsInterface( type, PANACHE_ORM_REPOSITORY_BASE )
			|| extendsClass( type, PANACHE_ORM_ENTITY_BASE );
	}

	private boolean isReactivePanacheType(TypeElement type) {
		return implementsInterface( type, PANACHE_REACTIVE_REPOSITORY_BASE )
			|| extendsClass( type, PANACHE_REACTIVE_ENTITY_BASE );
	}

	private boolean isPanache2Type(TypeElement type) {
		return implementsInterface( type, PANACHE2_ENTITY_MARKER )
				|| isPanache2Repository( type );
	}

	private boolean isPanache2Repository(TypeElement type) {
		return implementsInterface( type, PANACHE2_MANAGED_BLOCKING_REPOSITORY_BASE )
				|| implementsInterface( type, PANACHE2_STATELESS_BLOCKING_REPOSITORY_BASE )
				|| implementsInterface( type, PANACHE2_MANAGED_REACTIVE_REPOSITORY_BASE )
				|| implementsInterface( type, PANACHE2_STATELESS_REACTIVE_REPOSITORY_BASE )
				;
	}

	/**
	 * If there is a session getter method, we generate an instance
	 * variable backing it, together with a constructor that initializes
	 * it.
	 */
	private String addDaoConstructor(@Nullable ExecutableElement method) {
		final String returnType = method == null ? this.sessionType : fullReturnType( method );
		final String sessionType =
				jakartaDataRepository && springInjection
						? SPRING_OBJECT_PROVIDER + '<' + returnType + '>'
						: returnType;
		final String sessionVariableName = getSessionVariableName( sessionType );
		final String name = method == null ? sessionVariableName : method.getSimpleName().toString();

		if ( method == null || !method.isDefault() ) {
			putMember( name,
					new RepositoryConstructor(
							this,
							getConstructorName(),
							name,
							sessionType,
							sessionVariableName,
							dataStore(),
							context.addInjectAnnotation(),
							context.addNonnullAnnotation(),
							method != null,
							jakartaDataRepository,
							quarkusInjection
					)
			);
		}
		else {
			// use this getter to get the method, do not generate an injection point for its type
			sessionGetter = method.getSimpleName() + "()";
		}
		return sessionType;
	}

	/**
	 * For Quarkus, we generate a constructor with injection for EntityManager in ORM,
	 * and in HR, we define the static session getter.
	 * For Panache 2, we can use the element to figure out what kind of session we want since this
	 * is for repositories
	 */
	private String setupQuarkusDaoConstructor(@Nullable ExecutableElement getter, @Nullable TypeElement element) {
		boolean favorBlocking = context.usesQuarkusOrm()
								|| (context.usesQuarkusPanache2()
									&& element != null
									&& (implementsInterface(element, PANACHE2_MANAGED_BLOCKING_REPOSITORY_BASE)
										|| implementsInterface(element, PANACHE2_STATELESS_BLOCKING_REPOSITORY_BASE)));
		if ( context.usesQuarkusPanache2()
			&& element != null
			&& !implementsInterface(element, PANACHE2_MANAGED_BLOCKING_REPOSITORY_BASE)
			&& !implementsInterface(element, PANACHE2_STATELESS_BLOCKING_REPOSITORY_BASE)
			&& !implementsInterface(element, PANACHE2_MANAGED_REACTIVE_REPOSITORY_BASE)
			&& !implementsInterface(element, PANACHE2_STATELESS_REACTIVE_REPOSITORY_BASE)
			// FIXME: add other default for JD repos?
			) {
			// look for any annotated method, see if they return a Uni
			final List<ExecutableElement> methodsOfClass =
					methodsIn( context.getAllMembers( element ) );
			for ( ExecutableElement method : methodsOfClass ) {
				// trust the first method, no need to look for them all
				if ( containsAnnotation( method, HQL, SQL, JD_QUERY, FIND, JD_FIND ) ) {
					favorBlocking = !isUni( method.getReturnType() );
					break;
				}
			}
		}
		// FIXME: probably go in this branch if we have a getter too?
		if ( favorBlocking ) {
			String name;
			String sessionType;
			if ( getter != null ) {
				name = getter.getSimpleName().toString();
				sessionType = fullReturnType(getter);
			}
			else if(element != null
					&& implementsInterface(element, PANACHE2_STATELESS_BLOCKING_REPOSITORY_BASE)) {
				name = "getStatelessSession";
				sessionType = HIB_STATELESS_SESSION;
			}
			else { // good default
				name = "getSession";
				sessionType = HIB_SESSION;
			}
			putMember( name,
					new RepositoryConstructor(
							this,
							getConstructorName(),
							name,
							sessionType,
							getSessionVariableName( sessionType ),
							dataStore(),
							context.addInjectAnnotation(),
							context.addNonnullAnnotation(),
							false,
							false,
							true
					)
			);
			return sessionType;
		}
		else {
			importType( Constants.QUARKUS_SESSION_OPERATIONS );
			// use this getter to get the method, do not generate an injection point for its type
			if(element != null
					&& implementsInterface(element, PANACHE2_STATELESS_REACTIVE_REPOSITORY_BASE)) {
				sessionGetter = "SessionOperations.getStatelessSession()";
				return UNI_MUTINY_STATELESS_SESSION;
			}
			else {
				sessionGetter = "SessionOperations.getSession()";
				return UNI_MUTINY_SESSION;
			}
		}
	}

	/**
	 * The session getter method doesn't have to be a JavaBeans-style
	 * getter. It can be any method with no parameters and one of the
	 * needed return types.
	 */
	private static boolean isSessionGetter(ExecutableElement method) {
		return method.getParameters().isEmpty()
			&& isSessionGetterType( method.getReturnType() );
	}

	private static boolean isSessionGetterType(TypeMirror returnType) {
		if ( returnType.getKind() == TypeKind.DECLARED ) {
			final DeclaredType declaredType = (DeclaredType) ununi( returnType );
			final Element element = declaredType.asElement();
			if ( element.getKind() == ElementKind.INTERFACE ) {
				final TypeElement typeElement = (TypeElement) element;
				final Name name = typeElement.getQualifiedName();
				if ( name.contentEquals(UNI) || name.contentEquals(SPRING_OBJECT_PROVIDER) ) {
					final var typeArguments = declaredType.getTypeArguments();
					return typeArguments.size() == 1
						&& isSessionGetterType( typeArguments.get( 0 ) );
				}
				else {
					return name.contentEquals(HIB_SESSION)
						|| name.contentEquals(HIB_STATELESS_SESSION)
						|| name.contentEquals(MUTINY_SESSION)
						|| name.contentEquals(MUTINY_STATELESS_SESSION)
						|| name.contentEquals(ENTITY_MANAGER);
				}
			}
			else {
				return false;
			}
		}
		else {
			return false;
		}
	}

	/**
	 * Check if method respects Java Bean conventions for getter and setters.
	 *
	 * @param methodOfClass method element
	 *
	 * @return whether method respects Java Bean conventions.
	 */
	private boolean isGetterOrSetter(Element methodOfClass) {
		final ExecutableType methodType = (ExecutableType) methodOfClass.asType();
		final Name methodSimpleName = methodOfClass.getSimpleName();
		final List<? extends TypeMirror> methodParameterTypes = methodType.getParameterTypes();
		final TypeMirror returnType = methodType.getReturnType();
		return isSetter( methodSimpleName, methodParameterTypes, returnType )
			|| isGetter( methodSimpleName, methodParameterTypes, returnType );
	}

	private static boolean hasPrefix(Name methodSimpleName, String prefix) {
		final int prefixLength = prefix.length();
		if ( methodSimpleName.length() > prefixLength ) {
			for ( int i = 0; i < prefixLength; i++ ) {
				if ( methodSimpleName.charAt(i) != prefix.charAt(i) ) {
					return false;
				}
			}
			return true;
		}
		else {
			return false;
		}
	}

	private static boolean isGetter(
			Name methodSimpleName,
			List<? extends TypeMirror> methodParameterTypes,
			TypeMirror returnType) {
		return ( hasPrefix( methodSimpleName, "get" ) || hasPrefix( methodSimpleName,"is" ) )
			&& methodParameterTypes.isEmpty()
			&& returnType.getKind() != TypeKind.VOID;
	}

	private static boolean isSetter(
			Name methodSimpleName,
			List<? extends TypeMirror> methodParameterTypes,
			TypeMirror returnType) {
		return hasPrefix( methodSimpleName, "set")
			&& methodParameterTypes.size() == 1
			&& returnType.getKind() != TypeKind.VOID;
	}

	private void validatePersistentMembers(List<? extends Element> membersOfClass) {
		for ( Element memberOfClass : membersOfClass ) {
			if ( hasAnnotation(memberOfClass, MANY_TO_ONE, ONE_TO_ONE, ONE_TO_MANY, MANY_TO_MANY) ) {
				validateAssociation(memberOfClass);
			}
		}
	}

	private void addPersistentMembers(List<? extends Element> membersOfClass, AccessType membersKind) {
		for ( Element memberOfClass : membersOfClass ) {
			if ( isPersistent(memberOfClass, membersKind) ) {
				addPersistentMember(memberOfClass);
			}
		}
	}

	private void addPersistentMember(Element memberOfClass) {
		if ( jakartaDataStaticModel ) {
			final DataAnnotationMetaAttribute dataMetaAttribute =
					memberOfClass.asType()
							.accept( new DataMetaAttributeGenerationVisitor(this, context), memberOfClass );
			if ( dataMetaAttribute != null ) {
				final String path = dataMetaAttribute.getPropertyName();
				members.put('_' + path, dataMetaAttribute);
				if ( isEmbedded(memberOfClass) ) {
					final TypeMirror type = attributeType(memberOfClass);
					final DeclaredType declaredType = (DeclaredType) type;
					final TypeElement typeElement = (TypeElement) declaredType.asElement();
					for ( Element field : fieldsIn( typeElement.getEnclosedElements() ) ) {
						addEmbeddablePersistentMember(field, path, AccessType.FIELD);
					}
					for ( Element method : methodsIn( typeElement.getEnclosedElements() ) ) {
						if ( isGetterOrSetter(method) ) {
							addEmbeddablePersistentMember(method, path, AccessType.PROPERTY);
						}
					}
				}
			}
		}
		else {
			final AnnotationMetaAttribute jpaMetaAttribute =
					memberOfClass.asType()
							.accept( new MetaAttributeGenerationVisitor( this, context ), memberOfClass);
			if ( jpaMetaAttribute != null ) {
				members.put( jpaMetaAttribute.getPropertyName(), jpaMetaAttribute );
			}
		}
	}

	private void addEmbeddablePersistentMember(Element memberOfEmbeddable, String path, AccessType membersKind) {
		if ( isPersistent(memberOfEmbeddable, membersKind) ) { //TODO respect AccessType of embeddable
			final DataAnnotationMetaAttribute metaAttribute =
					memberOfEmbeddable.asType()
							.accept( new DataMetaAttributeGenerationVisitor(this, path, context),
									memberOfEmbeddable );
			if (metaAttribute != null) {
				members.put('_' + metaAttribute.getPropertyName(),
						metaAttribute);
			}
		}
	}

	static boolean isEmbedded(Element memberOfClass) {
		if ( hasAnnotation(memberOfClass, EMBEDDED) ) {
			return true;
		}
		else {
			final TypeMirror type = attributeType(memberOfClass);
			if ( type.getKind() == TypeKind.DECLARED ) {
				final DeclaredType declaredType = (DeclaredType) type;
				return hasAnnotation( declaredType.asElement(), EMBEDDABLE );
			}
			else {
				return false;
			}
		}
	}

	private void validateAssociation(Element memberOfClass) {
		final TypeMirror type = attributeType(memberOfClass);
		if ( hasAnnotation(memberOfClass, MANY_TO_ONE) ) {
			final AnnotationMirror annotation =
					castNonNull(getAnnotationMirror(memberOfClass, MANY_TO_ONE));
			validateToOneAssociation(memberOfClass, annotation, type);
		}
		else if ( hasAnnotation(memberOfClass, ONE_TO_ONE) ) {
			final AnnotationMirror annotation =
					castNonNull(getAnnotationMirror(memberOfClass, ONE_TO_ONE));
			validateToOneAssociation(memberOfClass, annotation, type);
		}
		else if ( hasAnnotation(memberOfClass, ONE_TO_MANY) ) {
			final AnnotationMirror annotation =
					castNonNull(getAnnotationMirror(memberOfClass, ONE_TO_MANY));
			validateToManyAssociation(memberOfClass, annotation, type);
		}
		else if ( hasAnnotation(memberOfClass, MANY_TO_MANY) ) {
			final AnnotationMirror annotation =
					castNonNull(getAnnotationMirror(memberOfClass, MANY_TO_MANY));
			validateToManyAssociation(memberOfClass, annotation, type);
		}
	}

	private static TypeMirror attributeType(Element memberOfClass) {
		switch ( memberOfClass.getKind() ) {
			case METHOD:
				final ExecutableElement method = (ExecutableElement) memberOfClass;
				return method.getReturnType();
			case FIELD:
				return memberOfClass.asType();
			default:
				throw new AssertionFailure("should be a field or getter");
		}
	}

	private void validateToOneAssociation(Element memberOfClass, AnnotationMirror annotation, TypeMirror type) {
		final AnnotationValue target = getAnnotationValue(annotation, "targetEntity");
		validateAssociation(memberOfClass, annotation, target == null ? type : (TypeMirror) target.getValue());
	}

	private void validateToManyAssociation(Element memberOfClass, AnnotationMirror annotation, TypeMirror type) {
		final AnnotationValue target = getAnnotationValue(annotation, "targetEntity");
		validateAssociation(memberOfClass, annotation, target == null ? elementType(type) : (TypeMirror) target.getValue());
	}

	private void validateAssociation(Element memberOfClass, AnnotationMirror annotation, @Nullable TypeMirror typeMirror) {
		if ( typeMirror != null ) {
			switch ( typeMirror.getKind() ) {
				case TYPEVAR:
					if ( hasAnnotation(element, ENTITY) ) {
						message(memberOfClass, "type '" + typeMirror + "' is a type variable",
								Diagnostic.Kind.WARNING);
					}
					break;
				case DECLARED:
					final DeclaredType assocDeclaredType = (DeclaredType) typeMirror;
					final TypeElement assocTypeElement = (TypeElement) assocDeclaredType.asElement();
					if ( hasAnnotation(assocTypeElement, ENTITY) ) {
						final AnnotationValue mappedBy = getAnnotationValue(annotation, "mappedBy");
						if ( mappedBy != null ) {
							validateBidirectionalMapping(memberOfClass, annotation, mappedBy, assocTypeElement);
						}
					}
					else {
						message(memberOfClass, "type '" + assocTypeElement.getSimpleName()
										+ "' is not annotated '@Entity'",
								Diagnostic.Kind.WARNING);
					}
					break;
				default:
					message(memberOfClass, "type '" + typeMirror + "' is not an entity type",
							Diagnostic.Kind.WARNING);
			}
		}
	}

	private void validateBidirectionalMapping(
			Element memberOfClass, AnnotationMirror annotation, AnnotationValue annotationVal, TypeElement assocTypeElement) {
		final String mappedBy = annotationVal.getValue().toString();
		if ( mappedBy != null && !mappedBy.isEmpty()
				// this happens for a typesafe ref, e.g. Page_BOOK
				// TODO: we should queue it to validate it later somehow
				&& !mappedBy.equals( "<error>" ) ) {
			if ( mappedBy.indexOf( '.' ) > 0 ) {
				//we don't know how to handle paths yet
				return;
			}
			for ( Element member : context.getAllMembers( assocTypeElement ) ) {
				if ( propertyName( member ).contentEquals( mappedBy )
						&& compatibleAccess( assocTypeElement, member ) ) {
					validateBackRef( memberOfClass, annotation, assocTypeElement, member, annotationVal );
					return;
				}
			}
			// not found
			message( memberOfClass, annotation, annotationVal,
					"no matching member in '" + assocTypeElement.getSimpleName() + "'",
					Diagnostic.Kind.ERROR );
		}
	}

	private boolean compatibleAccess(TypeElement assocTypeElement, Element member) {
		final AccessType memberAccessType = determineAnnotationSpecifiedAccessType( member );
		final AccessType accessType = memberAccessType == null ? getAccessType(assocTypeElement) : memberAccessType;
		return switch ( member.getKind() ) {
			case FIELD -> accessType == AccessType.FIELD;
			case METHOD -> accessType == AccessType.PROPERTY;
			default -> false;
		};
	}

	private void validateBackRef(
			Element memberOfClass,
			AnnotationMirror annotation,
			TypeElement assocTypeElement,
			Element referencedMember,
			AnnotationValue annotationVal) {
		final TypeMirror backType;
		final String expectedMappingAnnotation;
		switch ( annotation.getAnnotationType().asElement().toString() ) {
			case ONE_TO_ONE:
				backType = attributeType(referencedMember);
				expectedMappingAnnotation = ONE_TO_ONE;
				break;
			case ONE_TO_MANY:
				backType = attributeType(referencedMember);
				expectedMappingAnnotation = MANY_TO_ONE;
				break;
			case MANY_TO_MANY:
				backType = elementType( attributeType(referencedMember) );
				expectedMappingAnnotation = MANY_TO_MANY;
				break;
			default:
				throw new AssertionFailure("should not have a mappedBy");
		}
		if ( backType != null ) {
			final Element idMember = getIdMember();
			final Types typeUtils = context.getTypeUtils();
			if ( idMember != null && typeUtils.isSameType( backType, idMember.asType() ) ) {
				// mappedBy references a regular field of the same type as the entity id
				//TODO: any other validation to do here??
			}
			else if ( typeUtils.isSameType( backType, element.asType() ) ) {
				// mappedBy references a field of the same type as the entity
				// it needs to be mapped as the appropriate sort of association
				if ( !hasAnnotation( referencedMember, expectedMappingAnnotation ) ) {
					message(memberOfClass, annotation, annotationVal,
							"member '" + referencedMember.getSimpleName()
									+ "' of '" + assocTypeElement.getSimpleName()
									+ "' is not annotated '@" + unqualify(expectedMappingAnnotation) + "'",
							Diagnostic.Kind.WARNING);
				}
			}
			else {
				// mappedBy references a field which seems to be of the wrong type
				message( memberOfClass, annotation, annotationVal,
						"member '" + referencedMember.getSimpleName()
								+ "' of '" + assocTypeElement.getSimpleName()
								+ "' is not of type '" + element.getSimpleName() + "'",
						Diagnostic.Kind.WARNING );
			}
		}
	}

	private @Nullable Element getIdMember() {
		for ( Element e : element.getEnclosedElements() ) {
			if ( hasAnnotation( e, ID, EMBEDDED_ID ) ) {
				return e;
			}
		}
		return null;
	}

	private boolean isPersistent(Element memberOfClass, AccessType membersKind) {
		return ( entityAccessTypeInfo.getAccessType() == membersKind
					|| determineAnnotationSpecifiedAccessType( memberOfClass ) != null )
			&& !containsAnnotation( memberOfClass, TRANSIENT )
			&& !memberOfClass.getModifiers().contains( Modifier.TRANSIENT )
			&& !memberOfClass.getModifiers().contains( Modifier.STATIC )
			&& !( memberOfClass.getKind() == ElementKind.METHOD
				&& isSessionGetter( (ExecutableElement) memberOfClass ) );
	}

	private void addLifecycleMethods(List<ExecutableElement> queryMethods) {
		for ( ExecutableElement method : queryMethods) {
			if ( method.getModifiers().contains(Modifier.ABSTRACT) ) {
				addLifecycleMethod( method );
			}
		}
	}

	private boolean isDeleteLifecycle(ExecutableElement method) {
		if ( method.getParameters().size() == 1 ) {
			final VariableElement parameter = method.getParameters().get(0);
			final DeclaredType declaredType = entityType( parameterType(parameter) );
			return declaredType != null
				&& containsAnnotation( declaredType.asElement(), ENTITY );

			}
		else {
			return false;
		}
	}

	private void addQueryMethods(List<ExecutableElement> queryMethods) {
		for ( ExecutableElement method : queryMethods) {
			final Set<Modifier> modifiers = method.getModifiers();
			if ( modifiers.contains(Modifier.ABSTRACT)
					|| modifiers.contains(Modifier.NATIVE) ) {
				addQueryMethod( method );
			}
		}
	}

	private void addQueryMethod(ExecutableElement method) {
		final TypeMirror returnType = memberMethodType(method).getReturnType();
		final TypeKind kind = returnType.getKind();
		if ( kind == TypeKind.VOID || kind == TypeKind.ARRAY || kind.isPrimitive() ) {
			addQueryMethod( method, returnType, null );
		}
		else if ( kind == TypeKind.DECLARED ) {
			final DeclaredType declaredType = (DeclaredType) unUniIfPossible(method, returnType);
			final TypeElement typeElement = (TypeElement) declaredType.asElement();
			final List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
			switch ( typeArguments.size() ) {
				case 0:
					if ( containsAnnotation( declaredType.asElement(), ENTITY ) ) {
						addQueryMethod( method, declaredType, null );
					}
					else {
						if ( isLegalRawResultType( typeElement.getQualifiedName().toString() ) ) {
							addQueryMethod( method, null, typeElement );
						}
						else {
							// probably a projection
							addQueryMethod( method, declaredType, null );
						}
					}
					break;
				case 1:
					if ( validatedQueryReturnType( method, typeElement ) ) {
						addQueryMethod( method, typeArguments.get(0), typeElement );
					}
					break;
				default:
					message( method,
							"incorrect return type '" + declaredType + "'",
							Diagnostic.Kind.ERROR );
					break;
			}
		}
	}

	private TypeMirror unUniIfPossible(ExecutableElement method, TypeMirror returnType) {
		final TypeMirror result = ununi( returnType );
		if ( repository ) {
			if ( isReactive() )  {
				if ( result == returnType ) {
					message( method, "backed by a reactive session, must return 'Uni'", Diagnostic.Kind.ERROR );
				}
			}
			else {
				if ( result != returnType ) {
					message( method, "not backed by a reactive session, must not return 'Uni'", Diagnostic.Kind.ERROR );
				}
			}
		}
		return result;
	}

	private boolean validatedQueryReturnType(ExecutableElement method, TypeElement typeElement) {
		final String typeName = typeElement.getQualifiedName().toString();
		switch ( typeName ) {
			case JD_PAGE:
				if ( !hasPageRequest(method) ) {
					message(method,
							"method with return type '" + typeName
									+ "' has no parameter of type 'PageRequest'",
							Diagnostic.Kind.ERROR);
					return false;
				}
				else {
					return true;
				}
			case JD_CURSORED_PAGE:
				if ( !hasPageRequest(method) ) {
					message(method,
							"method with return type '" + typeName
									+ "' has no parameter of type 'PageRequest'",
							Diagnostic.Kind.ERROR);
					return false;
				}
				else if ( !hasOrder(method) ) {
					message(method,
							"method with return type '" + typeName
									+ "' has no parameter of type 'Order' or 'Sort' and no '@OrderBy' annotation",
							Diagnostic.Kind.ERROR);
					return false;
				}
				else {
					return true;
				}
			case HIB_KEYED_RESULT_LIST:
				if ( !hashKeyedPage(method) ) {
					message(method,
							"method with return type '" + typeName
									+ "' has no parameter of type 'KeyedPage'",
							Diagnostic.Kind.ERROR);
					return false;
				}
				else {
					return true;
				}
			default:
				if ( isLegalGenericResultType(typeName) ) {
					return true;
				}
				else {
					message(method,
							"incorrect return type '" + typeName + "'",
							Diagnostic.Kind.ERROR);
					return false;
				}
		}
	}

	private static boolean hashKeyedPage(ExecutableElement method) {
		return method.getParameters().stream()
				.anyMatch(param -> typeNameEquals(param.asType(), HIB_KEYED_PAGE));
	}

	private static boolean hasPageRequest(ExecutableElement method) {
		return method.getParameters().stream()
				.anyMatch(param -> typeNameEquals(param.asType(), JD_PAGE_REQUEST));
	}

	private static boolean hasOrder(ExecutableElement method) {
		return hasAnnotation(method, JD_ORDER_BY, JD_ORDER_BY_LIST)
			|| method.getParameters().stream()
				.anyMatch(param -> typeNameEquals(param.asType(), JD_ORDER)
						|| typeNameEquals(param.asType(), JD_SORT)
						|| typeNameEqualsArray(param.asType(), JD_SORT));
	}

	private static TypeMirror ununi(TypeMirror returnType) {
		if ( returnType.getKind() == TypeKind.DECLARED ) {
			final DeclaredType declaredType = (DeclaredType) returnType;
			final TypeElement typeElement = (TypeElement) declaredType.asElement();
			if ( typeElement.getQualifiedName().contentEquals(Constants.UNI) ) {
				return declaredType.getTypeArguments().get(0);
			}
		}
		return returnType;
	}

	private static boolean isUni (TypeMirror returnType){
		if ( returnType.getKind() == TypeKind.DECLARED ) {
			final DeclaredType declaredType = (DeclaredType) returnType;
			final TypeElement typeElement = (TypeElement) declaredType.asElement();
			return typeElement.getQualifiedName().contentEquals( Constants.UNI );
		}
		return false;
	}

	private static boolean isLegalRawResultType(String containerTypeName) {
		return LEGAL_RAW_RESULT_TYPES.contains( containerTypeName );
	}

	private static boolean isLegalGenericResultType(String containerTypeName) {
		return LEGAL_GENERIC_RESULT_TYPES.contains( containerTypeName );
	}

	private static final Set<String> LEGAL_RAW_RESULT_TYPES
			= Set.of(LIST, QUERY, HIB_QUERY);

	private static final Set<String> LEGAL_GENERIC_RESULT_TYPES
			= Set.of(LIST, STREAM, OPTIONAL,
					TYPED_QUERY, HIB_QUERY, HIB_SELECTION_QUERY, HIB_KEYED_RESULT_LIST,
					JD_PAGE, JD_CURSORED_PAGE);

	private void addQueryMethod(
			ExecutableElement method,
			@Nullable TypeMirror returnType,
			@Nullable TypeElement containerType) {
		final AnnotationMirror hql = getAnnotationMirror( method, HQL );
		if ( hql != null ) {
			addQueryMethod( method, returnType, containerType, hql, false );
		}
		final AnnotationMirror sql = getAnnotationMirror( method, SQL );
		if ( sql != null ) {
			addQueryMethod( method, returnType, containerType, sql, true );
		}
		final AnnotationMirror jdql = getAnnotationMirror( method, JD_QUERY );
		if ( jdql != null ) {
			addQueryMethod( method, returnType, containerType, jdql, false );
		}
		if ( hasAnnotation( method, FIND, JD_FIND ) ) {
			addFinderMethod( method, returnType, containerType );
		}
		else if ( hasAnnotation( method, JD_DELETE ) ) {
			addDeleteMethod( method, returnType );
		}
	}

	private void addDeleteMethod(ExecutableElement method, @Nullable TypeMirror returnType) {
		if ( returnType != null ) {
			if ( isReactive()
					? isLegalReactiveDeleteReturnType(returnType)
					: isLegalDeleteReturnType(returnType) ) {
				createCriteriaDelete(method);
			}
			else {
				message(method,
						isReactive()
								? "must be 'Uni<Void>' or 'Uni<Integer>'"
								: "must be 'void' or return 'int' or 'long'",
						Diagnostic.Kind.ERROR);
			}
		}
	}

	private static boolean isLegalDeleteReturnType(TypeMirror returnType) {
		final TypeKind kind = returnType.getKind();
		return kind == TypeKind.VOID
			|| kind == TypeKind.INT
			|| kind == TypeKind.LONG;
	}

	private static boolean isLegalReactiveDeleteReturnType(TypeMirror returnType) {
		final TypeKind kind = returnType.getKind();
		if ( kind == TypeKind.DECLARED ) {
			final DeclaredType type = (DeclaredType) returnType;
			final TypeElement typeElement = (TypeElement) type.asElement();
			final Name name = typeElement.getQualifiedName();
			return name.contentEquals( Void.class.getName() )
				|| name.contentEquals( Integer.class.getName() );
		}
		else {
			return false;
		}
	}

	private void addLifecycleMethod(ExecutableElement method) {
		final TypeMirror returnType = unUniIfPossible( method, method.getReturnType() );
		if ( method.getParameters().size() != 1 ) {
			message( method,
					"must have exactly one parameter",
					Diagnostic.Kind.ERROR );

		}
		else if ( returnType == null ) {
			message( method,
					"must be declared 'void'",
					Diagnostic.Kind.ERROR );
		}
		else {
			final boolean returnArgument = !isVoid(returnType);
			final String operation = lifecycleOperation( method );
			final VariableElement parameter = method.getParameters().get(0);
			final TypeMirror declaredParameterType = parameter.asType();
			final TypeMirror parameterType = parameterType( parameter );
			final DeclaredType declaredType = entityType( parameterType );
			if ( declaredType == null ) {
				message( parameter,
						"incorrect parameter type '" + parameterType + "' is not an entity type",
						Diagnostic.Kind.ERROR );
			}
			else if ( !hasAnnotation( declaredType.asElement(), ENTITY )
					// TODO: improve this (carefully consider the case of an erased type variable)
					&& declaredParameterType == parameterType ) {
				message( parameter,
						"incorrect parameter type '" + parameterType + "' is not annotated '@Entity'",
						Diagnostic.Kind.ERROR );
			}
			else if ( returnArgument
					&& !isSameType( returnType, declaredParameterType ) ) {
				message( parameter,
						"return type '" + returnType
								+ "' disagrees with parameter type '" + parameterType + "'",
						Diagnostic.Kind.ERROR );
			}
			else {
				final String entity = typeAsString(parameterType);
				final String methodName = method.getSimpleName().toString();
				putMember(
						methodName + '.' + entity,
						new LifecycleMethod(
								this, method,
								entity,
								typeAsString(lifecycleParameterArgument(parameterType)),
								methodName,
								parameter.getSimpleName().toString(),
								getSessionVariableName(),
								sessionType,
								operation,
								context.addNonnullAnnotation(),
								lifecycleParameterKind(parameterType),
								returnArgument,
								hasGeneratedId(declaredType)
						)
				);
			}
		}
	}

	private boolean hasGeneratedId(DeclaredType entityType)  {
		final TypeElement typeElement = (TypeElement) entityType.asElement();
		for ( Element member : context.getAllMembers(typeElement) ) {
			if ( hasAnnotation(member, GENERATED_VALUE)
					&& hasAnnotation(member, ID) ) {
				return true;
			}
			//TODO: look for generator annotations
		}
		return false;
	}

	private static LifecycleMethod.ParameterKind lifecycleParameterKind(TypeMirror parameterType) {
		switch (parameterType.getKind()) {
			case ARRAY:
				return LifecycleMethod.ParameterKind.ARRAY;
			case DECLARED:
				final DeclaredType declaredType = (DeclaredType) parameterType;
				final TypeElement typeElement = (TypeElement) declaredType.asElement();
				return typeElement.getQualifiedName().contentEquals(LIST)
						? LifecycleMethod.ParameterKind.LIST
						: LifecycleMethod.ParameterKind.NORMAL;
			default:
				return LifecycleMethod.ParameterKind.NORMAL;
		}
	}

	private TypeMirror lifecycleParameterArgument(TypeMirror parameterType) {
		switch (parameterType.getKind()) {
			case ARRAY:
				final ArrayType arrayType = (ArrayType) parameterType;
				return arrayType.getComponentType();
			case DECLARED:
				final DeclaredType declaredType = (DeclaredType) parameterType;
				final TypeElement typeElement = (TypeElement) declaredType.asElement();
				return typeElement.getQualifiedName().contentEquals(LIST)
					&& !declaredType.getTypeArguments().isEmpty()
						? declaredType.getTypeArguments().get(0)
						: context.getElementUtils().getTypeElement(JAVA_OBJECT).asType();
			default:
				return parameterType;
		}
	}

	private static boolean isVoid(TypeMirror returnType) {
		switch (returnType.getKind()) {
			case VOID:
				return true;
			case DECLARED:
				final DeclaredType declaredType = (DeclaredType) returnType;
				final TypeElement typeElement = (TypeElement) declaredType.asElement();
				return typeElement.getQualifiedName().contentEquals(VOID);
			default:
				return false;
		}
	}

	private @Nullable DeclaredType entityType(TypeMirror parameterType) {
		final Types types = context.getTypeUtils();
		switch ( parameterType.getKind() ) {
			case TYPEVAR:
				final TypeVariable typeVariable = (TypeVariable) parameterType;
				parameterType = typeVariable.getUpperBound();
				//INTENTIONAL FALL THROUGH
			case DECLARED:
				final DeclaredType declaredType = (DeclaredType) parameterType;
				final TypeElement typeElement = (TypeElement) declaredType.asElement();
				if ( typeElement.getQualifiedName().contentEquals(LIST)
						&& !declaredType.getTypeArguments().isEmpty() ) {
					final TypeMirror elementType = types.erasure( declaredType.getTypeArguments().get(0) );
					return elementType.getKind() == TypeKind.DECLARED ? (DeclaredType) elementType : null;
				}
				else {
					return declaredType;
				}
			case ARRAY:
				final ArrayType arrayType = (ArrayType) parameterType;
				final TypeMirror componentType = types.erasure( arrayType.getComponentType() );
				return componentType.getKind() == TypeKind.DECLARED ? (DeclaredType) componentType : null;
			default:
				return null;
		}
	}

	private @Nullable TypeMirror elementType(TypeMirror parameterType) {
		switch ( parameterType.getKind() ) {
			case DECLARED:
				final DeclaredType declaredType = (DeclaredType) parameterType;
				final List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
				return switch ( typeArguments.size() ) {
					case 1 -> typeArguments.get(0);
					case 2 -> typeArguments.get(1);
					default -> null;
				};
			case ARRAY:
				final ArrayType arrayType = (ArrayType) parameterType;
				return arrayType.getComponentType();
			default:
				return null;
		}
	}

	private static String lifecycleOperation(ExecutableElement method) {
		if ( hasAnnotation(method, JD_INSERT) ) {
			return "insert";
		}
		else if ( hasAnnotation(method, JD_UPDATE) ) {
			return "update";
		}
		else if ( hasAnnotation(method, JD_DELETE) ) {
			return "delete";
		}
		else if ( hasAnnotation(method, JD_SAVE) ) {
			return "upsert";
		}
		else {
			throw new AssertionFailure("Unrecognized lifecycle operation");
		}
	}

	private void addFinderMethod(
			ExecutableElement method,
			@Nullable TypeMirror returnType,
			@Nullable TypeElement containerType) {
		if ( returnType == null ) {
			message( method,
					"missing return type",
					Diagnostic.Kind.ERROR );
		}
		else if ( returnType.getKind() == TypeKind.ARRAY ) {
			final ArrayType arrayType = (ArrayType) returnType;
			final TypeMirror componentType = arrayType.getComponentType();
			if ( componentType.getKind() != TypeKind.DECLARED ) {
				message( method,
						"incorrect return type '" + returnType + "' is not an array with entity elements",
						Diagnostic.Kind.ERROR );
			}
			else {
				final DeclaredType declaredType = (DeclaredType) componentType;
				final TypeElement entity = (TypeElement) declaredType.asElement();
				if ( !containsAnnotation( entity, ENTITY ) ) {
					message( method,
							"incorrect return type '" + returnType + "' is not annotated '@Entity'",
							Diagnostic.Kind.ERROR );
				}
				else {
					// multiple results, it has to be a criteria finder
					createCriteriaFinder( method, arrayType.getComponentType(), "[]", entity );
				}
			}
		}
		else if ( returnType.getKind() == TypeKind.DECLARED ) {
			final DeclaredType declaredType = (DeclaredType) returnType;
			final TypeElement entity = (TypeElement) declaredType.asElement();
			if ( !containsAnnotation( entity, ENTITY ) ) {
				message( method,
						"incorrect return type '" + declaredType + "' is not annotated '@Entity'",
						Diagnostic.Kind.ERROR );
			}
			else {
				if ( containerType != null
						&& !containerType.getQualifiedName().contentEquals(OPTIONAL) ) {
					// multiple results, so it has to be a criteria finder
					createCriteriaFinder( method, declaredType, containerType.toString(), entity );
				}
				else {
					for ( VariableElement parameter : method.getParameters() ) {
						final String type = parameter.asType().toString();
						if ( isPageParam(type) ) {
							message( parameter, "pagination would have no effect", Diagnostic.Kind.ERROR);
						}
						else if ( isOrderParam(type) ) {
							message( parameter, "ordering would have no effect", Diagnostic.Kind.ERROR);
						}
					}
					final String containerTypeName = containerType==null ? null : OPTIONAL;
					final long parameterCount =
							method.getParameters().stream()
									.filter(AnnotationMetaEntity::isFinderParameterMappingToAttribute)
									.count();
					switch ( (int) parameterCount ) {
						case 0:
							message( method, "missing parameter", Diagnostic.Kind.ERROR );
							break;
						case 1:
							createSingleParameterFinder( method, declaredType, entity, containerTypeName );
							break;
						default:
							createMultipleParameterFinder( method, declaredType, entity, containerTypeName );
					}
				}
			}
		}
		else {
			message( method,
					"incorrect return type '" + returnType + "' is not an entity type",
					Diagnostic.Kind.ERROR );
		}
	}

	/**
	 * Create a finder method which returns multiple results.
	 */
	private void createCriteriaFinder(
			ExecutableElement method, TypeMirror returnType, @Nullable String containerType, TypeElement entity) {
		final String methodName = method.getSimpleName().toString();
		final List<String> paramNames = parameterNames( method, entity );
		final List<String> paramTypes = parameterTypes( method );
		final List<Boolean> paramPatterns = parameterPatterns( method );
		final String[] sessionType = sessionTypeFromParameters( paramNames, paramTypes );
		final String methodKey = methodName + paramTypes;
		final List<Boolean> multivalued = new ArrayList<>();
		for ( VariableElement parameter : method.getParameters() ) {
			if ( isFinderParameterMappingToAttribute( parameter ) ) {
				multivalued.add( validateFinderParameter( entity, parameter ) == FieldType.MULTIVALUED );
			}
			else {
				multivalued.add( false );
				checkFinderParameter(entity, parameter);
			}
		}
		warnAboutMissingOrder( method );
		putMember( methodKey,
				new CriteriaFinderMethod(
						this, method,
						methodName,
						returnType.toString(),
						containerType,
						paramNames,
						paramTypes,
						parameterNullability(method, entity),
						multivalued,
						paramPatterns,
						repository,
						sessionType[0],
						sessionType[1],
						enabledFetchProfiles( method ),
						orderByList( method, entity ),
						context.addNonnullAnnotation(),
						jakartaDataRepository,
						fullReturnType(method),
						hasAnnotation(method, NULLABLE)
				)
		);
	}

	private void checkFinderParameter(@Nullable TypeElement entityType, VariableElement parameter) {
		final Types types = context.getTypeUtils();
		final TypeMirror parameterType = parameterType(parameter);
		final String typeName = parameterType.toString();
		// TODO: we could allow restrictions even when the query returns a projection,
		//       but this would require some sort of change to Hibernate core, perhaps
		//       adding <T> SelectionQuery<T> setProjection(Class<T> recordType)
//		if ( isRestrictionParam( typeName ) ) {
//			final TypeMirror typeArgument = getTypeArgument( parameterType );
//			final TypeElement implicitEntityType = entityType == null ? primaryEntity : entityType;
//			if ( implicitEntityType != null ) {
//				if ( typeArgument == null ) {
//					missingTypeArgError( implicitEntityType.getSimpleName().toString(), parameter, typeName );
//				}
//				else if ( !types.isSameType( typeArgument, implicitEntityType.asType() ) ) {
//					wrongTypeArgError( implicitEntityType.getSimpleName().toString(), parameter, typeName );
//				}
//			}
//		}
		if ( isOrderParam( typeName ) || isRestrictionParam( typeName ) ) {
			final TypeMirror typeArgument = getTypeArgument( parameterType );
			if ( entityType != null ) {
				if ( typeArgument == null ) {
					missingTypeArgError( entityType, parameter, typeName );
				}
				else if ( !types.isSameType( typeArgument, entityType.asType() ) ) {
					wrongTypeArgError( entityType, parameter, typeName );
				}
			}
			else {
				message( parameter, "repository method does not return entity type", Diagnostic.Kind.ERROR );
			}
		}
	}

	private void createCriteriaDelete(ExecutableElement method) {
		final TypeElement entity = primaryEntity;
		if ( entity == null) {
			message( method, "repository does not have a well-defined primary entity type",
					Diagnostic.Kind.ERROR);
		}
		else {
			final String methodName = method.getSimpleName().toString();
			final List<String> paramNames = parameterNames( method, entity );
			final List<String> paramTypes = parameterTypes( method );
			final List<Boolean> paramPatterns = parameterPatterns( method );
			final String[] sessionType = sessionTypeFromParameters( paramNames, paramTypes );
			final String methodKey = methodName + paramTypes;
			final List<Boolean> multivalued = new ArrayList<>();
			for ( VariableElement parameter : method.getParameters() ) {
				if ( isFinderParameterMappingToAttribute( parameter ) ) {
					multivalued.add( validateFinderParameter( entity, parameter ) == FieldType.MULTIVALUED );
				}
				else {
					multivalued.add( false );
				}
			}
			putMember( methodKey,
					new CriteriaDeleteMethod(
							this, method,
							methodName,
							entity.getQualifiedName().toString(),
							paramNames,
							paramTypes,
							parameterNullability(method, entity),
							multivalued,
							paramPatterns,
							repository,
							sessionType[0],
							sessionType[1],
							context.addNonnullAnnotation(),
							jakartaDataRepository,
							fullReturnType(method)
					)
			);
		}
	}

	private void wrongTypeArgError(TypeElement entityType, VariableElement parameter, String parameterType) {
		message(parameter, "mismatched type of " + message(parameterType, entityType),
				Diagnostic.Kind.ERROR );
	}

	private void missingTypeArgError(TypeElement entityType, VariableElement parameter, String parameterType) {
		message(parameter, "missing type of " + message(parameterType, entityType),
				Diagnostic.Kind.ERROR );
	}

	private String message(String parameterType, TypeElement entityType) {
		final String entityTypeName = entityType.getSimpleName().toString();
		if (parameterType.startsWith(HIB_ORDER) || parameterType.startsWith(JD_ORDER)) {
			return "order (should be 'Order<? super " + entityTypeName + ">')";
		}
		else if (parameterType.startsWith(LIST + "<" + HIB_ORDER)) {
			return "order (should be 'List<Order<? super " + entityTypeName + ">>')";
		}
		else if (parameterType.startsWith(HIB_RESTRICTION)) {
			return "restriction (should be 'Restriction<? super " + entityTypeName + ">')";
		}
		else if (parameterType.startsWith(LIST + "<" + HIB_RESTRICTION)) {
			return "restriction (should be 'List<Restriction<? super " + entityTypeName + ">>')";
		}
		else if (parameterType.startsWith(JD_SORT)) {
			return "sort (should be 'Sort<? super " + entityTypeName + ">')";
		}
		else {
			return "parameter";
		}
	}

	private List<OrderBy> orderByList(ExecutableElement method, TypeElement returnType) {
		final TypeElement entityType = implicitEntityType( returnType );
		if ( entityType != null ) {
			final AnnotationMirror orderByList = getAnnotationMirror( method, JD_ORDER_BY_LIST );
			if ( orderByList != null ) {
				final List<OrderBy> result = new ArrayList<>();
				@SuppressWarnings("unchecked")
				final List<AnnotationValue> list = (List<AnnotationValue>)
						castNonNull( getAnnotationValue( orderByList ) ).getValue();
				for ( AnnotationValue element : list ) {
					result.add( orderByExpression( castNonNull( (AnnotationMirror) element.getValue() ), entityType, method ) );
				}
				return result;
			}
			final AnnotationMirror orderBy = getAnnotationMirror( method, JD_ORDER_BY );
			if ( orderBy != null ) {
				return List.of( orderByExpression(orderBy, entityType, method) );
			}
		}
		return emptyList();
	}

	private OrderBy orderByExpression(AnnotationMirror orderBy, TypeElement entityType, ExecutableElement method) {
		final String fieldName = castNonNull( getAnnotationValue(orderBy) ).getValue().toString();
		if ( fieldName.equals("<error>") ) {
			throw new ProcessLaterException();
		}
		final AnnotationValue descendingOrNull = getAnnotationValue(orderBy, "descending");
		final AnnotationValue ignoreCaseOrNull = getAnnotationValue(orderBy, "ignoreCase");
		final boolean descending = descendingOrNull != null && (Boolean) descendingOrNull.getValue();
		final boolean ignoreCase = ignoreCaseOrNull != null && (Boolean) ignoreCaseOrNull.getValue();
		final String path = fieldName
				.replace('$', '.')
				.replace('_', '.'); //Jakarta Data allows _ here
		if ( memberMatchingPath( entityType, path ) == null ) {
			message( method, orderBy,
					"no matching field named '" + fieldName
							+ "' in entity class '" + entityType.getQualifiedName() + "'",
					Diagnostic.Kind.ERROR );
		}
		return new OrderBy( path, descending, ignoreCase );
	}

	private static @Nullable TypeMirror getTypeArgument(TypeMirror parameterType) {
		switch ( parameterType.getKind() ) {
			case ARRAY:
				final ArrayType arrayType = (ArrayType) parameterType;
				return getTypeArgument( arrayType.getComponentType() );
			case DECLARED:
				final DeclaredType type = (DeclaredType) parameterType;
				switch ( typeName(parameterType) ) {
					case LIST:
						for (TypeMirror arg : type.getTypeArguments()) {
							return getTypeArgument( arg );
						}
						return null;
					case HIB_ORDER:
					case HIB_RESTRICTION:
					case JD_SORT:
					case JD_ORDER:
						for ( TypeMirror arg : type.getTypeArguments() ) {
							switch ( arg.getKind() ) {
								case WILDCARD:
									return ((WildcardType) arg).getSuperBound();
								case ARRAY:
								case DECLARED:
								case TYPEVAR:
									return arg;
								default:
									return null;
							}
						}
						return null;
					default:
						return null;
				}
			default:
				return null;
		}
	}

	private static boolean isFinderParameterMappingToAttribute(VariableElement parameter) {
		final String typeName = parameter.asType().toString();
		return !isSpecialParam(typeName)
			|| isRangeParam(typeName);
	}

	private String[] sessionTypeFromParameters(List<String> paramNames, List<String> paramTypes) {
		for ( int i = 0; i < paramNames.size(); i ++ ) {
			final String type = paramTypes.get(i);
			if ( isSessionParameter(type) ) {
				final String name = paramNames.get(i);
				return new String[] { type, name };
			}
		}
		return new String[] { getSessionType(), getSessionVariableName() };
	}

	@Override
	protected String getSessionVariableName() {
		return getSessionVariableName(sessionType);
	}

	private String getSessionVariableName(String sessionType) {
		return switch (sessionType) {
			case HIB_SESSION, HIB_STATELESS_SESSION,
				MUTINY_SESSION, MUTINY_STATELESS_SESSION,
				SPRING_STATELESS_SESSION_PROVIDER -> "session";
//			case UNI_MUTINY_SESSION, UNI_MUTINY_STATELESS_SESSION -> "session";
			default -> sessionGetter;
		};
	}

	private static List<String> enabledFetchProfiles(ExecutableElement method) {
		final AnnotationMirror findAnnotation = getAnnotationMirror( method, FIND );
		if ( findAnnotation == null ) {
			return emptyList();
		}
		else {
			final AnnotationValue enabledFetchProfiles =
					getAnnotationValue( findAnnotation, "enabledFetchProfiles" );
			if ( enabledFetchProfiles == null ) {
				return emptyList();
			}
			else {
				@SuppressWarnings("unchecked")
				final List<AnnotationValue> annotationValues = (List<AnnotationValue>) enabledFetchProfiles.getValue();
				final List<String> result = annotationValues.stream().map(AnnotationValue::toString).collect(toList());
				if ( result.stream().anyMatch("<error>"::equals) ) {
					throw new ProcessLaterException();
				}
				return result;
			}
		}
	}

	private void createMultipleParameterFinder(
			ExecutableElement method, TypeMirror returnType, TypeElement entity, @Nullable String containerType) {
		final String methodName = method.getSimpleName().toString();
		final List<String> paramNames = parameterNames( method, entity );
		final List<String> paramTypes = parameterTypes( method );
		final String[] sessionType = sessionTypeFromParameters( paramNames, paramTypes );
		final String methodKey = methodName + paramTypes;
		final List<Boolean> multivalued = new ArrayList<>();
		final List<@Nullable FieldType> fieldTypes = new ArrayList<>();
		for ( VariableElement parameter : method.getParameters() ) {
			if ( isFinderParameterMappingToAttribute( parameter ) ) {
				final FieldType fieldType = validateFinderParameter(entity, parameter);
				fieldTypes.add( fieldType );
				multivalued.add( fieldType == FieldType.MULTIVALUED );
			}
			else {
				multivalued.add( false );
			}
		}
		warnAboutMissingOrder( method );
		if ( !usingStatelessSession( sessionType[0] ) // no byNaturalId() lookup API for SS
				&& matchesNaturalKey( entity, fieldTypes ) ) {
			putMember( methodKey,
					new NaturalIdFinderMethod(
							this, method,
							methodName,
							returnType.toString(),
							containerType,
							paramNames,
							paramTypes,
							parameterNullability(method, entity),
							repository,
							sessionType[0],
							sessionType[1],
							enabledFetchProfiles( method ),
							context.addNonnullAnnotation(),
							jakartaDataRepository,
							fullReturnType(method)
					)
			);
		}
		else {
			final List<Boolean> paramPatterns = parameterPatterns( method );
			putMember( methodKey,
					new CriteriaFinderMethod(
							this, method,
							methodName,
							returnType.toString(),
							containerType,
							paramNames,
							paramTypes,
							parameterNullability(method, entity),
							multivalued,
							paramPatterns,
							repository,
							sessionType[0],
							sessionType[1],
							enabledFetchProfiles( method ),
							orderByList( method, entity ),
							context.addNonnullAnnotation(),
							jakartaDataRepository,
							fullReturnType(method),
							hasAnnotation(method, NULLABLE)
					)
			);
		}
	}

	private void warnAboutMissingOrder(ExecutableElement method) {
		if ( !hasAnnotation(  method, JD_ORDER_BY, JD_ORDER_BY_LIST ) ) {
			boolean hasPageRequest = false;
			boolean hasSortOrOrder = false;
			for ( VariableElement parameter : method.getParameters() ) {
				final String parameterType = parameter.asType().toString();
				if ( isOrderParam( parameterType ) ) {
					hasSortOrOrder = true;
				}
				if ( isPageParam( parameterType ) ) {
					hasPageRequest = true;
				}
			}
			if ( hasPageRequest && !hasSortOrOrder ) {
				context.message( method, "'PageRequest' with no 'Sort' or 'Order' and no '@OrderBy'", Diagnostic.Kind.MANDATORY_WARNING );
			}
		}
	}

	private void createSingleParameterFinder(
			ExecutableElement method, TypeMirror returnType, TypeElement entity, @Nullable String containerType) {
		final String methodName = method.getSimpleName().toString();
		final VariableElement parameter =
				method.getParameters().stream()
						.filter(AnnotationMetaEntity::isFinderParameterMappingToAttribute)
						.findFirst().orElseThrow();
		final List<String> paramNames = parameterNames( method, entity );
		final List<String> paramTypes = parameterTypes( method );
		final String[] sessionType = sessionTypeFromParameters( paramNames, paramTypes );
		final FieldType fieldType = validateFinderParameter( entity, parameter );
		if ( fieldType != null ) {
			final String methodKey = methodName + "!";
			final List<String> profiles = enabledFetchProfiles( method );
			switch ( pickStrategy( fieldType, sessionType[0], profiles ) ) {
				case ID:
					putMember( methodKey,
							new IdFinderMethod(
									this, method,
									methodName,
									returnType.toString(),
									containerType,
									paramNames,
									paramTypes,
									repository,
									sessionType[0],
									sessionType[1],
									profiles,
									context.addNonnullAnnotation(),
									jakartaDataRepository,
									fullReturnType(method),
									hasAnnotation(method, NULLABLE)
							)
					);
					break;
				case NATURAL_ID:
					if ( !usingStatelessSession( sessionType[0] ) ) {// no byNaturalId() lookup API for SS
						putMember( methodKey,
								new NaturalIdFinderMethod(
										this, method,
										methodName,
										returnType.toString(),
										containerType,
										paramNames,
										paramTypes,
										parameterNullability(method, entity),
										repository,
										sessionType[0],
										sessionType[1],
										profiles,
										context.addNonnullAnnotation(),
										jakartaDataRepository,
										fullReturnType(method)
								)
						);
						break;
					}
					// else intentionally fall through
				case BASIC:
				case MULTIVALUED:
					putMember( methodKey,
							new CriteriaFinderMethod(
									this, method,
									methodName,
									returnType.toString(),
									containerType,
									paramNames,
									paramTypes,
									parameterNullability(method, entity),
									parameterMultivalued( method, fieldType ),
									parameterPatterns( method ),
									repository,
									sessionType[0],
									sessionType[1],
									profiles,
									orderByList( method, entity ),
									context.addNonnullAnnotation(),
									jakartaDataRepository,
									fullReturnType(method),
									hasAnnotation(method, NULLABLE)
							)
					);
					break;
			}
		}
	}

	private static List<Boolean> parameterMultivalued(ExecutableElement method, FieldType fieldType) {
		return method.getParameters().stream()
				.map( param -> isFinderParameterMappingToAttribute( param )
							&& fieldType == FieldType.MULTIVALUED )
				.collect( toList() );
	}

	private static FieldType pickStrategy(FieldType fieldType, String sessionType, List<String> profiles) {
		if ( ( usingStatelessSession(sessionType) || usingReactiveSession(sessionType) )
				&& !profiles.isEmpty() ) {
			// no support for passing fetch profiles i.e. IdentifierLoadAccess
			// in SS or M.S except via Query.enableFetchProfile()
			return FieldType.BASIC;
		}
		else {
			return switch (fieldType) {
				case ID ->
					// no byId() API for SS or M.S, only get()
						FieldType.ID;
				case NATURAL_ID ->
					// no byNaturalId() lookup API for SS
					// no byNaturalId() in M.S, but we do have Identifier workaround
						FieldType.NATURAL_ID;
				default -> FieldType.BASIC;
			};
		}
	}

	private boolean matchesNaturalKey(TypeElement entity, List<@Nullable FieldType> fieldTypes) {
		return fieldTypes.stream().allMatch(type -> type == FieldType.NATURAL_ID)
			&& entity.getEnclosedElements().stream()
				.filter(member -> hasAnnotation(member, NATURAL_ID))
				.count() == fieldTypes.size();
	}

	enum FieldType {
		ID, NATURAL_ID, BASIC, MULTIVALUED
	}

	private @Nullable FieldType validateFinderParameter(TypeElement entityType, VariableElement param) {
		final String path = parameterName( param );
		final boolean idClassRef = isIdRef( path ) && hasAnnotation( entityType, ID_CLASS );
		final Element member = idClassRef ? null : memberMatchingPath( entityType, path );
		if ( member != null ) {
			if ( containsAnnotation( member, MANY_TO_MANY, ONE_TO_MANY, ELEMENT_COLLECTION ) ) {
				message( param,
						"matching field is a collection",
						Diagnostic.Kind.ERROR );
				return null;
			}

			if ( checkParameterType( entityType, param, memberType( member ) ) ) {
				return FieldType.MULTIVALUED;
			}
			else if ( containsAnnotation( param, PATTERN ) ) {
				final AnnotationMirror mirror = getAnnotationMirror(param, PATTERN);
				if ( mirror!=null && !typeNameEquals(param.asType(), STRING) ) {
					message( param, mirror,
							"parameter annotated '@Pattern' is not of type 'String'",
							Diagnostic.Kind.ERROR );
				}
				return FieldType.BASIC;
			}
			else if ( containsAnnotation( member, ID, EMBEDDED_ID ) ) {
				return FieldType.ID;
			}
			else if ( containsAnnotation( member, NATURAL_ID ) ) {
				return FieldType.NATURAL_ID;
			}
			else {
				return FieldType.BASIC;
			}
		}
		else if ( idClassRef ) {
			final AnnotationMirror idClass = getAnnotationMirror( entityType, ID_CLASS );
			if ( idClass == null ) {
				return null; // cannot happen!
			}
			else {
				final AnnotationValue value = getAnnotationValue( idClass );
				if ( value != null
					&& isSameType( actualParameterType( param ),
						(TypeMirror) value.getValue() ) ) {
					return FieldType.ID;
				}
				else {
					message( param,
							"does not match id class of entity class '" + entityType + "'",
							Diagnostic.Kind.ERROR );
					return null;
				}
			}
		}
		else {
			message( param,
					"no matching field named '" + path
						+ "' in entity class '" + entityType + "'",
					Diagnostic.Kind.ERROR );
			return null;
		}
	}

	private TypeMirror actualParameterType(VariableElement param) {
		final ExecutableElement method =
				(ExecutableElement)
						param.getEnclosingElement();
		final ExecutableType methodType =
				(ExecutableType)
						context.getTypeUtils()
								.asMemberOf( (DeclaredType) element.asType(), method );
		return methodType.getParameterTypes()
				.get( method.getParameters().indexOf( param ) );
	}

	/**
	 * Check the type of a parameter of a {@code @Find} method against the field type
	 * in the entity class.
	 * @return true if the parameter is multivalued (i.e. it's an {@code in} condition)
	 */
	private boolean checkParameterType(TypeElement entityType, VariableElement param, TypeMirror attributeType) {
		final Types types = context.getTypeUtils();
//		if ( entityType.getKind() == CLASS ) { // do no checks if the entity type is a type variable
			TypeMirror parameterType = parameterType( param );
			if ( types.isSameType( parameterType, attributeType ) ) {
				return false;
			}
			else {
				if ( attributeType.getKind().isPrimitive() ) {
					final PrimitiveType primitiveType = (PrimitiveType) attributeType;
					attributeType = types.boxedClass( primitiveType ).asType();
				}
				final TypeKind kind = parameterType.getKind();
				switch (kind) {
					case TYPEVAR:
						final TypeVariable typeVariable = (TypeVariable) parameterType;
						parameterType = typeVariable.getUpperBound();
						// INTENTIONAL FALL-THROUGH
					case DECLARED:
						if ( types.isSameType(parameterType, attributeType) ) {
							return false;
						}
						else {
							final TypeElement list = context.getTypeElementForFullyQualifiedName(LIST);
							final TypeElement range = context.getTypeElementForFullyQualifiedName(HIB_RANGE);
							if ( types.isSameType( parameterType, types.getDeclaredType(list, attributeType) ) ) {
								return true;
							}
							else if ( types.isSameType( parameterType, types.getDeclaredType(range, attributeType) ) ) {
								return false;
							}
							else {
								parameterTypeError( entityType, param, attributeType );
								return false;
							}
						}
					case ARRAY:
						if ( !types.isSameType( parameterType, types.getArrayType(attributeType) ) ) {
							parameterTypeError( entityType, param, attributeType );
						}
						return true;
					default:
						if ( kind.isPrimitive() ) {
							final PrimitiveType primitiveType = (PrimitiveType) parameterType;
							if ( !types.isSameType( types.boxedClass(primitiveType).asType(), attributeType ) ) {
								parameterTypeError( entityType, param, attributeType );
							}
							return false;
						}
						else {
							// probably impossible
							return false;
						}
				}
			}
//		}
//		else {
//			return false;
//		}
	}

	private void parameterTypeError(TypeElement entityType, VariableElement param, TypeMirror attributeType) {
		message(param,
				"matching field has type '" + stripAnnotations(typeAsString(attributeType))
						+ "' in entity class '" + entityType + "'",
				Diagnostic.Kind.ERROR );
	}

	private String stripAnnotations(String type) {
		if ( type.startsWith("@") ) {
			final int index = type.lastIndexOf(' ');
			return index > 0 ? type.substring( index + 1 ) : type;
		}
		else {
			return type;
		}
	}

	private boolean finderParameterNullable(TypeElement entity, VariableElement param) {
		final Element member = memberMatchingPath( entity, parameterName( param ) );
		return isNullable( param )
			&& ( member == null || isNullable( member ) );
	}

	private AccessType getAccessType(TypeElement entity) {
		final String entityClassName = entity.getQualifiedName().toString();
		determineAccessTypeForHierarchy( entity, context );
		return castNonNull( context.getAccessTypeInfo( entityClassName ) ).getAccessType();
	}

	private static TypeMirror memberType(Element member) {
		if ( member.getKind() == ElementKind.METHOD ) {
			final ExecutableElement method = (ExecutableElement) member;
			return method.getReturnType();
		}
		else {
			return member.asType();
		}
	}

	private @Nullable Element memberMatchingPath(TypeElement entityType, String path) {
		return memberMatchingPath( entityType, new StringTokenizer(path, ".") );
	}

	private @Nullable Element memberMatchingPath(TypeElement entityType, StringTokenizer tokens) {
		final AccessType accessType = getAccessType(entityType);
		final String nextToken = tokens.nextToken();
		for ( Element member : context.getAllMembers(entityType) ) {
			if ( isIdRef(nextToken) && hasAnnotation(member, ID, EMBEDDED_ID) ) {
				return member;
			}
			final Element match =
					memberMatchingPath( entityType, member, accessType, tokens, nextToken );
			if ( match != null ) {
				return match;
			}
		}
		return null;
	}

	private static boolean isIdRef(String token) {
		return "id(this)".equalsIgnoreCase(token); // post M4
	}

	private @Nullable Element memberMatchingPath(
			TypeElement entityType,
			Element candidate,
			AccessType accessType,
			StringTokenizer tokens,
			String token) {
		final Name memberName = candidate.getSimpleName();
		final TypeMirror type = memberType( candidate, accessType, token, memberName );
		if (type == null) {
			return null;
		}
		else if ( tokens.hasMoreTokens() ) {
			return type.getKind() == TypeKind.DECLARED
					? memberForPath( entityType, tokens, (DeclaredType) type, memberName )
					: null;
		}
		else {
			return candidate;
		}
	}

	private @Nullable Element memberForPath(
			TypeElement entityType, StringTokenizer tokens, DeclaredType type, Name memberName) {
		final TypeElement memberType = (TypeElement) type.asElement();
		memberTypes.put( qualify( entityType.getQualifiedName().toString(), memberName.toString() ),
				memberType.getQualifiedName().toString() ); // NOTE SIDE EFFECT!
		return memberMatchingPath( memberType, tokens );
	}

	private static @Nullable TypeMirror memberType(Element candidate, AccessType accessType, String token, Name memberName) {
		final ElementKind kind = candidate.getKind();
		if ( accessType == AccessType.FIELD && kind == ElementKind.FIELD ) {
			return fieldMatches(token, memberName)
					? candidate.asType()
					: null;
		}
		else if ( accessType == AccessType.PROPERTY && kind == ElementKind.METHOD ) {
			final ExecutableElement executable = (ExecutableElement) candidate;
			return getterMatches(token, memberName)
					? executable.getReturnType()
					: null;
		}
		else {
			return null;
		}
	}

	private static boolean fieldMatches(String token, Name fieldName) {
		return fieldName.contentEquals( token );
	}

	private static boolean getterMatches(String token, Name methodName) {
		if ( hasPrefix( methodName, "get" ) ) {
			return token.equals( decapitalize( methodName.subSequence( 3, methodName.length()).toString() ) );
		}
		else if ( hasPrefix( methodName, "is" ) ) {
			return token.equals( decapitalize( methodName.subSequence( 2, methodName.length() ).toString() ) );
		}
		else {
			return false;
		}
	}

	private void addQueryMethod(
			ExecutableElement method,
			@Nullable TypeMirror returnType,
			@Nullable TypeElement containerType,
			AnnotationMirror mirror,
			boolean isNative) {
		// The following is quite fragile!
		final String containerTypeName;
		if ( containerType == null ) {
			if ( returnType != null && returnType.getKind() == TypeKind.ARRAY ) {
				final ArrayType arrayType = (ArrayType) returnType;
				final TypeMirror componentType = arrayType.getComponentType();
				final TypeElement object = context.getElementUtils().getTypeElement(JAVA_OBJECT);
				if ( !isSameType( object.asType(), componentType ) ) {
					returnType = componentType;
					containerTypeName = "[]";
				}
				else {
					// assume it's returning a single tuple as Object[]
					containerTypeName = null;
				}
			}
			else {
				containerTypeName = null;
			}
		}
		else {
			containerTypeName = containerType.getQualifiedName().toString();
		}

		final AnnotationValue value = getAnnotationValue( mirror );
		if ( value != null && value.getValue() instanceof String queryString ) {
			addQueryMethod( method, returnType, containerTypeName, mirror, isNative, value, queryString );
		}
	}

	private void addQueryMethod(
			ExecutableElement method,
			@Nullable TypeMirror returnType,
			@Nullable String containerTypeName,
			AnnotationMirror mirror,
			boolean isNative,
			AnnotationValue value,
			String queryString) {

		final List<String> paramNames = parameterNames(method);
		final List<String> paramTypes = parameterTypes(method);

		// now check that the query has a parameter for every method parameter
		checkParameters(method, returnType, paramNames, paramTypes, mirror, value, queryString);

		final String[] sessionType = sessionTypeFromParameters( paramNames, paramTypes );
		final DeclaredType resultType = resultType(method, returnType, mirror, value);
		final List<OrderBy> orderBys =
				resultType == null
						? emptyList()
						: orderByList( method, (TypeElement) resultType.asElement() );

		final String processedQuery;
		if (isNative) {
			processedQuery = queryString;
			validateSql(method, mirror, processedQuery, paramNames, value);
		}
		else {
			processedQuery = addFromClauseIfNecessary( queryString, implicitEntityName(resultType) );
			validateHql(method, returnType, mirror, value, processedQuery, paramNames, paramTypes);
		}

		final QueryMethod attribute =
				new QueryMethod(
						this, method,
						method.getSimpleName().toString(),
						processedQuery,
						returnType == null ? null : returnType.toString(),
						returnType == null ? null : returnTypeClass(returnType),
						containerTypeName,
						paramNames,
						paramTypes,
						isInsertUpdateDelete(queryString),
						isNative,
						repository,
						sessionType[0],
						sessionType[1],
						orderBys,
						context.addNonnullAnnotation(),
						jakartaDataRepository,
						fullReturnType(method),
						hasAnnotation(method, NULLABLE)
				);
		putMember( attribute.getPropertyName() + paramTypes, attribute );
	}

	private String fullReturnType(ExecutableElement method) {
		return typeAsString( memberMethodType(method).getReturnType() );
	}

	private static String returnTypeClass(TypeMirror returnType) {
		switch (returnType.getKind()) {
			case DECLARED:
				DeclaredType declaredType = (DeclaredType) returnType;
				final TypeElement typeElement = (TypeElement) declaredType.asElement();
				return typeElement.getQualifiedName().toString();
			case INT:
				return "int";
			case LONG:
				return "long";
			case SHORT:
				return "short";
			case BYTE:
				return "byte";
			case BOOLEAN:
				return "boolean";
			case FLOAT:
				return "float";
			case DOUBLE:
				return "double";
			case CHAR:
				return "char";
			case ARRAY:
				final ArrayType arrayType = (ArrayType) returnType;
				return returnTypeClass( arrayType.getComponentType() ) + "[]";
			default:
				return returnType.toString();
		}
	}

	private @Nullable String implicitEntityName(@Nullable DeclaredType resultType) {
		if ( resultType != null && hasAnnotation(resultType.asElement(), ENTITY) ) {
			final AnnotationMirror annotation =
					getAnnotationMirror(resultType.asElement(), ENTITY);
			if ( annotation == null ) {
				throw new AssertionFailure("@Entity annotation should not be missing");
			}
			return entityName(resultType, annotation);
		}
		else if ( primaryEntity != null ) {
			return primaryEntity.getSimpleName().toString();
		}
		else {
			return null;
		}
	}

	private @Nullable TypeElement implicitEntityType(@Nullable TypeElement resultType) {
		if ( resultType != null && hasAnnotation(resultType, ENTITY) ) {
			return resultType;
		}
		else if ( primaryEntity != null ) {
			return primaryEntity;
		}
		else {
			return null;
		}
	}

	private static String entityName(DeclaredType resultType, AnnotationMirror annotation) {
		final AnnotationValue name = getAnnotationValue(annotation, "name");
		if (name != null) {
			final String explicitName = name.getValue().toString();
			if ( !explicitName.isEmpty() ) {
				return explicitName;
			}
		}
		return resultType.asElement().getSimpleName().toString();
	}

	/**
	 * In the {@code @Query} annotation we tolerate missing {@code from} clauses
	 * to an extent that ORM core does not. For example, we accept {@code select name, age}
	 * and {@code select id where year(date)>:year} as a legit queries. (It would be easy to
	 * change ORM core to also accept these queries.)
	 */
	private static String addFromClauseIfNecessary(String hql, @Nullable String entityType) {
		if ( entityType == null ) {
			return hql;
		}
		else if ( isInsertUpdateDelete(hql) ) {
			return hql;
		}
		else {
			final HqlLexer hqlLexer = HqlParseTreeBuilder.INSTANCE.buildHqlLexer( hql );
			final List<? extends Token> allTokens = hqlLexer.getAllTokens();
			for ( final Token token : allTokens ) {
				switch ( token.getType() ) {
					case FROM:
						return hql;
					case WHERE:
					case HAVING:
					case GROUP:
					case ORDER:
						return new StringBuilder(hql)
								.insert(token.getStartIndex(), "from " + entityType + " ")
								.toString();
				}
			}
			return hql + " from " + entityType;
		}
	}

	private @Nullable DeclaredType resultType(
			ExecutableElement method, @Nullable TypeMirror returnType, AnnotationMirror mirror, AnnotationValue value) {
		if ( returnType != null && returnType.getKind() == TypeKind.DECLARED ) {
			final DeclaredType resultType = (DeclaredType) returnType;
			if ( !resultType.getTypeArguments().isEmpty() ) {
				message(method, mirror, value,
						"query result type may not be a generic type"
								+ " (change '" + returnType +
								"' to '" + context.getTypeUtils().erasure(returnType) + "')",
						Diagnostic.Kind.ERROR );
			}
			return resultType;
		}
		else {
			return null;
		}
	}

	private static boolean isInsertUpdateDelete(String hql) {
		final String trimmed = hql.trim();
		final String keyword = trimmed.length() > 6 ? trimmed.substring(0, 6) : "";
		return keyword.equalsIgnoreCase("update")
			|| keyword.equalsIgnoreCase("delete")
			|| keyword.equalsIgnoreCase("insert");
	}

	private void validateHql(
			ExecutableElement method,
			@Nullable TypeMirror returnType,
			AnnotationMirror mirror,
			AnnotationValue value,
			String hql,
			List<String> paramNames, List<String> paramTypes) {
		final SqmStatement<?> statement =
				Validation.validate(
						hql,
						returnType,
						true,
						new ErrorHandler( context, isLocal(method) ? method : element, mirror, value, hql ),
						ProcessorSessionFactory.create( context.getProcessingEnvironment(),
								context.getEntityNameMappings(), context.getEnumTypesByValue(), context.isIndexing() )
				);
		if ( statement != null ) {
			if ( statement instanceof SqmSelectStatement<?> selectStatement ) {
				validateSelectHql( method, returnType, mirror, value, selectStatement );
			}
			else {
				validateUpdateHql( method, returnType, mirror, value );
			}
			for ( SqmParameter<?> param : statement.getSqmParameters() ) {
				checkParameter( param, paramNames, paramTypes, method, mirror, value);
			}
		}
	}

	private void validateUpdateHql(
			ExecutableElement method,
			@Nullable TypeMirror returnType,
			AnnotationMirror mirror,
			AnnotationValue value) {
		final boolean reactive = isReactive();
		if ( !isValidUpdateReturnType( returnType, method, reactive ) ) {
			message( method, mirror, value,
					"return type of mutation query method must be "
							+ (!reactive ? "'int', 'long', 'boolean' or 'void'"
								: "'Uni<Integer>', 'Uni<Boolean>' or 'Uni<Void>'"),
					Diagnostic.Kind.ERROR );
		}
	}

	private boolean isValidUpdateReturnType(@Nullable TypeMirror returnType, ExecutableElement method, boolean reactive) {
		if ( returnType == null ) {
			return false;
		}
		if ( reactive ) {
			// for reactive calls, don't use the returnType param, which has been ununi-ed, we want to check the full one
			final String returnTypeName = fullReturnType(method);
			return returnTypeName.equals( UNI_VOID )
				|| returnTypeName.equals( UNI_BOOLEAN )
				|| returnTypeName.equals( UNI_INTEGER );
		}
		else {
			// non-reactive
			return returnType.getKind() == TypeKind.VOID
				|| returnType.getKind() == TypeKind.BOOLEAN
				|| returnType.getKind() == TypeKind.INT
				|| returnType.getKind() == TypeKind.LONG;
		}
	}

	private void validateSelectHql(
			ExecutableElement method,
			@Nullable TypeMirror returnType,
			AnnotationMirror mirror,
			AnnotationValue value,
			SqmSelectStatement<?> statement) {
		if ( returnType != null ) {
			final JpaSelection<?> selection = statement.getSelection();
			boolean returnTypeCorrect;
			if ( selection.isCompoundSelection() ) {
				switch ( returnType.getKind() ) {
					case ARRAY:
						returnTypeCorrect = checkReturnedArrayType( (ArrayType) returnType );
						break;
					case DECLARED:
						if ( !checkConstructorReturn( (DeclaredType) returnType, selection ) ) {
							message(method, mirror, value,
									"return type '" + returnType
											+ "' of method has no constructor matching query selection list",
									Diagnostic.Kind.ERROR);
						}
						returnTypeCorrect = true;
						break;
					default:
						returnTypeCorrect = false;
				}
			}
			else if ( selection instanceof JpaEntityJoin<?, ?> from ) {
				returnTypeCorrect = checkReturnedEntity( from.getModel(), returnType );
			}
			else if ( selection instanceof JpaRoot<?> from ) {
				returnTypeCorrect = checkReturnedEntity( from.getModel(), returnType );
			}
			else {
				// TODO: anything more we can do here? e.g. check constructor
				try {
					final Class<?> javaResultType = selection.getJavaType();
					if ( javaResultType == null ) {
						returnTypeCorrect = true;
					}
					else {
						final TypeElement typeElement = context.getTypeElementForFullyQualifiedName( javaResultType.getName() );
						final Types types = context.getTypeUtils();
						returnTypeCorrect = types.isAssignable( returnType, types.erasure( typeElement.asType() ) );
					}
				}
				catch (Exception e) {
					//ignore
					returnTypeCorrect = true;
				}
			}
			if ( !returnTypeCorrect ) {
				message(method, mirror, value,
						"return type of query did not match return type '" + returnType + "' of method",
						Diagnostic.Kind.ERROR);
			}
		}
	}

	private void validateSql(
			ExecutableElement method,
			AnnotationMirror mirror,
			String sql,
			List<String> paramNames,
			AnnotationValue value) {
		// for SQL queries check that there is a method parameter for every query parameter
		ParameterParser.parse(sql, new ParameterRecognizer() {
			int ordinalCount = 0;
			@Override
			public void ordinalParameter(int sourcePosition) {
				ordinalCount++;
				if ( ordinalCount > paramNames.size() ) {
					message(method, mirror, value,
							"missing method parameter for query parameter " + ordinalCount
									+ " (add a parameter to '" + method.getSimpleName() + "')",
							Diagnostic.Kind.ERROR );
				}
			}

			@Override
			public void namedParameter(String name, int sourcePosition) {
				if ( !paramNames.contains(name) ) {
					message(method, mirror, value,
							"missing method parameter for query parameter :" + name
									+ " (add a parameter '" + name + "' to '" + method.getSimpleName() + "')",
							Diagnostic.Kind.ERROR );
				}
			}

			@Override
			public void jpaPositionalParameter(int label, int sourcePosition) {
				if ( label > paramNames.size() ) {
					message(method, mirror, value,
							"missing method parameter for query parameter ?" + label
									+ " (add a parameter to '" + method.getSimpleName() + "')",
							Diagnostic.Kind.ERROR );
				}
			}

			@Override
			public void other(char character) {
			}
		});
	}

	private static boolean checkConstructorReturn(DeclaredType returnType, JpaSelection<?> selection) {
		final List<? extends JpaSelection<?>> selectionItems = selection.getSelectionItems();
		if ( selectionItems == null ) {
			// should not occur
			return true;
		}
		final TypeElement typeElement = (TypeElement) returnType.asElement();
		final Name qualifiedName = typeElement.getQualifiedName();
		if ( qualifiedName.contentEquals(TUPLE)
				|| qualifiedName.contentEquals(LIST)
				|| qualifiedName.contentEquals(MAP) ) {
			// these are exceptionally allowed
			return true;
		}
		else {
			// otherwise we need appropriate constructor
			for ( Element member : typeElement.getEnclosedElements() ) {
				if ( member.getKind() == ElementKind.CONSTRUCTOR ) {
					final ExecutableElement constructor = (ExecutableElement) member;
					if ( constructorMatches( selectionItems, constructor.getParameters() ) ) {
						return true;
					}
				}
			}
			return false;
		}
	}

	private static boolean constructorMatches(
			List<? extends JpaSelection<?>> selectionItems,
			List<? extends VariableElement> parameters) {
		int itemCount = selectionItems.size();
		if ( parameters.size() == itemCount ) {
			for (int i = 0; i < itemCount; i++ ) {
				final JpaSelection<?> item = selectionItems.get(i);
				if ( item != null && item.getJavaType() != null ) {
					if ( !parameterMatches( parameters.get(i), item ) ) {
						return false;
					}
				}
			}
			return true;
		}
		else {
			return false;
		}
	}

	private static boolean parameterMatches(VariableElement parameter, JpaSelection<?> item) {
		final Class<?> javaType = item.getJavaType();
		return javaType != null && parameterMatches( parameter.asType(), javaType, item.getJavaTypeName() );
	}

	private static boolean parameterMatches(TypeMirror parameterType, Class<?> itemType, String itemTypeName) {
		final TypeKind kind = parameterType.getKind();
		if ( kind == TypeKind.DECLARED ) {
			final DeclaredType declaredType = (DeclaredType) parameterType;
			final TypeElement paramTypeElement = (TypeElement) declaredType.asElement();
			return paramTypeElement.getQualifiedName().contentEquals(itemTypeName);
		}
		else if ( kind.isPrimitive() ) {
			return primitiveClassMatchesKind( itemType, kind );
		}
		else if ( kind == TypeKind.ARRAY ) {
			final ArrayType arrayType = (ArrayType) parameterType;
			return itemType.isArray()
				&& parameterMatches( arrayType.getComponentType(), itemType.getComponentType(), itemType.getComponentType().getTypeName() );
		}
		else {
			return false;
		}
	}

	private static boolean checkReturnedArrayType(ArrayType returnType) {
		final TypeMirror componentType = returnType.getComponentType();
		if ( componentType.getKind() == TypeKind.DECLARED ) {
			final DeclaredType declaredType = (DeclaredType) componentType;
			final TypeElement typeElement = (TypeElement) declaredType.asElement();
			return typeElement.getQualifiedName().contentEquals(JAVA_OBJECT);
		}
		else {
			return false;
		}
	}

	private boolean checkReturnedEntity(EntityDomainType<?> model, TypeMirror returnType) {
		if ( returnType.getKind() == TypeKind.DECLARED ) {
			final DeclaredType declaredType = (DeclaredType) returnType;
			final TypeElement typeElement = (TypeElement) declaredType.asElement();
//			final AnnotationMirror mirror = getAnnotationMirror(typeElement, ENTITY );
//			if ( mirror != null ) {
//				message( element, "entity return type '" + typeElement.getQualifiedName()
//								+ "' was not annotated '@Entity'", Diagnostic.Kind.WARNING );
//			}
			return typeElement.getQualifiedName().contentEquals( model.getHibernateEntityName() );
		}
		return false;
	}

	private void checkParameter(
			SqmParameter<?> param, List<String> paramNames, List<String> paramTypes,
			ExecutableElement method, AnnotationMirror mirror, AnnotationValue value) {
		final SqmExpressible<?> expressible = param.getExpressible(); //same object as param.getAnticipatedType()
		final String queryParamType = expressible == null ? null : expressible.getTypeName(); //getTypeName() can return "unknown"
		if ( queryParamType!=null
				//TODO: arguments of functions get assigned "unknown" which sucks
				&& !"unknown".equals(queryParamType) ) {
			if ( param.getName() != null ) {
				checkNamedParameter(param, paramNames, paramTypes, method, mirror, value, queryParamType);
			}
			else if ( param.getPosition() != null ) {
				checkOrdinalParameter(param, paramNames, paramTypes, method, mirror, value, queryParamType);
			}
		}
	}

	private void checkOrdinalParameter(
			SqmParameter<?> param, List<String> paramNames, List<String> paramTypes, ExecutableElement method,
			AnnotationMirror mirror, AnnotationValue value, String queryParamType) {
		int position = param.getPosition();
		if ( position > paramNames.size() ) {
			message(method, mirror, value,
					"missing method parameter for query parameter ?" + position
							+ " (add a parameter of type '" + queryParamType + "' to '" + method.getSimpleName() + "')",
					Diagnostic.Kind.ERROR);
		}
		else {
			final String argType = paramTypes.get(position - 1);
			if ( !isLegalAssignment(param, argType, queryParamType) ) {
				message(method, mirror, value,
						"parameter matching query parameter ?" + position + " has the wrong type"
								+ " (change the method parameter type to '" + queryParamType + "')",
						Diagnostic.Kind.ERROR);
			}
		}
	}

	private static String stripTypeAnnotations(String argType) {
		while ( argType.startsWith("@") ) {
			int index = argType.indexOf(' ');
			if (index>0) {
				argType = argType.substring(index+1);
			}
		}
		return argType;
	}

	private void checkNamedParameter(
			SqmParameter<?> param, List<String> paramNames, List<String> paramTypes, ExecutableElement method,
			AnnotationMirror mirror, AnnotationValue value, String queryParamType) {
		final String name = param.getName();
		int index = paramNames.indexOf( name );
		if ( index < 0 ) {
			message(method, mirror, value,
					"missing method parameter for query parameter :" + name
					+ " (add a parameter '" + queryParamType + ' ' + name + "' to '" + method.getSimpleName() + "')",
					Diagnostic.Kind.ERROR);
		}
		else {
			final String argType = paramTypes.get(index);
			if ( !isLegalAssignment(param, argType, queryParamType) ) {
				message(method, mirror, value,
						"parameter matching query parameter :" + name + " has the wrong type"
								+ " (change the method parameter type to '" + queryParamType + "')",
						Diagnostic.Kind.ERROR);
			}
		}
	}

	private static boolean isLegalAssignment(SqmParameter<?> param, String argumentType, String queryParamType) {
		final String argType = stripTypeAnnotations(argumentType);
		return param.allowMultiValuedBinding()
				? isLegalAssignment(argType, LIST + '<' + queryParamType + '>')
					|| isLegalAssignment(argType, SET + '<' + queryParamType + '>')
					|| isLegalAssignment(argType, COLLECTION + '<' + queryParamType + '>')
				: isLegalAssignment(argType, queryParamType);
	}

	private static boolean isLegalAssignment(String argType, String paramType) {
		return paramType.equals(argType)
			|| paramType.equals(fromPrimitive(argType));
	}

	private static @Nullable String fromPrimitive(String argType) {
		return switch (argType) {
			case "boolean" -> Boolean.class.getName();
			case "char" -> Character.class.getName();
			case "int" -> Integer.class.getName();
			case "long" -> Long.class.getName();
			case "short" -> Short.class.getName();
			case "byte" -> Byte.class.getName();
			case "float" -> Float.class.getName();
			case "double" -> Double.class.getName();
			default -> null;
		};
	}

	private List<Boolean> parameterNullability(ExecutableElement method, TypeElement entity) {
		return method.getParameters().stream()
				.map(param -> finderParameterNullable(entity, param))
				.collect(toList());
	}

	private List<String> parameterTypes(ExecutableElement method) {
		return method.getParameters().stream()
				.map(param -> typeAsString(parameterType(param)))
				.collect(toList());
	}

	/**
	 * Workaround for a bug in Java 20/21. Should not be necessary!
	 */
	private String typeAsString(TypeMirror type) {
		String result = type.toString();
		for ( AnnotationMirror annotation : type.getAnnotationMirrors() ) {
			final String annotationString = annotation.toString();
			result = result
					// if it has a space after it, we need to remove that too
					.replace(annotationString + ' ', "")
					// just in case it did not have a space after it
					.replace(annotationString, "");
		}
		for ( AnnotationMirror annotation : type.getAnnotationMirrors() ) {
			result = annotation.toString() + ' ' + result;
		}
		return result;
	}

	private TypeMirror parameterType(VariableElement parameter) {
		final ExecutableElement method =
				(ExecutableElement) parameter.getEnclosingElement();
		final TypeMirror type =
				memberMethodType(method).getParameterTypes()
						.get( method.getParameters().indexOf(parameter) );
		switch ( type.getKind() ) {
			case TYPEVAR:
				final TypeVariable typeVariable = (TypeVariable) type;
				return context.getTypeUtils().erasure(typeVariable);
			case DECLARED:
				final DeclaredType declaredType = (DeclaredType) type;
				return declaredType.getTypeArguments().stream()
						.anyMatch(arg -> arg.getKind() == TypeKind.TYPEVAR)
						? context.getTypeUtils().erasure(type)
						: type;
			default:
				return type;
		}
	}

	private ExecutableType memberMethodType(ExecutableElement method) {
		return (ExecutableType) context.getTypeUtils()
				.asMemberOf((DeclaredType) element.asType(), method);
	}

	private static List<Boolean> parameterPatterns(ExecutableElement method) {
		return method.getParameters().stream()
				.map(param -> hasAnnotation(param, PATTERN))
				.toList();
	}

	private List<String> parameterNames(ExecutableElement method, TypeElement entity) {
		final String idName =
				hasAnnotation( entity, ID_CLASS )
					? ID_ROLE_NAME
					// account for special @By("id(this)") hack in Jakarta Data
					: entity.getEnclosedElements().stream()
							.filter(member -> hasAnnotation(member, ID))
							.map(TypeUtils::propertyName)
							.findFirst()
							.orElse(ID_ROLE_NAME);
		return method.getParameters().stream()
				.map(AnnotationMetaEntity::parameterName)
				.map(name -> isIdRef(name) ? idName : name)
				.toList();
	}

	private static List<String> parameterNames(ExecutableElement method) {
		return method.getParameters().stream()
				.map(AnnotationMetaEntity::parameterName)
				.toList();
	}

	private static String parameterName(VariableElement parameter) {
		final AnnotationMirror by = getAnnotationMirror( parameter, "jakarta.data.repository.By" );
		final AnnotationMirror param = getAnnotationMirror( parameter, "jakarta.data.repository.Param" );
		if ( by != null ) {
			final String name =
					castNonNull(getAnnotationValue(by))
							.getValue().toString();
			if ( name.contains("<error>") ) {
				throw new ProcessLaterException();
			}
			return name
					.replace('$', '.')
					.replace('_', '.');
		}
		else if ( param != null ) {
			final String name =
					castNonNull(getAnnotationValue(param))
							.getValue().toString();
			if ( name.contains("<error>") ) {
				throw new ProcessLaterException();
			}
			return name;
		}
		else {
			return parameter.getSimpleName().toString()
					.replace('$', '.')
					.replace('_', '.');
		}
	}

	private static boolean isNullable(Element member) {
		switch ( member.getKind() ) {
			case METHOD:
				final ExecutableElement method = (ExecutableElement) member;
				if ( method.getReturnType().getKind().isPrimitive() ) {
					return false;
				}
			case FIELD:
			case PARAMETER:
				if ( member.asType().getKind().isPrimitive() ) {
					return false;
				}
		}
		for ( AnnotationMirror mirror : member.getAnnotationMirrors() ) {
			final TypeElement annotationType = (TypeElement) mirror.getAnnotationType().asElement();
			final Name name = annotationType.getQualifiedName();
			if ( name.contentEquals(Constants.ID)
				|| name.contentEquals(Constants.NOT_NULL)
				|| name.contentEquals(Constants.NONNULL) ) {
				return false;
			}
			else if ( name.contentEquals(Constants.BASIC)
					|| name.contentEquals(Constants.MANY_TO_ONE)
					|| name.contentEquals(Constants.ONE_TO_ONE)) {
				final AnnotationValue optional = getAnnotationValue(mirror, "optional");
				if ( optional != null && optional.getValue().equals(FALSE) ) {
					return false;
				}
			}
		}
		return true;
	}

	private void checkParameters(
			ExecutableElement method,
			@Nullable TypeMirror returnType,
			List<String> paramNames, List<String> paramTypes,
			AnnotationMirror mirror,
			AnnotationValue value,
			String hql) {
		for (int i = 1; i <= paramNames.size(); i++) {
			final String param = paramNames.get(i-1);
			final String type = paramTypes.get(i-1);
			if ( parameterIsMissing( hql, i, param, type ) ) {
				message( method, mirror, value,
						"missing query parameter for '" + param
								+ "' (no parameter named :" + param + " or ?" + i + ")",
						Diagnostic.Kind.ERROR );
			}
		}
		if ( returnType != null ) {
			for ( VariableElement parameter : method.getParameters() ) {
				checkFinderParameter( explicitEntityType(returnType), parameter );
			}
		}
	}

	private @Nullable TypeElement explicitEntityType(@Nullable TypeMirror resultType) {
		if ( resultType != null && resultType.getKind() == TypeKind.DECLARED) {
			final DeclaredType declaredType = (DeclaredType) resultType;
			final Element typeElement = declaredType.asElement();
			if ( hasAnnotation(typeElement, ENTITY) ) {
				return (TypeElement) typeElement;
			}
		}
		return null;
	}

	private static boolean typeNameEquals(TypeMirror parameterType, String typeName) {
		if ( parameterType.getKind() == TypeKind.DECLARED ) {
			final DeclaredType declaredType = (DeclaredType) parameterType;
			final TypeElement typeElement = (TypeElement) declaredType.asElement();
			return typeElement.getQualifiedName().contentEquals(typeName);
		}
		else {
			return false;
		}
	}

	private static boolean typeNameEqualsArray(TypeMirror parameterType, String typeName) {
		if ( parameterType.getKind() == TypeKind.ARRAY ) {
			final ArrayType arrayType = (ArrayType) parameterType;
			return typeNameEquals( arrayType.getComponentType(), typeName );
		}
		else {
			return false;
		}
	}

	private static String typeName(TypeMirror parameterType) {
		if ( parameterType.getKind() == TypeKind.DECLARED ) {
			final DeclaredType declaredType = (DeclaredType) parameterType;
			final TypeElement typeElement = (TypeElement) declaredType.asElement();
			return typeElement.getQualifiedName().toString();
		}
		else {
			return parameterType.toString();
		}
	}

	private static boolean parameterIsMissing(String hql, int i, String param, String type) {
		return !hasParameter(hql, i, param) && !isSpecialParam(type);
	}

	private static boolean hasParameter(String hql, int i, String param) {
		return Pattern.compile(".*(:" + param + "|\\?" + i + ")\\b.*", Pattern.DOTALL)
				.matcher(hql).matches();
	}

	private static boolean usingReactiveSession(String sessionType) {
		return MUTINY_SESSION.equals(sessionType)
			|| MUTINY_STATELESS_SESSION.equals(sessionType)
			|| UNI_MUTINY_SESSION.equals(sessionType)
			|| UNI_MUTINY_STATELESS_SESSION.equals(sessionType);
	}

	private static boolean usingStatelessSession(String sessionType) {
		return HIB_STATELESS_SESSION.equals(sessionType)
			|| MUTINY_STATELESS_SESSION.equals(sessionType)
			|| UNI_MUTINY_STATELESS_SESSION.equals(sessionType)
			|| SPRING_STATELESS_SESSION_PROVIDER.equals(sessionType);
	}

	private static boolean usingReactiveSessionAccess(String sessionType) {
		return UNI_MUTINY_SESSION.equals(sessionType)
			|| UNI_MUTINY_STATELESS_SESSION.equals(sessionType);
	}

	private boolean isLocal(Element methodOrParam) {
		return switch ( methodOrParam.getKind() ) {
			case PARAMETER -> element.getEnclosedElements().contains( methodOrParam.getEnclosingElement() );
			case METHOD, FIELD -> element.getEnclosedElements().contains( methodOrParam );
			default -> true;
		};
	}

	public void message(Element method, String message, Diagnostic.Kind severity) {
		if ( isLocal(method) ) {
			context.message(method, message, severity);
		}
		else {
			context.message(element, messageWithLocation(method, message), severity);
		}
	}

	public void message(Element method, AnnotationMirror mirror, String message, Diagnostic.Kind severity) {
		if ( isLocal(method) ) {
			context.message(method, mirror, message, severity);
		}
		else {
			context.message(element, messageWithLocation(method, message), severity);
		}
	}

	public void message(Element method, AnnotationMirror mirror, AnnotationValue value, String message, Diagnostic.Kind severity) {
		if ( isLocal(method) ) {
			context.message(method, mirror, value, message, severity);
		}
		else {
			context.message(element, messageWithLocation(method, message), severity);
		}
	}

	private static String messageWithLocation(Element element, String message) {
		return element.getKind() == ElementKind.PARAMETER
				? message + " for parameter '" + element.getSimpleName()
						+ "' of inherited member '" + element.getEnclosingElement().getSimpleName() + "'"
				: message + " for inherited member '" + element.getSimpleName() + "'";
	}

	@Override
	public List<AnnotationMirror> inheritedAnnotations() {
		if ( jakartaDataRepository ) {
			return element.getAnnotationMirrors().stream()
					.filter(annotationMirror -> hasAnnotation(annotationMirror.getAnnotationType().asElement(),
							"jakarta.interceptor.InterceptorBinding"))
					.collect(toList());
		}
		else {
			return emptyList();
		}
	}

	@Override
	public String javadoc() {
		if ( jakartaDataRepository ) {
			return "/**\n * Implements Jakarta Data repository {@link " + qualifiedName + "}\n **/";
		}
		else if ( repository ) {
			return "/**\n * Implements repository {@link " + qualifiedName + "}\n **/";
		}
		else if ( jakartaDataStaticModel ) {
			return "/**\n * Jakarta Data static metamodel for {@link " + qualifiedName + "}\n **/";
		}
		else {
			return "/**\n * Static metamodel for {@link " + qualifiedName + "}\n **/";
		}
	}
}
