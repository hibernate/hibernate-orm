/**
 * Defines contracts for accessing managed beans, choosing a caching policy,
 * and supplying fallback instances independently of a particular bean container.
 * <p>
 * The central contracts here from a client point of view are
 * {@link org.hibernate.resource.beans.spi.ManagedBean} and
 * {@link org.hibernate.resource.beans.spi.ManagedBeanRegistry},
 * which may be backed by CDI, another bean container, or fallback instance creation.
 */
package org.hibernate.resource.beans.spi;
