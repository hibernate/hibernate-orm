/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.associationOverride;

import javax.persistence.AssociationOverride;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.ToOne;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.orm.test.util.SchemaUtil;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				EmbeddedOverrideTests.EmbeddedOverrideContact.class,
				EmbeddedOverrideTests.EmbeddedOverrideAddress.class,
				EmbeddedOverrideTests.EmbeddedOverrideState.class
		}
)
@SessionFactory
public class EmbeddedOverrideTests {

	@Test
	public void testMapping(DomainModelScope scope) {
		final PersistentClass contactBinding = scope.getDomainModel().getEntityBinding( EmbeddedOverrideContact.class.getName() );

		SchemaUtil.isColumnPresent( "embedded_override_contact", "home_street_addr", scope.getDomainModel() );
		SchemaUtil.isColumnPresent( "embedded_override_contact", "home_city", scope.getDomainModel() );
		SchemaUtil.isColumnPresent( "embedded_override_contact", "home_state_id", scope.getDomainModel() );

		SchemaUtil.isColumnPresent( "embedded_override_contact", "work_street_addr", scope.getDomainModel() );
		SchemaUtil.isColumnPresent( "embedded_override_contact", "work_city", scope.getDomainModel() );
		SchemaUtil.isColumnPresent( "embedded_override_contact", "work_state_id", scope.getDomainModel() );

		final Property homeAddressProperty = contactBinding.getProperty( "homeAddress" );
		final Component homeAddressMapping = (Component) homeAddressProperty.getValue();
		final Property homeAddressStateProperty = homeAddressMapping.getProperty( "state" );
		final ToOne homeAddressStateMapping = (ToOne) homeAddressStateProperty.getValue();
		assertThat( homeAddressStateMapping.getColumnSpan(), is( 1 ) );
		final org.hibernate.mapping.Column homeAddressStateJoinColumn = (org.hibernate.mapping.Column) homeAddressStateMapping.getColumnIterator().next();
		assertThat( homeAddressStateJoinColumn.getName(), is ( "home_state_id" ) );

		final Property workAddressProperty = contactBinding.getProperty( "workAddress" );
		final Component workAddressMapping = (Component) workAddressProperty.getValue();
		final Property workAddressStateProperty = workAddressMapping.getProperty( "state" );
		final ToOne workAddressStateMapping = (ToOne) workAddressStateProperty.getValue();
		assertThat( workAddressStateMapping.getColumnSpan(), is( 1 ) );
		final org.hibernate.mapping.Column workAddressStateJoinColumn = (org.hibernate.mapping.Column) workAddressStateMapping.getColumnIterator().next();
		assertThat( workAddressStateJoinColumn.getName(), is ( "work_state_id" ) );
	}

	@Entity( name = "EmbeddedOverrideContact" )
	@Table( name = "embedded_override_contact" )
	public static class EmbeddedOverrideContact {
		@Id
		private Integer id;
		private String name;

		@Embedded
		@AttributeOverride( name = "streetAddress", column = @Column( name = "home_street_addr" ) )
		@AttributeOverride( name = "city", column = @Column( name = "home_city" ) )
		@AssociationOverride( name = "state", joinColumns = @JoinColumn( name = "home_state_id" ) )
		private EmbeddedOverrideAddress homeAddress;

		@Embedded
		@AttributeOverride( name = "streetAddress", column = @Column( name = "work_street_addr" ) )
		@AttributeOverride( name = "city", column = @Column( name = "work_city" ) )
		@AssociationOverride( name = "state", joinColumns = @JoinColumn( name = "work_state_id" ) )
		private EmbeddedOverrideAddress workAddress;
	}

	@Embeddable
	public static class EmbeddedOverrideAddress {
		private String streetAddress;
		private String city;
		@ManyToOne
		@JoinColumn
		private EmbeddedOverrideState state;

	}

	@Entity( name= "EmbeddedOverrideState" )
	@Table( name = "embedded_override_state" )
	public static class EmbeddedOverrideState {
		@Id
		private Integer id;
		private String code;
		private String name;

		public EmbeddedOverrideState() {
		}

		public EmbeddedOverrideState(Integer id, String code, String name) {
			this.id = id;
			this.code = code;
			this.name = name;
		}

		protected Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}

