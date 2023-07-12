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
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.jpamodelgen.Context;
import org.hibernate.jpamodelgen.ImportContextImpl;
import org.hibernate.jpamodelgen.ProcessLaterException;
import org.hibernate.jpamodelgen.model.ImportContext;
import org.hibernate.jpamodelgen.model.MetaAttribute;
import org.hibernate.jpamodelgen.model.Metamodel;
import org.hibernate.jpamodelgen.util.AccessType;
import org.hibernate.jpamodelgen.util.AccessTypeInformation;
import org.hibernate.jpamodelgen.util.Constants;
import org.hibernate.jpamodelgen.validation.ProcessorSessionFactory;
import org.hibernate.jpamodelgen.validation.Validation;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static org.hibernate.jpamodelgen.annotation.QueryMethod.isOrderParam;
import static org.hibernate.jpamodelgen.annotation.QueryMethod.isPageParam;
import static org.hibernate.jpamodelgen.util.NullnessUtil.castNonNull;
import static org.hibernate.jpamodelgen.util.TypeUtils.containsAnnotation;
import static org.hibernate.jpamodelgen.util.TypeUtils.determineAccessTypeForHierarchy;
import static org.hibernate.jpamodelgen.util.TypeUtils.determineAnnotationSpecifiedAccessType;
import static org.hibernate.jpamodelgen.util.TypeUtils.getAnnotationMirror;
import static org.hibernate.jpamodelgen.util.TypeUtils.getAnnotationValue;
import static org.hibernate.jpamodelgen.util.TypeUtils.getAnnotationValueRef;

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
 */
public class AnnotationMetaEntity extends AnnotationMeta {

	private final ImportContext importContext;
	private final TypeElement element;
	private final Map<String, MetaAttribute> members;
	private final Context context;

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
	private boolean dao = false;

	/**
	 * The type of the "session getter" method of a DAO-style repository.
	 */
	private String sessionType = Constants.ENTITY_MANAGER;

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

	@Override
	public final Context getContext() {
		return context;
	}

	@Override
	public boolean isImplementation() {
		return dao;
	}

	@Override
	public final String getSimpleName() {
		return element.getSimpleName().toString();
	}

	@Override
	public final String getQualifiedName() {
		return element.getQualifiedName().toString();
	}

