/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */

/**
 * Support for processing mapping XML files, ultimately creating/updating
 * {@linkplain org.hibernate.models.spi.AnnotationUsage annotation} references
 * on the model's {@linkplain org.hibernate.models.spi.AnnotationTarget targets}
 * based on the XML.<ol>
 *     <li>
 *         First performs some {@linkplain org.hibernate.boot.models.categorize.xml.spi.XmlPreProcessor pre-processing}
 *         which aggregates information across all XML mappings
 *     </li>
 *     <li>
 *         Next performs XML {@linkplain org.hibernate.boot.models.categorize.xml.spi.XmlProcessor processing} which
 *         applies metadata-complete mappings and collects overlay/override XML for later application.
 *     </li>
 *     <li>
 *         Performs XML {@linkplain org.hibernate.boot.models.categorize.xml.spi.XmlProcessingResult post-processing} which
 *         applies overlay/override XML.
 *     </li>
 * </ol>
 *
 * @author Steve Ebersole
 */
package org.hibernate.boot.models.categorize.xml;
