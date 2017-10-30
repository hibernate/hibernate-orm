/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.List;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.CompositeQueryResult;
import org.hibernate.sql.results.spi.InitializerCollector;
import org.hibernate.sql.results.spi.QueryResultAssembler;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class CompositeQueryResultImpl extends AbstractFetchParent implements CompositeQueryResult {
	private final String resultVariable;

	private final QueryResultAssembler assembler;

	public CompositeQueryResultImpl(
			String resultVariable,
			EmbeddedValuedNavigable navigable,
			List<SqlSelection> sqlSelections) {
		super( navigable, new NavigablePath( navigable.getEmbeddedDescriptor().getRoleName() ) );
		this.resultVariable = resultVariable;

		this.assembler = new CompositeQueryResultAssembler(
				this,
				sqlSelections,
				navigable.getEmbeddedDescriptor()
		);
	}

	@Override
	public EmbeddedValuedNavigable getFetchContainer() {
		return (EmbeddedValuedNavigable) super.getFetchContainer();
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}


	@Override
	public QueryResultAssembler getResultAssembler() {
		return assembler;
	}

	@Override
	public EmbeddedTypeDescriptor getEmbeddedDescriptor() {
		return getFetchContainer().getEmbeddedDescriptor();
	}

	@Override
	public EmbeddableJavaDescriptor getJavaTypeDescriptor() {
		return getEmbeddedDescriptor().getJavaTypeDescriptor();
	}

	@Override
	public void registerInitializers(InitializerCollector collector) {
		// todo (6.0) : register the CompositeInitializer as well as the initializers for our fetches
		throw new NotYetImplementedFor6Exception(  );
	}

//	@Override
//	public Initializer generateInitializer(QueryResultCreationContext creationContext) {
//		// todo (6.0) : register the CompositeInitializer as well as the initializers for our fetches
//		throw new NotYetImplementedException(  );
//	}
}
