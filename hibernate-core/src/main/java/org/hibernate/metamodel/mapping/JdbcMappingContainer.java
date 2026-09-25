package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;

import org.hibernate.spi.IndexedConsumer;

/**
 * Container for one-or-more JdbcMappings
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
public interface JdbcMappingContainer {
	/**
	 * The number of JDBC mappings
	 */
	default int getJdbcTypeCount() {
		return forEachJdbcType( (index, jdbcMapping) -> {} );
	}

	@Nonnull
	JdbcMapping getJdbcMapping(int index);

	@Nonnull
	default JdbcMapping getSingleJdbcMapping() {
		assert getJdbcTypeCount() == 1;
		return getJdbcMapping( 0 );
	}

	/**
	 * Visit each of JdbcMapping
	 *
	 * @apiNote Same as {@link #forEachJdbcType(int, IndexedConsumer)} starting from `0`
	 */
	default int forEachJdbcType(@Nonnull IndexedConsumer<JdbcMapping> action) {
		return forEachJdbcType( 0, action );
	}

	/**
	 * Visit each JdbcMapping starting from the given offset
	 */
	int forEachJdbcType(int offset, @Nonnull IndexedConsumer<JdbcMapping> action);
}
