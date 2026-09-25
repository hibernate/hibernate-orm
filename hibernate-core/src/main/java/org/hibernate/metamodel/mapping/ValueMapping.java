package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;

import java.util.Locale;

import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.JavaTypedExpressible;

/**
 * Describes a mapping related to any part of the app's domain model,
 * for example, an attribute, an entity identifier, collection elements, etc.
 * Used during generation of a SQL AST.
 *
 * @author Steve Ebersole
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
public interface ValueMapping extends MappingModelExpressible, JavaTypedExpressible {
	/**
	 * Descriptor for the type of this mapping
	 */
	@Nonnull
	MappingType getMappedType();

	@Nonnull
	@Override
	default JavaType<?> getExpressibleJavaType() {
		return getMappedType().getMappedJavaType();
	}

	/**
	 * Treat operation.  Asks the ValueMapping to treat itself as the
	 * given `targetType`, if it can.
	 *
	 * @apiNote This is not necessarily limited to things the ValueMapping
	 * itself implements.
	 *
	 * @implNote This default implementation is however limited to just
	 * things the ValueMapping itself implements.
	 *
	 */
	@Nonnull
	default <X> X treatAs(@Nonnull Class<X> targetType) {
		if ( targetType.isInstance( this ) ) {
			return targetType.cast( this );
		}

		throw new IllegalArgumentException(
				String.format(
						Locale.ROOT,
						"`%s` cannot be treated as `%s`",
						getClass().getName(),
						targetType.getName()
				)
		);
	}
}
