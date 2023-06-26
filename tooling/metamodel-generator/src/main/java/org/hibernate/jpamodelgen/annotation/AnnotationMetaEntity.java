/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.annotation;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.grammars.hql.HqlLexer;
import org.hibernate.grammars.hql.HqlParser;
import org.hibernate.jpamodelgen.Context;
import org.hibernate.jpamodelgen.ImportContextImpl;
import org.hibernate.jpamodelgen.model.ImportContext;
import org.hibernate.jpamodelgen.model.MetaAttribute;
import org.hibernate.jpamodelgen.model.Metamodel;
import org.hibernate.jpamodelgen.util.AccessType;
import org.hibernate.jpamodelgen.util.AccessTypeInformation;
import org.hibernate.jpamodelgen.util.Constants;
import org.hibernate.jpamodelgen.util.NullnessUtil;
import org.hibernate.jpamodelgen.util.TypeUtils;
import org.hibernate.query.hql.internal.HqlParseTreeBuilder;

import static java.util.stream.Collectors.toList;
import static org.hibernate.jpamodelgen.util.TypeUtils.containsAnnotation;
import static org.hibernate.jpamodelgen.util.TypeUtils.determineAnnotationSpecifiedAccessType;
import static org.hibernate.jpamodelgen.util.TypeUtils.getAnnotationMirror;
import static org.hibernate.jpamodelgen.util.TypeUtils.getAnnotationValue;
import static org.hibernate.query.hql.internal.StandardHqlTranslator.prettifyAntlrError;

