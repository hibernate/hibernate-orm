package org.hibernate.orm.test.query.hql;

import java.util.List;

import org.hibernate.jpa.spi.NativeQueryListTransformer;
import org.hibernate.sql.results.internal.RowTransformerListImpl;
import org.hibernate.testing.orm.junit.JiraKey;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JiraKey( "HHH-20328" )
public class ListResultTransformerTest {
	@Test
	public void testHqlListIsUnmodifiable() {
		Object[] row = { 1L, null };
		List<Object> result = RowTransformerListImpl.instance().transformRow( row );
		assertEquals( 2, result.size() );
		assertEquals( 1L, result.get( 0 ) );
		assertNull( result.get( 1 ) );
		assertThrows( UnsupportedOperationException.class, () -> result.set( 0, 3L ) );
		assertThrows( UnsupportedOperationException.class, () -> result.add( 3L ) );
		assertThrows( UnsupportedOperationException.class, () -> result.remove( 0 ) );
	}

	@Test
	public void testNativeListIsUnmodifiable() {
		Object[] tuple = { 1L, null };
		List<Object> result = NativeQueryListTransformer.INSTANCE.transformTuple(
				tuple, new String[] { "id", "name" } );
		assertEquals( 2, result.size() );
		assertEquals( 1L, result.get( 0 ) );
		assertNull( result.get( 1 ) );
		assertThrows( UnsupportedOperationException.class, () -> result.set( 0, 3L ) );
		assertThrows( UnsupportedOperationException.class, () -> result.add( 3L ) );
		assertThrows( UnsupportedOperationException.class, () -> result.remove( 0 ) );
	}
}
