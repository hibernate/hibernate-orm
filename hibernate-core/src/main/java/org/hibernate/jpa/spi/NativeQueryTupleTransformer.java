package org.hibernate.jpa.spi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.hibernate.HibernateException;
import org.hibernate.transform.BasicTransformerAdapter;

/**
 * ResultTransformer adapter for handling Tuple results from Native queries
 *
 * @author Arnold Galovics
 */
public class NativeQueryTupleTransformer extends BasicTransformerAdapter {

	@Override
	public Object transformTuple(Object[] tuple, String[] aliases) {
		return new NativeTupleImpl( tuple, aliases );
	}

	private static class NativeTupleElementImpl<X> implements TupleElement<X> {

		private final Class<? extends X> javaType;

		private final String alias;

		public NativeTupleElementImpl(Class<? extends X> javaType, String alias) {
			this.javaType = javaType;
			this.alias = alias;
		}

		@Override
		public Class<? extends X> getJavaType() {
			return javaType;
		}

		@Override
		public String getAlias() {
			return alias;
		}
	}

	private static class NativeTupleImpl implements Tuple {

		private Object[] tuple;

		private Map<String, Object> aliasToValue = new LinkedHashMap<>();

		public NativeTupleImpl(Object[] tuple, String[] aliases) {
			if ( tuple == null || aliases == null || tuple.length != aliases.length ) {
				throw new HibernateException( "Got different size of tuples and aliases" );
			}
			this.tuple = tuple;
			for ( int i = 0; i < tuple.length; i++ ) {
				aliasToValue.put( aliases[i].toLowerCase(), tuple[i] );
			}
		}

		@Override
		public <X> X get(String alias, Class<X> type) {
			final Object untyped = get( alias );

			return ( untyped != null ) ? type.cast( untyped ) : null;
		}

		@Override
		public Object get(String alias) {
			Object tupleElement = aliasToValue.get( alias.toLowerCase() );

			if ( tupleElement == null ) {
				throw new IllegalArgumentException( "Unknown alias [" + alias + "]" );
			}
			return tupleElement;
		}

		@Override
		public <X> X get(int i, Class<X> type) {
			final Object untyped = get( i );

			return ( untyped != null ) ? type.cast( untyped ) : null;
		}

		@Override
		public Object get(int i) {
			if ( i < 0 ) {
				throw new IllegalArgumentException( "requested tuple index must be greater than zero" );
			}
			if ( i >= aliasToValue.size() ) {
				throw new IllegalArgumentException( "requested tuple index exceeds actual tuple size" );
			}
			return tuple[i];
		}

		@Override
		public Object[] toArray() {
			// todo : make a copy?
			return tuple;
		}

		@Override
		public List<TupleElement<?>> getElements() {
			List<TupleElement<?>> elements = new ArrayList<>( aliasToValue.size() );

			for ( Map.Entry<String, Object> entry : aliasToValue.entrySet() ) {
				elements.add( new NativeTupleElementImpl<>( entry.getValue().getClass(), entry.getKey() ) );
			}
			return elements;
		}

		@Override
		public <X> X get(TupleElement<X> tupleElement) {
			return get( tupleElement.getAlias(), tupleElement.getJavaType() );
		}
	}
}
