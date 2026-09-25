package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;

/**
 * Unifying contract for things that are capable of being an expression in
 * the SQL AST.
 *
 * @author Steve Ebersole
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
public interface SqlExpressible extends JdbcMappingContainer {
	/**
	 * Anything that is expressible at the SQL AST level
	 * would be of basic type.
	 */
	@Nullable
	JdbcMapping getJdbcMapping();

	@Nonnull
	@Override
	default JdbcMapping getJdbcMapping(int index) {
		final var jdbcMapping = getJdbcMapping();
		if ( index != 0 || jdbcMapping == null ) {
			throw new IndexOutOfBoundsException( index );
		}
		return jdbcMapping;
	}

}
