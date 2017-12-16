/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Package defining boot-time handling of JPA's
 * {@link javax.persistence.AttributeConverter}
 *
 * The general paradigm is that handling converters is split into a
 * boot-time piece and a run-time piece.  This package defines the
 * boot-time piece.    Specifically the boot-time representation of a
 * converter is {@link org.hibernate.boot.model.convert.spi.ConverterDescriptor}.
 * Another important aspect is managing the resolution of auto-applied
 * converters which is handled by coordination between
 * {@link org.hibernate.boot.model.convert.spi.AutoApplicableConverterDescriptor}
 * and {@link org.hibernate.boot.model.convert.spi.ConverterAutoApplyHandler}
 *
 * The run-time piece is defined by
 * {@link org.hibernate.metamodel.model.convert.spi.JpaAttributeConverter}.
 * The bridge from boot-time to run-time is defined by
 * {@link org.hibernate.boot.model.convert.spi.ConverterDescriptor#createJpaAttributeConverter}.
 * This process also incorporates integration with
 * {@link org.hibernate.resource.beans.spi.ManagedBeanRegistry} for resolving
 * converters from DI containers (if configured)
 */
package org.hibernate.boot.model.convert;
