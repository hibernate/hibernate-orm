/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.relational;

import java.util.Set;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.Mapping;

/**
 * @author Steve Ebersole
 */
public class BasicAuxiliaryDatabaseObjectImpl implements AuxiliaryDatabaseObject {
	private final String createString;
	private final String dropString;
	private final Set<String> dialectScopes;

	public BasicAuxiliaryDatabaseObjectImpl(String createString, String dropString, Set<String> dialectScopes) {
		this.createString = createString;
		this.dropString = dropString;
		this.dialectScopes = dialectScopes;
	}

	@Override
	public boolean appliesToDialect(Dialect dialect) {
		// empty means no scoping
		return dialectScopes.isEmpty() || dialectScopes.contains( dialect.getClass().getName() );
	}

	@Override
	public String sqlCreateString(Dialect dialect, Mapping p, String defaultCatalog, String defaultSchema) {
		return createString;
	}

	@Override
	public String sqlDropString(Dialect dialect, String defaultCatalog, String defaultSchema) {
		return dropString;
	}
}
