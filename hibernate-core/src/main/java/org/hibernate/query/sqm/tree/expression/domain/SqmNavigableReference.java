/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import java.util.Collection;

import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.EntityTypeImplementor;
import org.hibernate.sql.ast.produce.metamodel.spi.NavigableReferenceInfo;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmDowncast;

/**
 * Represents a specific reference to a given {@link Navigable}
 * in relation to an SQM query.  E.g., a query defined as {@code select .. from Person p1, Person p2}
 * contains 2 different Navigable references: the SqmRoot references p1 and p2.  So it is the same
 * Navigable (Person entity), but 2 different NavigableReferences.
 * <p/>
 * Such a reference is a specialization of SqmExpression, meaning it can occur in any place
 * in the query that an expression is valid - although some limitations do apply to specific
 * contexts
 *
 * @author Steve Ebersole
 */
public interface SqmNavigableReference extends SqmExpression, NavigableReferenceInfo {
	/**
	 * Get the Navigable reference that is the source ("lhs") of this reference.
	 */
	SqmNavigableContainerReference getSourceReference();

	/**
	 * The Navigable represented by this reference.
	 */
	Navigable getReferencedNavigable();

	/**
	 * Returns the NavigablePath representing the path to this NavigableReference
	 * relative to a "query root".
	 */
	NavigablePath getNavigablePath();


	// JPA downcast (TREAT .. AS ..) support

	SqmNavigableReference treatAs(EntityTypeImplementor target);

	void addDowncast(SqmDowncast downcast);

	Collection<SqmDowncast> getDowncasts();
}
