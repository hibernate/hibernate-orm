/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */

/**
 * Support for processing mapping XML files, ultimately creating/updating
 * {@linkplain org.hibernate.models.spi.AnnotationUsage annotation} references
 * on the model's {@linkplain org.hibernate.models.spi.AnnotationTarget targets}
 * based on the XML.<ol>
 *     <li>
 *         First performs some {@linkplain org.hibernate.boot.models.xml.spi.XmlPreProcessor pre-processing}
 *         which aggregates information across all XML mappings
 *     </li>
 *     <li>
 *         Next performs XML {@linkplain org.hibernate.boot.models.xml.spi.XmlProcessor processing} which
 *         applies metadata-complete mappings and collects overlay/override XML for later application.
 *     </li>
 *     <li>
 *         Performs XML {@linkplain org.hibernate.boot.models.xml.spi.XmlProcessingResult post-processing} which
 *         applies overlay/override XML.
 *     </li>
 * </ol>
 *
 * @author Steve Ebersole
 */
package org.hibernate.boot.models.xml;
