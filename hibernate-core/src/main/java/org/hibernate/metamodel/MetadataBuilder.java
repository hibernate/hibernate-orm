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
package org.hibernate.metamodel;

import javax.persistence.SharedCacheMode;

import org.hibernate.boot.spi.CacheRegionDefinition;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.metamodel.spi.TypeContributor;
import org.hibernate.type.BasicType;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;

import org.jboss.jandex.IndexView;

import org.xml.sax.EntityResolver;

/**
 * Contract for specifying various overrides to be used in metamodel building.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public interface MetadataBuilder {
	/**
	 * Specify a specific NamingStrategy to use in building the metamodel.
	 *
	 * @param namingStrategy The naming strategy to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	public MetadataBuilder with(NamingStrategy namingStrategy);

	/**
	 * Specify a specific XML EntityResolver to use in building the metamodel.
	 *
	 * @param entityResolver The entity resolver to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	public MetadataBuilder with(EntityResolver entityResolver);

	/**
	 * Specify the order in which to process metadata sources.
	 *
	 * @param metadataSourceProcessingOrder The order.
	 *
	 * @return {@code this}, for method chaining
	 */
	public MetadataBuilder with(MetadataSourceProcessingOrder metadataSourceProcessingOrder);

	/**
	 * Specify the second-level cache mode to be used.  This is the cache mode in terms of whether or
	 * not to cache.
	 *
	 * @param cacheMode The cache mode.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #with(org.hibernate.cache.spi.access.AccessType)
	 */
	public MetadataBuilder with(SharedCacheMode cacheMode);

	/**
	 * Specify the second-level access-type to be used by default for entities and collections that define second-level
	 * caching, but do not specify a granular access-type.
	 *
	 * @param accessType The access-type to use as default.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #with(javax.persistence.SharedCacheMode)
	 */
	public MetadataBuilder with(AccessType accessType);

	/**
	 * Allows specifying a specific Jandex index to use for reading annotation information.
	 * <p/>
	 * It is <i>important</i> to understand that if a Jandex index is passed in, it is expected that
	 * this Jandex index already contains all entries for all classes.  No additional indexing will be
	 * done in this case.
	 *
	 * @param jandexView The Jandex index to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	public MetadataBuilder with(IndexView jandexView);

	/**
	 * Should the new (well "new" since 3.2) identifier generators be used for
	 * {@link javax.persistence.GenerationType#SEQUENCE},
	 * {@link javax.persistence.GenerationType#IDENTITY},
	 * {@link javax.persistence.GenerationType#TABLE} and
	 * {@link javax.persistence.GenerationType#AUTO} handling?
	 *
	 * @param enabled {@code true} says to use the new generator mappings; {@code false} says to use the legacy
	 * generator mappings.
	 *
	 * @return {@code this}, for method chaining
	 */
	public MetadataBuilder withNewIdentifierGeneratorsEnabled(boolean enabled);

	/**
	 *
	 * @param enabled
	 * @return
	 */
	public MetadataBuilder withExplicitDiscriminatorsForJoinedSubclassSupport(boolean enabled);

	public MetadataBuilder withImplicitDiscriminatorsForJoinedSubclassSupport(boolean enabled);

	/**
	 * Specify an additional or overridden basic type mapping.
	 *
	 * @param type The type addition or override.
	 *
	 * @return {@code this}, for method chaining
	 */
	public MetadataBuilder with(BasicType type);

	/**
	 * Register an additional or overridden custom type mapping.
	 *
	 * @param type The custom type
	 * @param keys The keys under which to register the custom type.
	 *
	 * @return {@code this}, for method chaining
	 */
	public MetadataBuilder with(UserType type, String[] keys);

	/**
	 * Register an additional or overridden composite custom type mapping.
	 *
	 * @param type The composite custom type
	 * @param keys The keys under which to register the composite custom type.
	 *
	 * @return {@code this}, for method chaining
	 */
	public MetadataBuilder with(CompositeUserType type, String[] keys);

	/**
	 * Apply an explicit TypeContributor (implicit application via ServiceLoader will still happen too)
	 *
	 * @param typeContributor The contributor to apply
	 *
	 * @return {@code this}, for method chaining
	 */
	public MetadataBuilder with(TypeContributor typeContributor);

	/**
	 * Apply a CacheRegionDefinition to be applied to an entity, collection or query while building the
	 * Metadata object.
	 *
	 * @param cacheRegionDefinition The cache region definition to apply
	 */
	public MetadataBuilder with(CacheRegionDefinition cacheRegionDefinition);

	/**
	 * Apply a ClassLoader for use while building the Metadata.
	 * <p/>
	 * Ideally we should avoid accessing ClassLoaders when perform 1st phase of bootstrap.  This
	 * is a ClassLoader that can be used in cases when we have to.  IN EE managed environments, this
	 * is the ClassLoader mandated by
	 * {@link javax.persistence.spi.PersistenceUnitInfo#getNewTempClassLoader()}.  This ClassLoader
	 * is thrown away by the container afterwards.  The idea being that the Class can still be enhanced
	 * in the application ClassLoader.  In other environments, pass a ClassLoader that performs the
	 * same function if desired.
	 *
	 * @param tempClassLoader ClassLoader for use during building the Metadata
	 */
	public MetadataBuilder with(ClassLoader tempClassLoader);

	/**
	 * Actually build the metamodel
	 *
	 * @return The built metadata.
	 */
	public Metadata build();
}
