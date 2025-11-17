/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.embeddables.collection.xml;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.annotations.embeddables.collection.AbstractEmbeddableWithManyToManyTest;

import org.hibernate.testing.orm.junit.JiraKey;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-11302")
public class EmbeddableWithOneToMany_HHH_11302_xml_Test extends AbstractEmbeddableWithManyToManyTest {

	protected void addResources(MetadataSources metadataSources) {
		metadataSources.addResource( "org/hibernate/orm/test/annotations/embeddables/collection/orm.xml" );
	}

}
