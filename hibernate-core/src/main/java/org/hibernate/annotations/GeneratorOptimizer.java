package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.Incubating;
import org.hibernate.id.enhanced.StandardOptimizerDescriptor;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/// Explicitly selects a standard optimizer to use with identifier generation.
///
/// ```java
/// @Id
/// @GeneratedValue(strategy = GenerationType.SEQUENCE)
/// @SequenceGenerator(sequenceName = "optimized_sequence", allocationSize = 20)
/// @GeneratorOptimizer(StandardOptimizerDescriptor.POOLED_LO)
/// ```
///
/// Intended for use with [jakarta.persistence.GeneratedValue], usually
/// in combination with [jakarta.persistence.SequenceGenerator] or
/// [jakarta.persistence.TableGenerator].
///
/// @author Gavin King
/// @author Steve Ebersole
/// @since 8.0
@Target({METHOD, FIELD})
@Retention(RUNTIME)
@Incubating(since = "8.0", group = "optimizer-ann")
public @interface GeneratorOptimizer {
	/// The optimization strategy to apply to generation.
	StandardOptimizerDescriptor value();
}
