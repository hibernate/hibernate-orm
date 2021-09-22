/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.util.List;
import java.util.Set;

import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

/**
 * @author Gavin King
 * @author Steve Ebersole
 */
public class SubselectFetch {
	private final EntityValuedModelPart entityModelPart;
	private final QuerySpec loadingSqlAst;
	private final TableGroup ownerTableGroup;
	private final List<JdbcParameter> loadingJdbcParameters;
	private final JdbcParameterBindings loadingJdbcParameterBindings;
	private final Set<EntityKey> resultingEntityKeys;

	public SubselectFetch(
			EntityValuedModelPart entityModelPart,
			QuerySpec loadingSqlAst,
			TableGroup ownerTableGroup,
			List<JdbcParameter> loadingJdbcParameters,
			JdbcParameterBindings loadingJdbcParameterBindings,
			Set<EntityKey> resultingEntityKeys) {
		this.entityModelPart = entityModelPart;
		this.loadingSqlAst = loadingSqlAst;
		this.ownerTableGroup = ownerTableGroup;
		this.loadingJdbcParameters = loadingJdbcParameters;
		this.loadingJdbcParameterBindings = loadingJdbcParameterBindings;
		this.resultingEntityKeys = resultingEntityKeys;
	}

	public EntityValuedModelPart getEntityModelPart() {
		return entityModelPart;
	}

	public QuerySpec getLoadingSqlAst() {
		return loadingSqlAst;
	}

	public TableGroup getOwnerTableGroup() {
		return ownerTableGroup;
	}

	public List<JdbcParameter> getLoadingJdbcParameters() {
		return loadingJdbcParameters;
	}

	public JdbcParameterBindings getLoadingJdbcParameterBindings() {
		return loadingJdbcParameterBindings;
	}

	public Set<EntityKey> getResultingEntityKeys() {
		return resultingEntityKeys;
	}

	@Override
	public String toString() {
		return "SubselectFetch(" + entityModelPart.getEntityMappingType().getEntityName() + ")";
	}
}
