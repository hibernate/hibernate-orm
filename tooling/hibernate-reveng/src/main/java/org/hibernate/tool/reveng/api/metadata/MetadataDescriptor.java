/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.api.metadata;

import org.hibernate.boot.Metadata;

import java.util.Properties;

public interface MetadataDescriptor {

	Metadata createMetadata();

	Properties getProperties();

}