/**
 * Class used to collect meta information about an annotated type (entity, embeddable or mapped superclass).
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
	 * Embeddables and mapped superclasses need to be lazily initialized since the access type may be determined by
	 * the class which is embedding or subclassing the entity or superclass. This might not be known until
	 * annotations are processed.
	 * <p>
	 * Also note, that if two different classes with different access types embed this entity or extend this mapped
	 * super-class, the access type of the embeddable/superclass will be the one of the last embedding/subclassing
	 * entity processed. The result is not determined (that's ok according to the spec).
	 */
	private boolean initialized;

	/**
	 * Another meta entity for the same type which should be merged lazily with this meta entity. Doing the merge
	 * lazily is required for embeddedables and mapped supertypes to only pull in those members matching the access
	 * type as configured via the embedding entity or subclass (also see METAGEN-85).
	 */
	private Metamodel entityToMerge;

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
		PackageElement packageOf = context.getElementUtils().getPackageOf( element );
		return context.getElementUtils().getName( packageOf.getQualifiedName() ).toString();
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
		// store the entity in order do the merge lazily in case of a non-initialized embeddedable or mapped superclass
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
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "AnnotationMetaEntity" );
		sb.append( "{element=" ).append( element );
		sb.append( ", members=" ).append( members );
		sb.append( '}' );
		return sb.toString();
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
		List<ExecutableElement> queryMethods = new ArrayList<>();

		for ( Element rawMethodOfClass: methodsOfClass ) {
			if ( isGetterOrSetter( rawMethodOfClass ) ) {
				gettersAndSettersOfClass.add( rawMethodOfClass );
			}
			else if ( rawMethodOfClass instanceof ExecutableElement
					&& containsAnnotation( rawMethodOfClass, Constants.HQL, Constants.SQL ) ) {
				queryMethods.add( (ExecutableElement) rawMethodOfClass );
			}
		}
		addPersistentMembers( gettersAndSettersOfClass, AccessType.PROPERTY );

		addAuxiliaryMembers();

		addQueryMethods( queryMethods );

		initialized = true;
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

		return isSetter(methodSimpleName, methodParameterTypes, returnType)
			|| isGetter(methodSimpleName, methodParameterTypes, returnType);
	}

	private static boolean isGetter(String methodSimpleName, List<? extends TypeMirror> methodParameterTypes, TypeMirror returnType) {
		return (methodSimpleName.startsWith("get") || methodSimpleName.startsWith("is"))
			&& methodParameterTypes.isEmpty()
			&& !"void".equalsIgnoreCase(returnType.toString());
	}

	private static boolean isSetter(String methodSimpleName, List<? extends TypeMirror> methodParameterTypes, TypeMirror returnType) {
		return methodSimpleName.startsWith("set")
			&& methodParameterTypes.size() == 1
			&& "void".equalsIgnoreCase(returnType.toString());
	}

	private void addPersistentMembers(List<? extends Element> membersOfClass, AccessType membersKind) {
		for ( Element memberOfClass : membersOfClass ) {
			AccessType forcedAccessType = determineAnnotationSpecifiedAccessType( memberOfClass );
			if ( entityAccessTypeInfo.getAccessType() != membersKind && forcedAccessType == null ) {
				continue;
			}

			if ( containsAnnotation( memberOfClass, Constants.TRANSIENT )
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

	private void addQueryMethods(List<ExecutableElement> queryMethods) {
		for ( ExecutableElement method : queryMethods) {
			addQueryMethod( method );
		}
	}

	private void addQueryMethod(ExecutableElement method) {
		final String methodName = method.getSimpleName().toString();
		final TypeMirror returnType = method.getReturnType();
		if ( returnType instanceof DeclaredType ) {
			final DeclaredType declaredType = (DeclaredType) returnType;
			final List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
			if ( typeArguments.size() == 0 ) {
				final String typeName = declaredType.toString();
				if ( containsAnnotation( declaredType.asElement(), Constants.ENTITY ) ) {
					addQueryMethod(method, methodName, typeName, null);
				}
				else {
					if ( isLegalRawResultType( typeName ) ) {
						addQueryMethod( method, methodName, null, typeName );
					}
					else {
						// probably a projection
						addQueryMethod( method, methodName, typeName, null );
					}
				}
			}
			else if ( typeArguments.size() == 1 ) {
				final String containerTypeName = declaredType.asElement().toString();
				final String returnTypeName = typeArguments.get(0).toString();
				if ( isLegalGenericResultType( containerTypeName ) ) {
					addQueryMethod( method, methodName, returnTypeName, containerTypeName );
				}
				else {
					displayError( method, "incorrect return type '" + containerTypeName + "'" );
				}
			}
			else {
				displayError( method, "incorrect return type '" + declaredType + "'" );
			}
		}
	}

	private static boolean isLegalRawResultType(String containerTypeName) {
		return containerTypeName.equals("java.util.List")
			|| containerTypeName.equals("jakarta.persistence.Query")
			|| containerTypeName.equals("org.hibernate.query.Query");
	}

	private static boolean isLegalGenericResultType(String containerTypeName) {
		return containerTypeName.equals("java.util.List")
			|| containerTypeName.equals("jakarta.persistence.TypedQuery")
			|| containerTypeName.equals("org.hibernate.query.Query")
			|| containerTypeName.equals("org.hibernate.query.SelectionQuery");
	}

	private void addQueryMethod(
			ExecutableElement method,
			String methodName,
			@Nullable String returnTypeName,
			@Nullable String containerTypeName) {
		final AnnotationMirror hql = getAnnotationMirror( method, Constants.HQL );
		if ( hql != null ) {
			addQueryMethod( method, methodName, returnTypeName, containerTypeName, hql, false );
		}
		final AnnotationMirror sql = getAnnotationMirror( method, Constants.SQL );
		if ( sql != null ) {
			addQueryMethod( method, methodName, returnTypeName, containerTypeName, sql, true );
		}
	}

	private void addQueryMethod(
			ExecutableElement method,
			String methodName,
			@Nullable
			String returnTypeName,
			@Nullable
			String containerTypeName,
			AnnotationMirror mirror,
			boolean isNative) {
		final Object queryString = getAnnotationValue( mirror, "value" );
		if ( queryString instanceof String ) {
			final List<String> paramNames =
					method.getParameters().stream()
							.map( param -> param.getSimpleName().toString() )
							.collect( toList() );
			final List<String> paramTypes =
					method.getParameters().stream()
							.map( param -> param.asType().toString() )
							.collect( toList() );
			final String hql = (String) queryString;
			final QueryMethod attribute =
					new QueryMethod(
							this,
							methodName,
							hql,
							returnTypeName,
							containerTypeName,
							paramNames,
							paramTypes,
							isNative
					);
			putMember( attribute.getPropertyName(), attribute );

			checkParameters( method, paramNames, mirror, hql );
			if ( !isNative ) {
				checkHqlSyntax( method, mirror, hql );
			}
		}
	}

	private void checkParameters(ExecutableElement method, List<String> paramNames, AnnotationMirror mirror, String hql) {
		for (int i = 1; i <= paramNames.size(); i++) {
			final String param = paramNames.get(i-1);
			if ( !hql.contains(":" + param) && !hql.contains("?" + i) ) {
				displayError( method, mirror, "missing query parameter for '" + param
						+ "' (no parameter named :" + param + " or ?" + i + ")" );
			}
		}
	}

	private void checkHqlSyntax(ExecutableElement method, AnnotationMirror mirror, String queryString) {
		ANTLRErrorListener errorListener = new ANTLRErrorListener() {
			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String message, RecognitionException e) {
				displayError( method, mirror, "illegal HQL syntax - "
						+ prettifyAntlrError( offendingSymbol, line, charPositionInLine, message, e, queryString, false ) );
			}

			@Override
			public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact, BitSet ambigAlts, ATNConfigSet configs) {
			}

			@Override
			public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex, BitSet conflictingAlts, ATNConfigSet configs) {
			}

			@Override
			public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, int prediction, ATNConfigSet configs) {
			}
		};

		final HqlLexer hqlLexer = HqlParseTreeBuilder.INSTANCE.buildHqlLexer( queryString );
		final HqlParser hqlParser = HqlParseTreeBuilder.INSTANCE.buildHqlParser( queryString, hqlLexer );
		hqlLexer.addErrorListener( errorListener );
		hqlParser.getInterpreter().setPredictionMode( PredictionMode.SLL );
		hqlParser.removeErrorListeners();
		hqlParser.addErrorListener( errorListener );
		hqlParser.setErrorHandler( new BailErrorStrategy() );

		try {
			hqlParser.statement();
		}
		catch ( ParseCancellationException e) {
			// reset the input token stream and parser state
			hqlLexer.reset();
			hqlParser.reset();

			// fall back to LL(k)-based parsing
			hqlParser.getInterpreter().setPredictionMode( PredictionMode.LL );
			hqlParser.setErrorHandler( new DefaultErrorStrategy() );

			hqlParser.statement();
		}
	}

	private void displayError(ExecutableElement method, String message) {
		context.getProcessingEnvironment().getMessager()
				.printMessage( Diagnostic.Kind.ERROR, message, method );
	}
	private void displayError(ExecutableElement method, AnnotationMirror mirror, String message) {
		context.getProcessingEnvironment().getMessager()
				.printMessage( Diagnostic.Kind.ERROR, message, method, mirror,
						mirror.getElementValues().entrySet().stream()
								.filter( entry -> entry.getKey().getSimpleName().toString().equals("value") )
								.map(Map.Entry::getValue).findAny().orElseThrow() );
	}
}
