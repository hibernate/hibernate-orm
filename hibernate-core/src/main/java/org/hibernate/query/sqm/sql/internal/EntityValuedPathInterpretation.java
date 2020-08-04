/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.TableGroup;

/**
 * @author Koen Aers
 */
public class EntityValuedPathInterpretation<T> extends AbstractSqmPathInterpretation<T> {

	public static <T> EntityValuedPathInterpretation<T> from(
			SqmEntityValuedSimplePath<T> sqmPath,
			SqlAstCreationState sqlAstCreationState) {
		final TableGroup tableGroup = sqlAstCreationState
				.getFromClauseAccess()
				.findTableGroup( sqmPath.getLhs().getNavigablePath() );
		final EntityValuedModelPart mapping = (EntityValuedModelPart) tableGroup
				.getModelPart()
				.findSubPart( sqmPath.getReferencedPathSource().getPathName(), null );
		return new EntityValuedPathInterpretation<>(
				sqmPath,
				tableGroup,
				mapping
		);
	}

	private EntityValuedPathInterpretation(
			SqmEntityValuedSimplePath sqmPath,
			TableGroup tableGroup,
			EntityValuedModelPart mapping) {
		super( sqmPath, mapping, tableGroup );
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {

	}
}
