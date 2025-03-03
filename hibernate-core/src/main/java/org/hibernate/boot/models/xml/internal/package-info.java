/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Support for processing mapping XML files and applying annotation references over the appropriate
 * {@linkplain org.hibernate.models.spi.ClassDetails classes},
 * {@linkplain org.hibernate.models.spi.FieldDetails fields} and
 * {@linkplain org.hibernate.models.spi.MethodDetails methods}.
 * <p/>
 * JPA defines 2 modes for applying mapping XML information, as covered under
 * section 12.1 (<i>Use of the XML Descriptor</i>) of the specification-<dl>
 *     <dt>metadata-complete</dt>
 *     <dd>
 *         Annotations on the classes are ignored and all mapping metadata is taken from
 *         the mapping XML exclusively.
 *     </dd>
 *     <dt>override</dt>
 *     <dd>
 *         Values in the XML descriptor override or supplement the metadata define
 *         in annotations. The rules for this are fairly complex and covered in section
 *         12.2 (<i>XML Overriding Rules</i>) of the specification.
 *     </dd>
 * </dl>
 * <p/>
 * When operating in metadata-complete mode,
 * {@linkplain org.hibernate.boot.models.xml.internal.ManagedTypeProcessor} will first clear
 * all annotations from the managed type's
 * {@linkplain org.hibernate.models.spi.ClassDetails class},
 * {@linkplain org.hibernate.models.spi.FieldDetails fields} and
 * {@linkplain org.hibernate.models.spi.MethodDetails methods}.  This allows the same
 * code to be used to apply the metadata in both modes.
 *
 * @author Steve Ebersole
 */
package org.hibernate.boot.models.xml.internal;
