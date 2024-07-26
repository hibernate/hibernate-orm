/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
