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
	public void testHqlListIsAnUnmodifiableCopy() {
		Object[] row = { 1L, null };
		List<Object> result = RowTransformerListImpl.instance().transformRow( row );
		row[0] = 2L;
		row[1] = "changed";
		assertEquals( 2, result.size() );
		assertEquals( 1L, result.get( 0 ) );
		assertNull( result.get( 1 ) );
		assertThrows( UnsupportedOperationException.class, () -> result.set( 0, 3L ) );
		assertThrows( UnsupportedOperationException.class, () -> result.add( 3L ) );
		assertThrows( UnsupportedOperationException.class, () -> result.remove( 0 ) );
	}

	@Test
	public void testNativeListIsAnUnmodifiableCopy() {
		Object[] tuple = { 1L, null };
		List<Object> result = NativeQueryListTransformer.INSTANCE.transformTuple(
				tuple, new String[] { "id", "name" } );
		tuple[0] = 2L;
		tuple[1] = "changed";
		assertEquals( 2, result.size() );
		assertEquals( 1L, result.get( 0 ) );
		assertNull( result.get( 1 ) );
		assertThrows( UnsupportedOperationException.class, () -> result.set( 0, 3L ) );
		assertThrows( UnsupportedOperationException.class, () -> result.add( 3L ) );
		assertThrows( UnsupportedOperationException.class, () -> result.remove( 0 ) );
	}
}
