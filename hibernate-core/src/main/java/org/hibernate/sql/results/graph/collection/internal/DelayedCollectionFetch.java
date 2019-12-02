/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class DelayedCollectionFetch extends CollectionFetch {
	public DelayedCollectionFetch(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedAttribute,
			boolean nullable,
			FetchParent fetchParent) {
		super( fetchedPath, fetchedAttribute, nullable, fetchParent );
	}

	@Override
	public DomainResultAssembler createAssembler(
			FetchParentAccess parentAccess,
			Consumer<Initializer> collector,
			AssemblerCreationState creationState) {
		return new DelayedCollectionAssembler(
				getNavigablePath(),
				getFetchedMapping(),
				parentAccess,
				collector,
				creationState
		);
	}

	@Override
	public JavaTypeDescriptor getResultJavaTypeDescriptor() {
		return getFetchedMapping().getJavaTypeDescriptor();
	}
}
