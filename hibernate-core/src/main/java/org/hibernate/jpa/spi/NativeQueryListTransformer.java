package org.hibernate.jpa.spi;

import jakarta.annotation.Nonnull;

import org.hibernate.query.TupleTransformer;

import java.util.List;

/**
 * A {@link TupleTransformer} for handling {@link List} results from native queries.
 *
 * @since 6.3
 *
 * @author Gavin King
 */
public class NativeQueryListTransformer implements TupleTransformer<List<Object>> {

	public static final NativeQueryListTransformer INSTANCE = new NativeQueryListTransformer();

	@Override
	@Nonnull
	public List<Object> transformTuple(@Nonnull Object[] tuple, @Nonnull String[] aliases) {
		return List.of( tuple );
	}
}
