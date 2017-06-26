/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.tree.spi.select.Selectable;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeReference implements NavigableReference {
	private final NavigableContainerReference containerReference;
	private final PluralPersistentAttribute referencedAttribute;
	private final NavigablePath navigablePath;

	public PluralAttributeReference(
			NavigableContainerReference containerReference,
			PluralPersistentAttribute referencedAttribute,
			NavigablePath navigablePath) {
		this.containerReference = containerReference;
		this.referencedAttribute = referencedAttribute;
		this.navigablePath = navigablePath;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitPluralAttribute( this );
	}

	@Override
	public ExpressableType getType() {
		return referencedAttribute;
	}

	@Override
	public Navigable getNavigable() {
		return referencedAttribute;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public Selectable getSelectable() {
		return referencedAttribute;
	}

	@Override
	public NavigableContainerReference getNavigableContainerReference() {
		return containerReference;
	}
}
