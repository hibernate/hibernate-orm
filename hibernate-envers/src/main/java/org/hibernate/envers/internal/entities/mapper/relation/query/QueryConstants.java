/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers.internal.entities.mapper.relation.query;

/**
 * Constants used in JPQL queries.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public final class QueryConstants {
	private QueryConstants() {
	}

	public static final String REFERENCED_ENTITY_ALIAS = "e__";
	public static final String REFERENCED_ENTITY_ALIAS_DEF_AUD_STR = "e2__";

	public static final String INDEX_ENTITY_ALIAS = "f__";
	public static final String INDEX_ENTITY_ALIAS_DEF_AUD_STR = "f2__";

	public static final String MIDDLE_ENTITY_ALIAS = "ee__";
	public static final String MIDDLE_ENTITY_ALIAS_DEF_AUD_STR = "ee2__";

	public static final String REVISION_PARAMETER = "revision";
	public static final String DEL_REVISION_TYPE_PARAMETER = "delrevisiontype";
}