	@Override
	public final String getPackageName() {
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
			this.entityToMerge = other;
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
	boolean belongsToDao() {
		return dao;
	}

	@Override
	String getSessionType() {
		return sessionType;
	}

	@Override
	public boolean isInjectable() {
		return dao;
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

	protected final void init() {
		getContext().logMessage( Diagnostic.Kind.OTHER, "Initializing type '" + getQualifiedName() + "'" );

		determineAccessTypeForHierarchy( element, context );
		entityAccessTypeInfo = castNonNull( context.getAccessTypeInfo( getQualifiedName() ) );

		final List<VariableElement> fieldsOfClass = fieldsIn( element.getEnclosedElements() );
		final List<ExecutableElement> methodsOfClass = methodsIn( element.getEnclosedElements() );
		final List<ExecutableElement> gettersAndSettersOfClass = new ArrayList<>();
		final List<ExecutableElement> queryMethods = new ArrayList<>();
		for ( ExecutableElement method: methodsOfClass ) {
			if ( isGetterOrSetter( method ) ) {
				gettersAndSettersOfClass.add( method );
			}
			else if ( containsAnnotation( method, Constants.HQL, Constants.SQL, Constants.FIND ) ) {
				queryMethods.add( method );
			}
		}

		findSessionGetter( methodsOfClass );

		addPersistentMembers( fieldsOfClass, AccessType.FIELD );
		addPersistentMembers( gettersAndSettersOfClass, AccessType.PROPERTY );

		addAuxiliaryMembers();

		checkNamedQueries();

		addQueryMethods( queryMethods );

		initialized = true;
	}

	private void findSessionGetter(List<ExecutableElement> methodsOfClass) {
		if ( !containsAnnotation( element, Constants.ENTITY ) ) {
			for ( ExecutableElement method : methodsOfClass ) {
				if ( isSessionGetter( method ) ) {
					dao = true;
					sessionType = addDaoConstructor( method );
				}
			}
		}
	}

	/**
	 * If there is a session getter method, we generate an instance
	 * variable backing it, together with a constructor that initializes
	 * it.
	 */
	private String addDaoConstructor(ExecutableElement method) {
		final String name = method.getSimpleName().toString();
		final String typeName = element.getSimpleName().toString() + '_';
		final String sessionType = method.getReturnType().toString();
		putMember( name,
				new DaoConstructor(
						this,
						typeName,
						name,
						sessionType,
						context.addInjectAnnotation(),
						context.addNonnullAnnotation()
				)
		);
		return sessionType;
	}

	/**
	 * The session getter method doesn't have to be a JavaBeans-style
	 * getter. It can be any method with no parameters and one of the
	 * needed return types.
	 */
	private static boolean isSessionGetter(ExecutableElement method) {
		if ( method.getParameters().isEmpty() ) {
			final TypeMirror type = method.getReturnType();
			if ( type.getKind() == TypeKind.DECLARED ) {
				final DeclaredType declaredType = (DeclaredType) type;
				final Element element = declaredType.asElement();
				if ( element.getKind() == ElementKind.INTERFACE ) {
					final Name name = ((TypeElement) element).getQualifiedName();
					return name.contentEquals(Constants.HIB_SESSION)
						|| name.contentEquals(Constants.HIB_STATELESS_SESSION)
						|| name.contentEquals(Constants.MUTINY_SESSION)
						|| name.contentEquals(Constants.ENTITY_MANAGER);
				}
			}
		}
		return false;
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

	private static boolean isGetter(Name methodSimpleName, List<? extends TypeMirror> methodParameterTypes, TypeMirror returnType) {
		return ( methodSimpleName.subSequence(0,3).toString().equals("get")
				|| methodSimpleName.subSequence(0,2).toString().equals("is") )
			&& methodParameterTypes.isEmpty()
			&& returnType.getKind() != TypeKind.VOID;
	}

	private static boolean isSetter(Name methodSimpleName, List<? extends TypeMirror> methodParameterTypes, TypeMirror returnType) {
		return methodSimpleName.subSequence(0,3).toString().equals("set")
			&& methodParameterTypes.size() == 1
			&& returnType.getKind() != TypeKind.VOID;
	}

	private void addPersistentMembers(List<? extends Element> membersOfClass, AccessType membersKind) {
		for ( Element memberOfClass : membersOfClass ) {
			if ( isPersistent( memberOfClass, membersKind ) ) {
				final AnnotationMetaAttribute result =
						memberOfClass.asType()
								.accept( new MetaAttributeGenerationVisitor( this, context ), memberOfClass );
				if ( result != null ) {
					members.put( result.getPropertyName(), result );
				}
			}
		}
	}

	private boolean isPersistent(Element memberOfClass, AccessType membersKind) {
		return ( entityAccessTypeInfo.getAccessType() == membersKind
					|| determineAnnotationSpecifiedAccessType( memberOfClass ) != null )
			&& !containsAnnotation( memberOfClass, Constants.TRANSIENT )
			&& !memberOfClass.getModifiers().contains( Modifier.TRANSIENT )
			&& !memberOfClass.getModifiers().contains( Modifier.STATIC );
	}

	private void addQueryMethods(List<ExecutableElement> queryMethods) {
		for ( ExecutableElement method : queryMethods) {
			if ( method.getModifiers().contains(Modifier.ABSTRACT) ) {
				addQueryMethod( method );
			}
		}
	}

	private void addQueryMethod(ExecutableElement method) {
		final TypeMirror returnType = method.getReturnType();
		if ( returnType.getKind() == TypeKind.DECLARED ) {
			final DeclaredType declaredType = ununi((DeclaredType) returnType);
			final TypeElement typeElement = (TypeElement) declaredType.asElement();
			final List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
			switch ( typeArguments.size() ) {
				case 0:
					if ( containsAnnotation( declaredType.asElement(), Constants.ENTITY ) ) {
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
					if ( isLegalGenericResultType( typeElement.toString() ) ) {
						addQueryMethod( method, typeArguments.get(0), typeElement );
					}
					else {
						context.message( method,
								"incorrect return type '" + typeElement + "'",
								Diagnostic.Kind.ERROR );
					}
					break;
				default:
					context.message( method,
							"incorrect return type '" + declaredType + "'",
							Diagnostic.Kind.ERROR );
					break;
			}
		}
	}

	private static DeclaredType ununi(DeclaredType returnType) {
		final TypeElement typeElement = (TypeElement) returnType.asElement();
		return typeElement.getQualifiedName().contentEquals(Constants.UNI)
				? (DeclaredType) returnType.getTypeArguments().get(0)
				: returnType;
	}

	private static boolean isLegalRawResultType(String containerTypeName) {
		return containerTypeName.equals(Constants.LIST)
			|| containerTypeName.equals(Constants.QUERY)
			|| containerTypeName.equals(Constants.HIB_QUERY);
	}

	private static boolean isLegalGenericResultType(String containerTypeName) {
		return containerTypeName.equals(Constants.LIST)
			|| containerTypeName.equals(Constants.TYPED_QUERY)
			|| containerTypeName.equals(Constants.HIB_QUERY)
			|| containerTypeName.equals(Constants.HIB_SELECTION_QUERY);
	}

	private void addQueryMethod(
			ExecutableElement method,
			@Nullable TypeMirror returnType,
			@Nullable TypeElement containerType) {
		final AnnotationMirror hql = getAnnotationMirror( method, Constants.HQL );
		if ( hql != null ) {
			addQueryMethod( method, returnType, containerType, hql, false );
		}
		final AnnotationMirror sql = getAnnotationMirror( method, Constants.SQL );
		if ( sql != null ) {
			addQueryMethod( method, returnType, containerType, sql, true );
		}
		final AnnotationMirror find = getAnnotationMirror( method, Constants.FIND );
		if ( find != null ) {
			addFinderMethod( method, returnType, containerType );
		}
	}

	private void addFinderMethod(
			ExecutableElement method,
			@Nullable TypeMirror returnType,
			@Nullable TypeElement containerType) {
		if ( returnType == null || returnType.getKind() != TypeKind.DECLARED ) {
			context.message( method,
					"incorrect return type '" + returnType + "' is not an entity type",
					Diagnostic.Kind.ERROR );
		}
		else {
			final DeclaredType declaredType = ununi( (DeclaredType) returnType );
			final TypeElement entity = (TypeElement) declaredType.asElement();
			if ( !containsAnnotation( entity, Constants.ENTITY ) ) {
				context.message( method,
						"incorrect return type '" + returnType + "' is not annotated '@Entity'",
						Diagnostic.Kind.ERROR );
			}
			else {
				if ( containerType != null ) {
					// multiple results, it has to be a criteria finder
					createCriteriaFinder( method, returnType, containerType, entity );
				}
				else {
					switch ( method.getParameters().size() ) {
						case 0:
							context.message( method, "missing parameter", Diagnostic.Kind.ERROR );
							break;
						case 1:
							createSingleParameterFinder( method, returnType, entity );
							break;
						default:
							createMultipleParameterFinder( method, returnType, entity );
					}
				}
			}
		}
	}

	/**
	 * Create a finder method which returns multiple results.
	 */
	private void createCriteriaFinder(
			ExecutableElement method, TypeMirror returnType, @Nullable TypeElement containerType, TypeElement entity) {
		final String methodName = method.getSimpleName().toString();
		final List<String> paramNames = parameterNames(method);
		final List<String> paramTypes = parameterTypes(method);
		final String methodKey = methodName + paramTypes;
		for ( VariableElement param : method.getParameters() ) {
			validateFinderParameter(entity, param );
		}
		putMember( methodKey,
				new CriteriaFinderMethod(
						this,
						methodName,
						returnType.toString(),
						containerType == null ? null : containerType.toString(),
						paramNames,
						paramTypes,
						false,
						dao,
						sessionType,
						enabledFetchProfiles( method ),
						context.addNonnullAnnotation()
				)
		);
	}

	private static List<String> enabledFetchProfiles(ExecutableElement method) {
		final Object enabledFetchProfiles =
				getAnnotationValue( castNonNull( getAnnotationMirror( method, Constants.FIND ) ),
						"enabledFetchProfiles" );
		if ( enabledFetchProfiles == null ) {
			return emptyList();
		}
		else {
			@SuppressWarnings("unchecked")
			final List<AnnotationValue> annotationValues = (List<AnnotationValue>) enabledFetchProfiles;
			final List<String> result = annotationValues.stream().map(AnnotationValue::toString).collect(toList());
			if ( result.stream().anyMatch("<error>"::equals) ) {
				throw new ProcessLaterException();
			}
			return result;
		}
	}

	private void createMultipleParameterFinder(ExecutableElement method, TypeMirror returnType, TypeElement entity) {
		final String methodName = method.getSimpleName().toString();
		final List<String> paramNames = parameterNames( method );
		final List<String> paramTypes = parameterTypes( method );
		final String methodKey = methodName + paramTypes;
		if (  !usingStatelessSession() // no byNaturalId() lookup API for SS
				&& matchesNaturalKey( method, entity ) ) {
			putMember( methodKey,
					new NaturalIdFinderMethod(
							this,
							methodName,
							returnType.toString(),
							paramNames,
							paramTypes,
							dao,
							sessionType,
							enabledFetchProfiles( method ),
							context.addNonnullAnnotation()
					)
			);
		}
		else {
			putMember( methodKey,
					new CriteriaFinderMethod(
							this,
							methodName,
							returnType.toString(),
							null,
							paramNames,
							paramTypes,
							false,
							dao,
							sessionType,
							enabledFetchProfiles( method ),
							context.addNonnullAnnotation()
					)
			);
		}
	}

	private void createSingleParameterFinder(ExecutableElement method, TypeMirror returnType, TypeElement entity) {
		final String methodName = method.getSimpleName().toString();
		final VariableElement parameter = method.getParameters().get(0);
		final FieldType fieldType = validateFinderParameter( entity, parameter );
		if ( fieldType != null ) {
			final String methodKey = methodName + "!";
			final List<String> profiles = enabledFetchProfiles( method );
			switch ( pickStrategy( fieldType, profiles ) ) {
				case ID:
					putMember( methodKey,
							new IdFinderMethod(
									this,
									methodName,
									returnType.toString(),
									parameter.getSimpleName().toString(),
									parameter.asType().toString(),
									dao,
									sessionType,
									profiles,
									context.addNonnullAnnotation()
							)
					);
					break;
				case NATURAL_ID:
					putMember( methodKey,
							new NaturalIdFinderMethod(
									this,
									methodName,
									returnType.toString(),
									List.of( parameter.getSimpleName().toString() ),
									List.of( parameter.asType().toString() ),
									dao,
									sessionType,
									profiles,
									context.addNonnullAnnotation()
							)
					);
					break;
				case BASIC:
					putMember( methodKey,
							new CriteriaFinderMethod(
									this,
									methodName,
									returnType.toString(),
									null,
									List.of( parameter.getSimpleName().toString() ),
									List.of( parameter.asType().toString() ),
									fieldType == FieldType.ID,
									dao,
									sessionType,
									profiles,
									context.addNonnullAnnotation()
							)
					);
					break;
			}
		}
	}

	private FieldType pickStrategy(FieldType fieldType, List<String> profiles) {
		switch (fieldType) {
			case ID:
				// no byId() API for SS or M.S, only get()
				return (usingStatelessSession() || usingReactiveSession()) && !profiles.isEmpty()
						? FieldType.BASIC : FieldType.ID;
			case NATURAL_ID:
				// no byNaturalId() lookup API for SS
				// no byNaturalId() in M.S, but we do have Identifier workaround
				return usingStatelessSession() || (usingReactiveSession() && !profiles.isEmpty())
						? FieldType.BASIC : FieldType.NATURAL_ID;
			default:
				return FieldType.BASIC;
		}
	}

	private boolean matchesNaturalKey(ExecutableElement method, TypeElement entity) {
		boolean result = true;
		List<? extends VariableElement> parameters = method.getParameters();
		for ( VariableElement param : parameters) {
			if ( validateFinderParameter( entity, param ) != FieldType.NATURAL_ID ) {
				// no short-circuit here because we want to validate
				// all of them and get the nice error report
				result = false;
			}
		}
		return result && countNaturalIdFields( entity ) == parameters.size() ;
	}

	enum FieldType {
		ID, NATURAL_ID, BASIC
	}

	private int countNaturalIdFields(TypeElement entity) {
		int count = 0;
		for ( Element member : entity.getEnclosedElements() ) {
			if ( containsAnnotation( member, Constants.NATURAL_ID ) ) {
				count ++;
			}
		}
		return count;
	}

	private @Nullable FieldType validateFinderParameter(TypeElement entity, VariableElement param) {
		for ( Element member : entity.getEnclosedElements() ) {
			if ( member.getSimpleName().contentEquals( param.getSimpleName() ) ) {
				final String memberType = member.asType().toString();
				final String paramType = param.asType().toString();
				if ( !memberType.equals(paramType) ) {
					context.message( param,
							"matching field has type '" + memberType
									+ "' in entity class '" + entity.getQualifiedName() + "'",
							Diagnostic.Kind.ERROR );
				}
				if ( containsAnnotation( member, Constants.ID, Constants.EMBEDDED_ID ) ) {
					return FieldType.ID;
				}
				else if ( containsAnnotation( member, Constants.NATURAL_ID ) ) {
					return FieldType.NATURAL_ID;
				}
				else {
					return FieldType.BASIC;
				}
			}
		}
		final AnnotationMirror idClass = getAnnotationMirror( entity, Constants.ID_CLASS );
		if ( idClass != null ) {
			final Object value = getAnnotationValue( idClass, "value" );
			if ( value instanceof TypeMirror ) {
				if ( context.getTypeUtils().isSameType( param.asType(), (TypeMirror) value ) ) {
					return FieldType.ID;
				}
			}
		}
		context.message( param,
				"no matching field named '" + param.getSimpleName()
						+ "' in entity class '" + entity.getQualifiedName() + "'",
				Diagnostic.Kind.ERROR );
		return null;
	}

	private void addQueryMethod(
			ExecutableElement method,
			@Nullable TypeMirror returnType,
			@Nullable TypeElement containerType,
			AnnotationMirror mirror,
			boolean isNative) {
		final AnnotationValue value = getAnnotationValueRef( mirror, "value" );
		if ( value != null ) {
			final Object query = value.getValue();
			if ( query instanceof String ) {
				final String hql = (String) query;
				final List<String> paramNames = parameterNames( method );
				final List<String> paramTypes = parameterTypes( method );

				final QueryMethod attribute =
						new QueryMethod(
								this,
								method.getSimpleName().toString(),
								hql,
								returnType == null ? null : returnType.toString(),
								containerType == null ? null : containerType.getQualifiedName().toString(),
								paramNames,
								paramTypes,
								isNative,
								dao,
								sessionType,
								context.addNonnullAnnotation()
						);
				putMember( attribute.getPropertyName() + paramTypes, attribute );

				if ( !isNative ) {
					final SqmStatement<?> statement =
							Validation.validate(
									hql,
									true,
									new ErrorHandler( context, method, mirror, value, hql ),
									ProcessorSessionFactory.create( context.getProcessingEnvironment() )
							);
					if ( statement != null ) {
						for ( SqmParameter<?> param : statement.getSqmParameters() ) {
							checkParameter( param, paramNames, paramTypes, method, mirror, value );
						}
					}
				}
				//TODO: for SQL queries check that there is a method parameter for every query parameter

				// now check that the query has a parameter for every method parameter
				checkParameters( method, paramNames, paramTypes, mirror, value, hql );
			}
		}
	}

	private void checkParameter(
			SqmParameter<?> param, List<String> paramNames, List<String> paramTypes,
			ExecutableElement method, AnnotationMirror mirror, AnnotationValue value) {
		final SqmExpressible<?> expressible = param.getExpressible();
		final String queryParamType = expressible == null ? "unknown" : expressible.getTypeName(); //getTypeName() can return "unknown"
		if ( param.getName() != null ) {
			final String name = param.getName();
			int index = paramNames.indexOf( name );
			if ( index < 0 ) {
				context.message( method, mirror, value,
						"missing method parameter for query parameter :" + name
						+ " (add a parameter '" + queryParamType + ' ' + name + "' to '" + method.getSimpleName() + "')",
						Diagnostic.Kind.ERROR );
			}
			else if ( !isLegalAssignment( paramTypes.get(index), queryParamType ) ) {
				context.message( method, mirror, value,
						"parameter matching query parameter :" + name + " has the wrong type"
								+ " (change the method parameter type to '" + queryParamType + "')",
						Diagnostic.Kind.ERROR );
			}
		}
		else if ( param.getPosition() != null ) {
			int position = param.getPosition();
			if ( position > paramNames.size() ) {
				context.message( method, mirror, value,
						"missing method parameter for query parameter ?" + position
								+ " (add a parameter of type '" + queryParamType + "' to '" + method.getSimpleName() + "')",
						Diagnostic.Kind.ERROR );
			}
			else if ( !isLegalAssignment( paramTypes.get(position-1), queryParamType ) ) {
				context.message( method, mirror, value,
						"parameter matching query parameter ?" + position + " has the wrong type"
								+ " (change the method parameter type to '" + queryParamType + "')",
						Diagnostic.Kind.ERROR );
			}
		}
	}

	private static boolean isLegalAssignment(String argType, String paramType) {
		return paramType.equals("unknown")
			|| paramType.equals(argType)
			|| paramType.equals(fromPrimitive(argType));
	}

	private static @Nullable String fromPrimitive(String argType) {
		switch (argType) {
			case "boolean":
				return Boolean.class.getName();
			case "char":
				return Character.class.getName();
			case "int":
				return Integer.class.getName();
			case "long":
				return Long.class.getName();
			case "short":
				return Short.class.getName();
			case "byte":
				return Byte.class.getName();
			case "float":
				return Float.class.getName();
			case "double":
				return Double.class.getName();
			default:
				return null;
		}
	}

	private static List<String> parameterTypes(ExecutableElement method) {
		return method.getParameters().stream()
				.map(param -> param.asType().toString())
				.collect(toList());
	}

	private static List<String> parameterNames(ExecutableElement method) {
		return method.getParameters().stream()
				.map(param -> param.getSimpleName().toString())
				.collect(toList());
	}

	private void checkParameters(
			ExecutableElement method,
			List<String> paramNames, List<String> paramTypes,
			AnnotationMirror mirror,
			AnnotationValue value,
			String hql) {
		for (int i = 1; i <= paramNames.size(); i++) {
			final String param = paramNames.get(i-1);
			final String type = paramTypes.get(i-1);
			if ( parameterIsMissing( hql, i, param, type ) ) {
				context.message( method, mirror, value,
						"missing query parameter for '" + param
								+ "' (no parameter named :" + param + " or ?" + i + ")",
						Diagnostic.Kind.ERROR );
			}
		}
	}

	private static boolean parameterIsMissing(String hql, int i, String param, String type) {
		return !hql.matches(".*(:" + param + "|\\?" + i + ")\\b.*")
			&& !isPageParam(type)
			&& !isOrderParam(type);
	}

	private boolean usingReactiveSession() {
		return Constants.MUTINY_SESSION.equals(sessionType);
	}

	private boolean usingStatelessSession() {
		return Constants.HIB_STATELESS_SESSION.equals(sessionType);
	}
}
