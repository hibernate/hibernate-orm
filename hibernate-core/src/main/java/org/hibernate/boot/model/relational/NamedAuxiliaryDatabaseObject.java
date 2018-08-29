/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.relational;

import java.util.Set;

import org.hibernate.boot.model.naming.Identifier;

/**
 * Mainly this is used to support legacy sequence exporting.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.id.SequenceGenerator
 */
public class NamedAuxiliaryDatabaseObject
		extends SimpleAuxiliaryDatabaseObject
		implements Exportable {
	private final String name;

	public NamedAuxiliaryDatabaseObject(
			String name,
			Namespace namespace,
			String createString,
			String dropString,
			Set<String> dialectScopes) {
		super( namespace, createString, dropString, dialectScopes );
		this.name = name;
	}

	public NamedAuxiliaryDatabaseObject(
			String name,
			Namespace namespace,
			String[] createStrings,
			String[] dropStrings,
			Set<String> dialectScopes) {
		super( namespace, createStrings, dropStrings, dialectScopes );
		this.name = name;
	}

	@Override
	public String getExportIdentifier() {
		return new QualifiedNameImpl(
				Identifier.toIdentifier( getCatalogName() ),
				Identifier.toIdentifier( getSchemaName() ),
				Identifier.toIdentifier( name )
		).render();
	}
}
