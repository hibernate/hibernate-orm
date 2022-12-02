/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.tuple.Generator;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Meta-annotation used to mark another annotation as providing configuration
 * for a custom {@linkplain Generator identifier generator}. This is the best
 * way to work with customized identifier generation in Hibernate.
 * <p>
 * For example, if we have a custom identifier generator:
 * <pre>{@code
 * public class CustomSequenceGenerator implements InMemoryGenerator {
 *     public CustomSequenceGenerator(CustomSequence config, Member annotatedMember,
 *                                    CustomIdGeneratorCreationContext context) {
 *         ...
 *     }
 *     ...
 * }
 * }</pre>
 * Then we may also define an annotation which associates this generator with
 * an entity and supplies configuration parameters:
 * <pre>{@code
 * @IdGeneratorType(CustomSequenceGenerator.class)
 * @Retention(RUNTIME) @Target({METHOD,FIELD})
 * public @interface CustomSequence {
 *     String name();
 *     int startWith() default 1;
 *     int incrementBy() default 50;
 * }
 * }</pre>
 * and we may use it as follows:
 * <pre>{@code
 * @Id @CustomSequence(name = "mysequence", startWith = 0)
 * private Integer id;
 * }</pre>
 * We did not use the JPA-defined {@link jakarta.persistence.GeneratedValue}
 * here, since that API is designed around the use of stringly-typed names.
 * The {@code @CustomSequence} annotation itself implies that {@code id} is
 * a generated value.
 * <p>
 * For a more complete example, see the annotation {@link UuidGenerator} and
 * the corresponding generator class {@link org.hibernate.id.uuid.UuidGenerator}.
 * <p>
 * A {@code @IdGeneratorType} annotation must have retention policy
 * {@link RetentionPolicy#RUNTIME}.
 *
 * @see Generator
 * @see org.hibernate.tuple.AnnotationBasedGenerator
 *
 * @since 6.0
 */
@Target(ANNOTATION_TYPE)
@Retention(RUNTIME)
public @interface IdGeneratorType {
	/**
	 * A class which implements {@link Generator} and has a constructor with
	 * the signature:
	 * <pre>{@code
	 * public GeneratorType(AnnotationType config, Member idMember,
	 *                      CustomIdGeneratorCreationContext creationContext)
	 * }</pre>
	 * where {@code GeneratorType} is the class that implements {@code Generator},
	 * and {@code AnnotationType} is the annotation type to which this annotation
	 * was applied.
	 */
	Class<? extends Generator> value();
}
