/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.convert;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Marco Belladelli
 */
@JiraKey(value = "HHH-15823")
@DomainModel(annotatedClasses = ConvertedAttributeConcatTest.Post.class)
public class ConvertedAttributeConcatTest {
	@Test
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.CRITERIA_VALUE_HANDLING_MODE, value = "inline")
	})
	@SessionFactory
	public void testConvertedAttributeConcatInline(SessionFactoryScope scope) {
		scope.inTransaction( this::executeTestQuery );
	}

	@Test
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.CRITERIA_VALUE_HANDLING_MODE, value = "bind")
	})
	@SessionFactory
	public void testConvertedAttributeConcatBind(SessionFactoryScope scope) {
		scope.inTransaction( this::executeTestQuery );
	}

	private void executeTestQuery(Session session) {
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<Post> query = cb.createQuery( Post.class );
		Root<Post> root = query.from( Post.class );
		query.select( root ).where( cb.like( cb.concat( ",", root.get( "tags" ) ), "%foo%" ) );
		assertNotNull( session.createQuery( query ).getResultList() );
	}

	@Entity(name = "Post")
	public static class Post {
		@Id
		private Long id;

		@Convert(converter = StringSetConverter.class)
		private Set<String> tags;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Set<String> getTags() {
			return tags;
		}

		public void setTags(Set<String> tags) {
			this.tags = tags;
		}
	}

	@Converter
	public static class StringSetConverter implements AttributeConverter<Set<String>, String> {
		@Override
		public String convertToDatabaseColumn(Set<String> attribute) {
			return attribute == null ? null : String.join( ",", attribute );
		}

		@Override
		public Set<String> convertToEntityAttribute(String dbData) {
			return dbData == null ? null : new LinkedHashSet<>( Arrays.asList( dbData.split( "," ) ) );
		}
	}
}
