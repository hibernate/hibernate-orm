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
package org.hibernate.loader.spi;

import java.util.Map;

import org.hibernate.Filter;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.spi.CollectionReference;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.Fetch;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.sql.JoinType;
import org.hibernate.type.AssociationType;

/**
 * @author Gail Badner
 */
public interface JoinableAssociation {
	PropertyPath getPropertyPath();

	JoinType getJoinType();

	Fetch getCurrentFetch();

	EntityReference getCurrentEntityReference();

	CollectionReference getCurrentCollectionReference();

	AssociationType getJoinableType();

	Joinable getJoinable();

	boolean isCollection();

	public String[] getRhsColumns();

	boolean hasRestriction();

	boolean isManyToManyWith(JoinableAssociation other);

	String getWithClause();

	Map<String,Filter> getEnabledFilters();
}
