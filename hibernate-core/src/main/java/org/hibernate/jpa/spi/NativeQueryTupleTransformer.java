package org.hibernate.jpa.spi;

import org.hibernate.HibernateException;
import org.hibernate.transform.BasicTransformerAdapter;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ResultTransformer adapter for handling Tuple results from Native queries
 *
 * @author Arnold Galovics
 */
public class NativeQueryTupleTransformer extends BasicTransformerAdapter  {
    @Override
    public Object transformTuple(Object[] tuple, String[] aliases) {
        return new NativeTupleImpl(tuple, aliases);
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
        private LinkedHashMap<String, Object> aliasToValue = new LinkedHashMap<>();

        public NativeTupleImpl(Object[] tuple, String[] aliases) {
            if (tuple == null || aliases == null || tuple.length != aliases.length) {
                throw new HibernateException("Got different size of tuples and aliases");
            }
            this.tuple = tuple;
            for (int i = 0; i < tuple.length; i++) {
                aliasToValue.put(aliases[i], tuple[i]);
            }
        }

        @Override
        public <X> X get(String alias, Class<X> type) {
            final Object untyped = get( alias );
            if ( untyped != null ) {
                if ( !type.isInstance( untyped ) ) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "Requested tuple value [alias=%s, value=%s] cannot be assigned to requested type [%s]",
                                    alias,
                                    untyped,
                                    type.getName()
                            )
                    );
                }
            }
            return (X) untyped;
        }

        @Override
        public Object get(String alias) {
            Object tupleElement = aliasToValue.get( alias );
            if ( tupleElement == null ) {
                throw new IllegalArgumentException( "Unknown alias [" + alias + "]" );
            }
            return tupleElement;
        }

        @Override
        public <X> X get(int i, Class<X> type) {
            final Object result = get( i );
            if ( result != null && !type.isInstance( result ) ) {
                throw new IllegalArgumentException(
                        String.format(
                                "Requested tuple value [index=%s, realType=%s] cannot be assigned to requested type [%s]",
                                i,
                                result.getClass().getName(),
                                type.getName()
                        )
                );
            }
            return (X) result;
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
            return tuple;
        }

        @Override
        public List<TupleElement<?>> getElements() {
            List<TupleElement<?>> elements = new ArrayList<>(aliasToValue.size());
            for (Map.Entry<String, Object> entry : aliasToValue.entrySet()) {
                elements.add(new NativeTupleElementImpl<>(entry.getValue().getClass(), entry.getKey()));
            }
            return elements;
        }

        @Override
        public <X> X get(TupleElement<X> tupleElement) {
            return get( tupleElement.getAlias(), tupleElement.getJavaType() );
        }
    }
}
