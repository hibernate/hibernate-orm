/// Support for discovery of managed classes.
/// Used in conjunction with {@linkplain jakarta.persistence.PersistenceConfiguration}, especially
/// {@linkplain org.hibernate.jpa.HibernatePersistenceConfiguration}.
/// Designed with use from integrations in mind as well.
///
/// @see org.hibernate.jpa.boot.spi.PersistenceConfigurationDescriptor
///
/// @todo (jpa4) : drop `org.hibernate.boot.archive` in favor of this.
///
/// @author Steve Ebersole
package org.hibernate.boot.scan;
