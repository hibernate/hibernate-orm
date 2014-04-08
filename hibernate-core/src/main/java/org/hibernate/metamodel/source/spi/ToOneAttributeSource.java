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
package org.hibernate.metamodel.source.spi;

import java.util.List;

import org.hibernate.metamodel.internal.binder.Binder;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.type.ForeignKeyDirection;

/**
 * Further contract for sources of {@code *-to-one} style associations.
 *
 * @author Steve Ebersole
 */
public interface ToOneAttributeSource
		extends SingularAttributeSource,
			ToOneAttributeSourceNatureResolver,
			ForeignKeyContributingSource,
			FetchableAttributeSource,
			AssociationSource {

	public MapsIdSource getMapsIdSource();

	public boolean isUnique();
	public boolean isUnWrapProxy();
	ForeignKeyDirection getForeignKeyDirection();
	public List<Binder.DefaultNamingStrategy> getDefaultNamingStrategies(final String entityName, final String tableName, final AttributeBinding referencedAttributeBinding);

	/**
	 * Source for JPA {@link javax.persistence.MapsId} annotation information
	 */
	public static interface MapsIdSource {
		/**
		 * Was a MapsId annotation present on the to-one?
		 *
		 * @return {@code true} if MapsId annotation was present; {@code false} otherwise.
		 */
		public boolean isDefined();

		/**
		 * The {@link javax.persistence.MapsId#value()} value.
		 *
		 * @return The indicated name
		 */
		public String getLookupClassAttributeName();
	}
}
