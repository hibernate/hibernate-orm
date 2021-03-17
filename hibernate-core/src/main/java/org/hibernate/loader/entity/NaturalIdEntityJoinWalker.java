/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.entity;

import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.OuterJoinLoadable;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_STRING_ARRAY;
import static org.hibernate.internal.util.collections.ArrayHelper.negate;

/**
 * An {@link EntityJoinWalker} that uses 'is null' predicates to match
 * null {@link org.hibernate.annotations.NaturalId} properties.
 *
 * @author Gavin King
 */
public class NaturalIdEntityJoinWalker extends EntityJoinWalker {

	private static String[] naturalIdColumns(Loadable persister, boolean[] valueNullness) {
		int i = 0;
		List<String> columns = new ArrayList<>();
		for ( int p : persister.getNaturalIdentifierProperties() ) {
			if ( !valueNullness[i++] ) {
				columns.addAll( asList( persister.getPropertyColumnNames(p) ) );
			}
		}
		return columns.toArray(EMPTY_STRING_ARRAY);
	}

	public NaturalIdEntityJoinWalker(
			OuterJoinLoadable persister,
			boolean[] valueNullness,
			int batchSize,
			LockOptions lockOptions,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		super(persister, naturalIdColumns( persister, valueNullness ), batchSize, lockOptions, factory, loadQueryInfluencers);
		StringBuilder sql = new StringBuilder( getSQLString() );
		for ( String nullCol : naturalIdColumns( getPersister(), negate( valueNullness ) ) ) {
			sql.append(" and ").append( getAlias() ).append('.').append( nullCol ).append(" is null");
		}
		setSql( sql.toString() );
	}
}
