/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import org.hibernate.boot.mapping.internal.relational.TableOwner;

/// Internal role shared by categorized attributes which also own a table
/// reference.
///
/// This interface is retained because multiple unrelated attribute
/// implementations need the intersection of the supported
/// [org.hibernate.boot.mapping.spi.AttributeMetadata] contract and the
/// internal [TableOwner] marker. It does not add another descriptive API.
///
/// @since 9.0
/// @author Steve Ebersole
public interface AttributeMetadataImplementor extends TableOwner, org.hibernate.boot.mapping.spi.AttributeMetadata {
}
