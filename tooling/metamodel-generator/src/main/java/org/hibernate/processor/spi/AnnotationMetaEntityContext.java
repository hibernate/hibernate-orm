package org.hibernate.processor.spi;

import jakarta.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;

/**
 * Gives a {@link HibernateProcessorExtension} access to the state of the metamodel class
 * the processor is currently generating, and lets it add members to that class.
 * <p>
 * An instance is only valid during the call of the extension method it is passed to.
 */
@Incubating(since = "8.0", group = "processor-extension")
public interface AnnotationMetaEntityContext {

	/**
	 * The metamodel class being generated, for example to {@linkplain Metamodel#importType import
	 * types} so that they can be referenced by short name in generated code.
	 */
	Metamodel metamodel();

	/**
	 * Add a member to the generated metamodel class. If a member with the same name was
	 * already added, it is replaced.
	 *
	 * @param name the name under which the member is registered, which is also used to
	 * detect clashes with user-defined members, see {@link #hasMember(String)}
	 * @param attribute the code to generate
	 */
	void addMember(String name, MetaAttribute attribute);

	/**
	 * Add a member to the generated metamodel class, given as the source code of its declaration,
	 * for example {@code "public static String name() { return \"x\"; }"}. Types it refers to
	 * should be {@linkplain Metamodel#importType imported} when building the declaration. If a member
	 * with the same name was already added, it is replaced.
	 *
	 * @param name the name under which the member is registered, which is also used to
	 * detect clashes with user-defined members, see {@link #hasMember(String)}
	 * @param declaration the code to generate
	 */
	void addMember(String name, String declaration);

	/**
	 * Whether a member with the given name was already registered.
	 */
	boolean hasMember(String name);

	/**
	 * The type of the identifier of the {@linkplain #primaryEntity() primary entity},
	 * or {@code null} if it cannot be determined.
	 */
	@Nullable TypeMirror findIdType();

	/**
	 * The entity the generated repository is about: the entity type itself when generating an
	 * entity metamodel, or the entity a repository declares, or {@code null} if unknown.
	 */
	@Nullable TypeElement primaryEntity();

	/**
	 * Whether the type being processed is a Jakarta Data repository, that is, is annotated
	 * {@code @Repository}.
	 */
	boolean isJakartaDataRepository();

	/**
	 * Report a message through the annotation processing environment.
	 *
	 * @param element the element the message is about
	 * @param message the message
	 * @param kind the severity; {@link Diagnostic.Kind#ERROR} fails the compilation
	 */
	void message(Element element, String message, Diagnostic.Kind kind);

	/**
	 * Declare the Java expression that the generated query methods evaluate to obtain the session, for
	 * example {@code "SessionOperations.getSession()"}, whatever the
	 * {@linkplain SessionSetup#sessionType() type of the session}. Nothing is then generated to acquire or
	 * release a session. Use this instead of {@link #addRepositoryConstructor(String, String)}, not in
	 * addition to it, when the session is not injected.
	 *
	 * @param expression a Java expression of the session type
	 */
	void setSessionExpression(String expression);

	/**
	 * The return type of the given method as source code, with its type arguments, resolved
	 * in the context of the type being processed.
	 */
	String fullReturnType(ExecutableElement method);

	/**
	 * All members of the given type, including inherited ones.
	 */
	List<? extends Element> getAllMembers(TypeElement type);

	/**
	 * Generate a constructor which takes the session as parameter, annotated
	 * with {@code @Inject} if available, together with an accessor method returning it, and use it
	 * as the session of the repository.
	 *
	 * @param name the name of the session accessor method and of the registered member
	 * @param sessionType the fully qualified name of the type of the session
	 */
	void addRepositoryConstructor(String name, String sessionType);
}
