/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.basic;

import java.sql.Types;
import java.util.Set;

import org.hibernate.annotations.Type;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.usertype.UserTypeSupport;

import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
public class CollectionAsBasicTest {
	@Test
	public void testCollectionAsBasic() {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();
		try {
			Metadata metadata = new MetadataSources(ssr).addAnnotatedClass( Post.class ).getMetadataBuilder().build();
			PersistentClass postBinding = metadata.getEntityBinding( Post.class.getName() );
			Property tagsAttribute = postBinding.getProperty( "tags" );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Entity
	@Table( name = "post")
	public static class Post {
		@Id
		public Integer id;
		public String name;
		@Basic
		@Type( DelimitedStringsJavaType.class )
		Set<String> tags;
	}

	public static class DelimitedStringsJavaType extends UserTypeSupport<Set> {
		public DelimitedStringsJavaType() {
			super( Set.class, Types.VARCHAR );
		}
	}
}
