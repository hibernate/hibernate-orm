/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Support for processing mapping XML files, ultimately creating/updating
 * {@linkplain org.hibernate.models.spi.ClassDetails}, {@linkplain org.hibernate.models.spi.FieldDetails}
 * and annotation references based on the XML.<ol>
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
