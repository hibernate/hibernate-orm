package org.hibernate.orm.test.boot.models.hbm.joinformula;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * Tests parsing a combination of {@code <join-column/>} and {@code <join-formula/>}
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class JoinColumnAndFormulaTests {
	@Test
	@DomainModel( xmlMappings = "mappings/models/joinformula/many-to-one-join-column-and-formula.xml")
	void testXml(DomainModelScope domainModelScope) {
		verifyMapping( domainModelScope );
	}

	@Test
	@DomainModel( annotatedClasses = {Address.class, Contact.class} )
	void testAnnotations(DomainModelScope domainModelScope) {
		verifyMapping( domainModelScope );
	}

	void verifyMapping(DomainModelScope domainModelScope) {
		final PersistentClass entityBinding = domainModelScope.getEntityBinding( Contact.class );

		final Property mailingAddress = entityBinding.getProperty( "mailingAddress" );
		final Value mailingAddressValue = mailingAddress.getValue();
		assertThat( mailingAddressValue.getSelectables() ).hasSize( 2 );
		assertThat( ( (Column) mailingAddressValue.getSelectables().get( 0 ) ).getName() ).isEqualTo( "mailing_address_fk" );
		assertThat( ( (Formula) mailingAddressValue.getSelectables().get( 1 ) ).getFormula() ).isEqualTo( "'MAILING'" );

		final Property shippingAddress = entityBinding.getProperty( "shippingAddress" );
		final Value shippingAddressValue = shippingAddress.getValue();
		assertThat( shippingAddressValue.getSelectables() ).hasSize( 2 );
		assertThat( ( (Column) shippingAddressValue.getSelectables().get( 0 ) ).getName() ).isEqualTo( "shipping_address_fk" );
		assertThat( ( (Formula) shippingAddressValue.getSelectables().get( 1 ) ).getFormula() ).isEqualTo( "'SHIPPING'" );
	}

	@Test
	@DomainModel( xmlMappings = "mappings/models/joinformula/one-to-one-join-column-and-formula.xml")
	void testOneToOneXml(DomainModelScope domainModelScope) {
		verifyMixedOneToOneRemainsManyToOne( domainModelScope );
	}

	@Test
	@DomainModel( annotatedClasses = {Address.class, OneToOneContact.class} )
	void testOneToOneAnnotations(DomainModelScope domainModelScope) {
		verifyMixedOneToOneRemainsManyToOne( domainModelScope );
	}

	/**
	 * A {@code @OneToOne}/{@code <one-to-one>} whose join mixes a writable physical column
	 * with a formula must retain its existing logical many-to-one binding -- writable mixed
	 * tuples are not eligible for the "special one-to-one" binding path.
	 */
	void verifyMixedOneToOneRemainsManyToOne(DomainModelScope domainModelScope) {
		final PersistentClass entityBinding = domainModelScope.getEntityBinding( OneToOneContact.class );

		final Property mailingAddress = entityBinding.getProperty( "mailingAddress" );
		final Value mailingAddressValue = mailingAddress.getValue();
		assertThat( mailingAddressValue ).isInstanceOf( ManyToOne.class );
		assertThat( ( (ManyToOne) mailingAddressValue ).isLogicalOneToOne() ).isTrue();

		assertThat( mailingAddressValue.getSelectables() ).hasSize( 2 );
		assertThat( ( (Column) mailingAddressValue.getSelectables().get( 0 ) ).getName() ).isEqualTo( "mailing_address_fk" );
		assertThat( ( (Formula) mailingAddressValue.getSelectables().get( 1 ) ).getFormula() ).isEqualTo( "'MAILING'" );
	}
}
