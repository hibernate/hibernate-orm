/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.query.sqm.sql.ConversionException;
import org.hibernate.query.sqm.sql.SqlAstCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.sql.ast.tree.from.TableGroup;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class SqmSqlHelper {
	private SqmSqlHelper() {
	}

	public static TableGroup resolveTableGroup(SqmPath<?> sqmPath, SqlAstCreationState creationState) {
		SqmPath<?> lhs = sqmPath;
		while ( lhs.getReferencedPathSource().getSqmPathType() instanceof EmbeddableDomainType ) {
			lhs = lhs.getLhs();
		}

		final TableGroup tableGroup = creationState.getFromClauseAccess().findTableGroup( lhs.getNavigablePath() );
		if ( tableGroup != null ) {
			return tableGroup;
		}

		throw new ConversionException( "Could not locate TableGroup to use : " + lhs.getNavigablePath() );
	}
}
