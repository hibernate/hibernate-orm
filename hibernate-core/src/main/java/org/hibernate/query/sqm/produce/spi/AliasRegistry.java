/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.hibernate.query.sqm.AliasCollisionException;
import org.hibernate.query.sqm.produce.internal.NavigableBindingHelper;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.select.SqmSelection;

/**
 * @author Andrea Boriero
 */
public class AliasRegistry {
	private Map<String, SqmNavigableReference> navigableBindingsByAlias = new HashMap<>();
	private Map<String, SqmSelection> selectionsByAlias = new HashMap<>();

	private AliasRegistry parent;

	public AliasRegistry() {
	}

	public AliasRegistry(AliasRegistry parent) {
		this.parent = parent;
	}

	public AliasRegistry getParent() {
		return parent;
	}

	public void registerAlias(SqmSelection selection) {
		if ( selection.getAlias() != null ) {
			checkResultVariable( selection );
			selectionsByAlias.put( selection.getAlias(), selection );
		}
	}

	public void registerAlias(SqmNavigableReference binding) {
		final SqmFrom exportedFromElement = NavigableBindingHelper.resolveExportedFromElement( binding );
		final SqmNavigableReference old = navigableBindingsByAlias.put( exportedFromElement.getIdentificationVariable(), binding );
		if ( old != null ) {
			throw new AliasCollisionException(
					String.format(
							Locale.ENGLISH,
							"Alias [%s] used for multiple from-clause-elements : %s, %s",
							exportedFromElement.getIdentificationVariable(),
							old,
							binding
					)
			);
		}
	}

	public SqmSelection findSelectionByAlias(String alias) {
		return selectionsByAlias.get( alias );
	}

	public SqmNavigableReference findFromElementByAlias(String alias) {
		if ( navigableBindingsByAlias.containsKey( alias ) ) {
			return navigableBindingsByAlias.get( alias );
		}
		else if ( parent != null ) {
			return parent.findFromElementByAlias( alias );
		}
		return null;
	}

	private void checkResultVariable(SqmSelection selection) {
		final String alias = selection.getAlias();
		if ( selectionsByAlias.containsKey( alias ) ) {
			throw new AliasCollisionException(
					String.format(
							Locale.ENGLISH,
							"Alias [%s] is already used in same select clause",
							alias
					)
			);
		}
		final SqmNavigableReference binding = navigableBindingsByAlias.get( alias );
		if ( binding != null ) {
			if ( !selection.getExpression().getExpressionType().equals( binding.getReferencedNavigable().getExportedDomainType() ) ) {
				throw new AliasCollisionException(
						String.format(
								Locale.ENGLISH,
								"Alias [%s] used in select-clause for %s is also used in from element: %s for %s",
								alias,
								selection.getExpression().getExpressionType(),
								binding,
								binding.getReferencedNavigable()
						)
				);
			}
		}
	}

}
