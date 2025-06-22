/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.generator.AnnotationBasedGenerator;
import org.hibernate.generator.Generator;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Meta-annotation used to mark another annotation as providing configuration
 * for a custom {@linkplain Generator identifier generator}. This is the best
 * way to work with customized identifier generation in Hibernate.
 * <p>
 * For example, if we have a custom identifier generator:
 * <pre>
 * public class CustomSequenceGenerator implements BeforeExecutionGenerator {
 *     public CustomSequenceGenerator(CustomSequence config, Member annotatedMember,
 *                                    GeneratorCreationContext context) {
 *         ...
 *     }
 *     ...
 * }
 * </pre>
 * <p>
 * Then we may also define an annotation which associates this generator with
 * an entity and supplies configuration parameters:
 * <pre>
 * &#64;IdGeneratorType(CustomSequenceGenerator.class)
 * &#64;Retention(RUNTIME) @Target({METHOD,FIELD})
 * public @interface CustomSequence {
 *     String name();
 *     int startWith() default 1;
 *     int incrementBy() default 50;
 * }
 * </pre>
 * <p>
 * and we may use it as follows:
 * <pre>
 * &#64;Id &#64;CustomSequence(name = "mysequence", startWith = 0)
 * private Integer id;
 * </pre>
 * <p>
 * We did not use the JPA-defined {@link jakarta.persistence.GeneratedValue}
 * here, since that API is designed around the use of stringly-typed names.
 * The {@code @CustomSequence} annotation itself implies that {@code id} is
 * a generated value.
 * <p>
 * An id generator annotation may have members, which are used to configure
 * the id generator, if either:
 * <ul>
 * <li>the id generator implements {@link AnnotationBasedGenerator}, or
 * <li>the id generator class has a constructor with the same signature as
 *     {@link AnnotationBasedGenerator#initialize}.
 * </ul>
 * <p>
 * For a more complete example, see the annotation {@link UuidGenerator} and
 * the corresponding generator class {@link org.hibernate.id.uuid.UuidGenerator}.
 * <p>
 * A {@code @IdGeneratorType} annotation must have retention policy
 * {@link RetentionPolicy#RUNTIME}.
 * <p>
 * If a {@code Generator} may be used to generate values of non-identifier
 * fields, its generator annotation should also be meta-annotated
 * {@link ValueGenerationType @ValueGenerationType}.
 *
 * @see Generator
 * @see AnnotationBasedGenerator
 *
 * @since 6.0
 */
@Target(ANNOTATION_TYPE)
@Retention(RUNTIME)
public @interface IdGeneratorType {
	/**
	 * A class which implements {@link Generator}.
	 */
	Class<? extends Generator> value();
}
