/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;

/**
 * @author Koen Aers
 */
public class EntityValuedPathInterpretation<T> implements SqmPathInterpretation<T> {

	public static <T> EntityValuedPathInterpretation<T> from(
			SqmEntityValuedSimplePath<T> sqmPath,
			SqlAstCreationState sqlAstCreationState) {
		final TableGroup tableGroup = sqlAstCreationState
				.getFromClauseAccess()
				.findTableGroup( sqmPath.getLhs().getNavigablePath() );
		final EntityValuedModelPart mapping = (EntityValuedModelPart) tableGroup
				.getModelPart()
				.findSubPart( sqmPath.getReferencedPathSource().getPathName(),null );
		return new EntityValuedPathInterpretation<>(
				sqmPath,
				tableGroup,
				mapping);
	}

	private EntityValuedModelPart mapping = null;
	private TableGroup tableGroup = null;
	private SqmEntityValuedSimplePath sqmPath = null;

	private EntityValuedPathInterpretation(
			SqmEntityValuedSimplePath sqmPath,
			TableGroup tableGroup,
			EntityValuedModelPart mapping) {
		this.tableGroup = tableGroup;
		this.mapping = mapping;
		this.sqmPath = sqmPath;
	}

	@Override
	public DomainResult<T> createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		return mapping.createDomainResult(
				getNavigablePath(),
				tableGroup,
				resultVariable,
				creationState
		);
	}

	@Override
	public SqmPath<T> getInterpretedSqmPath() {
		return sqmPath;
	}

	@Override
	public ModelPart getExpressionType() {
		return null;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {

	}
}
