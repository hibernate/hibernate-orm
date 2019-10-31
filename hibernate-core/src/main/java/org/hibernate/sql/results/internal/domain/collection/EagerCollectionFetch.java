/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.collection;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.CollectionIdentifierDescriptor;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.FetchableContainer;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class EagerCollectionFetch extends CollectionFetch implements FetchParent {
	private final DomainResult fkResult;

	private final Fetch elementFetch;
	private final Fetch indexFetch;
	private final DomainResult identifierResult;

	private final List<Fetch> fetches;

	public EagerCollectionFetch(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedAttribute,
			DomainResult fkResult,
			boolean nullable,
			FetchParent fetchParent,
			DomainResultCreationState creationState) {
		super( fetchedPath, fetchedAttribute, nullable, fetchParent );
		this.fkResult = fkResult;

		final CollectionIdentifierDescriptor identifierDescriptor = fetchedAttribute.getIdentifierDescriptor();
		if ( identifierDescriptor == null ) {
			this.identifierResult = null;
		}
		else {
			final TableGroup collectionTableGroup = creationState.getSqlAstCreationState().getFromClauseAccess().getTableGroup( fetchedPath );
			this.identifierResult = identifierDescriptor.createDomainResult( fetchedPath, collectionTableGroup, creationState );
		}

		fetches = creationState.visitFetches( this );
		if ( fetchedAttribute.getIndexDescriptor() != null ) {
			assert fetches.size() == 2;
			indexFetch = fetches.get( 0 );
			elementFetch = fetches.get( 1 );
		}
		else {
			assert fetches.size() == 1;
			indexFetch = null;
			elementFetch = fetches.get( 0 );
		}
	}

	@Override
	public DomainResultAssembler createAssembler(
			FetchParentAccess parentAccess,
			Consumer<Initializer> collector,
			AssemblerCreationState creationState) {
		return new EagerCollectionAssembler(
				getNavigablePath(),
				getFetchedMapping(),
				fkResult,
				elementFetch,
				indexFetch,
				identifierResult,
				parentAccess,
				collector,
				creationState
		);
	}

	@Override
	public FetchableContainer getReferencedMappingContainer() {
		return getFetchedMapping();
	}

	@Override
	public PluralAttributeMapping getReferencedMappingType() {
		return getFetchedMapping();
	}

	@Override
	public List<Fetch> getFetches() {
		return fetches;
	}

	@Override
	public Fetch findFetch(String fetchableName) {
		if ( CollectionPart.Nature.ELEMENT.getName().equals( fetchableName ) ) {
			return elementFetch;
		}
		else if ( CollectionPart.Nature.INDEX.getName().equals( fetchableName ) ) {
			return indexFetch;
		}
		else {
			throw new IllegalArgumentException(
					"Unknown fetchable [" + getFetchedMapping().getCollectionDescriptor().getRole() +
							" -> " + fetchableName + "]"
			);
		}
	}

	@Override
	public JavaTypeDescriptor getResultJavaTypeDescriptor() {
		return getFetchedMapping().getJavaTypeDescriptor();
	}
}
