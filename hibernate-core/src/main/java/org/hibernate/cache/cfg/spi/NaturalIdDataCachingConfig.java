/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.cfg.spi;

/**
 * Specialized DomainDataCachingConfig describing the requested
 * caching config for the natural-id data of a particular entity (hierarchy)
 *
 * @author Steve Ebersole
 */
public interface NaturalIdDataCachingConfig extends DomainDataCachingConfig {
}
