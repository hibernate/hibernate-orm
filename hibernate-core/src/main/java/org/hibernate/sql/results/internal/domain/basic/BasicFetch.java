/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.basic;

import java.util.function.Consumer;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.Fetchable;
import org.hibernate.sql.results.spi.Initializer;

/**
 * @author Steve Ebersole
 */
public class BasicFetch implements Fetch {
	private final NavigablePath navigablePath;
	private final FetchParent fetchParent;
	private final BasicValuedModelPart valuedMapping;
	private final boolean nullable;

	private final BasicResultAssembler assembler;

	private final FetchTiming fetchTiming;

	public BasicFetch(
			int valuesArrayPosition,
			FetchParent fetchParent,
			BasicValuedModelPart valuedMapping,
			boolean nullable,
			BasicValueConverter valueConverter,
			FetchTiming fetchTiming,
			DomainResultCreationState creationState) {
		this.nullable = nullable;
		this.navigablePath = fetchParent.getNavigablePath().append( valuedMapping.getFetchableName() );

		this.fetchParent = fetchParent;
		this.valuedMapping = valuedMapping;
		this.fetchTiming = fetchTiming;

		//noinspection unchecked
		this.assembler = new BasicResultAssembler(
				valuesArrayPosition,
				valuedMapping.getJavaTypeDescriptor(),
				valueConverter
		);

	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public Fetchable getFetchedMapping() {
		return valuedMapping;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public boolean isNullable() {
		return nullable;
	}

	@Override
	public DomainResultAssembler createAssembler(
			FetchParentAccess parentAccess,
			Consumer<Initializer> collector,
			AssemblerCreationState creationState) {
		return assembler;
	}

}
