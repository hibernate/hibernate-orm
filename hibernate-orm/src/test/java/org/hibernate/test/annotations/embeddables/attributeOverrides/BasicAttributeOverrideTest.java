/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.annotations.embeddables.attributeOverrides;

import java.util.Map;
import java.util.UUID;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.BasicType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class BasicAttributeOverrideTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { TypeValue.class, AggregatedTypeValue.class };
	}

	@Test
	@TestForIssue( jiraKey = "HHH-8630" )
	public void testIt() {
		final PersistentClass entityBinding = metadata().getEntityBinding( AggregatedTypeValue.class.getName() );
		final Property attributesBinding = entityBinding.getProperty( "attributes" );
		final org.hibernate.mapping.Map attributesMap = (org.hibernate.mapping.Map) attributesBinding.getValue();

		final SimpleValue mapKey = assertTyping( SimpleValue.class, attributesMap.getIndex() );
		final BasicType mapKeyType = assertTyping( BasicType.class, mapKey.getType() );
		assertTrue( String.class.equals( mapKeyType.getReturnedClass() ) );

		// let's also make sure the @MapKeyColumn got applied
		assertThat( mapKey.getColumnSpan(), is(1) );
		final org.hibernate.mapping.Column mapKeyColumn = assertTyping( org.hibernate.mapping.Column .class, mapKey.getColumnIterator().next() );
		assertThat( mapKeyColumn.getName(), equalTo( "attribute_name" ) );
	}

	@Embeddable
	public static class TypeValue {
		String type;

		@Column(columnDefinition = "TEXT")
		String value;
	}

	@Entity
	@Table( name = "AGG_TYPE" )
	public static class AggregatedTypeValue {
		@Id
		UUID id;

		@Embedded
		@AttributeOverrides({
				@AttributeOverride(name = "type", column = @Column(name = "content_type")),
				@AttributeOverride(name = "value", column = @Column(name = "content_value"))
		})
		TypeValue content;

		@CollectionTable( name = "ATTRIBUTES" )
		@ElementCollection(fetch = FetchType.EAGER)
		@MapKeyColumn(name = "attribute_name")
		@AttributeOverrides({
				@AttributeOverride(name = "value.type", column = @Column(name = "attribute_type")),
				@AttributeOverride(name = "value.value", column = @Column(name = "attribute_value"))
		})
		Map<String, TypeValue> attributes;
	}
}
