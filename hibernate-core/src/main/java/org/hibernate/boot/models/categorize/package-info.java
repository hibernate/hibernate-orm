/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */

/**
 * Support for processing an application's domain model, as known through
 * {@linkplain org.hibernate.boot.model.process.spi.ManagedResources} and ultimately
 * producing a mildly {@linkplain org.hibernate.boot.models.categorize.spi.CategorizedDomainModel categorized model}
 * representing entities, embeddables, etc.
 * <p/>
 * Happens in 2 steps -<ol>
 *     <li>
 *         Create the "source metamodel" ({@linkplain org.hibernate.models.spi.ClassDetails classes},
 *         {@linkplain org.hibernate.models.spi.AnnotationUsage annotations},
 *         {@linkplain org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl XML}, etc.)
 *     </li>
 *     <li>
 *         Process this "source metamodel" and produce the {@linkplain org.hibernate.boot.models.categorize.spi.CategorizedDomainModel categorized model}
 *     </li>
 * </ol>
 *
 * @author Steve Ebersole
 */
package org.hibernate.boot.models.categorize;
