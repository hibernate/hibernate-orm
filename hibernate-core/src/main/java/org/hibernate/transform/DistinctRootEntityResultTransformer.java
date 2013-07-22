/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.transform;
import java.util.List;

/**
 * Much like {@link RootEntityResultTransformer}, but we also distinct
 * the entity in the final result.
 * <p/>
 * Since this transformer is stateless, all instances would be considered equal.
 * So for optimization purposes we limit it to a single, singleton {@link #INSTANCE instance}.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class DistinctRootEntityResultTransformer implements TupleSubsetResultTransformer {

	public static final DistinctRootEntityResultTransformer INSTANCE = new DistinctRootEntityResultTransformer();

	/**
	 * Disallow instantiation of DistinctRootEntityResultTransformer.
	 */
	private DistinctRootEntityResultTransformer() {
	}

	/**
	 * Simply delegates to {@link RootEntityResultTransformer#transformTuple}.
	 *
	 * @param tuple The tuple to transform
	 * @param aliases The tuple aliases
	 * @return The transformed tuple row.
	 */
	@Override
	public Object transformTuple(Object[] tuple, String[] aliases) {
		return RootEntityResultTransformer.INSTANCE.transformTuple( tuple, aliases );
	}

	/**
	 * Simply delegates to {@link DistinctResultTransformer#transformList}.
	 *
	 * @param list The list to transform.
	 * @return The transformed List.
	 */
	@Override
	public List transformList(List list) {
		return DistinctResultTransformer.INSTANCE.transformList( list );
	}

	@Override
	public boolean[] includeInTransform(String[] aliases, int tupleLength) {
		return RootEntityResultTransformer.INSTANCE.includeInTransform( aliases, tupleLength );
	}

	@Override
	public boolean isTransformedValueATupleElement(String[] aliases, int tupleLength) {
		return RootEntityResultTransformer.INSTANCE.isTransformedValueATupleElement( null, tupleLength );
	}

	/**
	 * Serialization hook for ensuring singleton uniqueing.
	 *
	 * @return The singleton instance : {@link #INSTANCE}
	 */
	private Object readResolve() {
		return INSTANCE;
	}

}
