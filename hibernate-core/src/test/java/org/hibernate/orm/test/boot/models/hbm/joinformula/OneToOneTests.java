package org.hibernate.orm.test.boot.models.hbm.joinformula;

import org.hibernate.mapping.Formula;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.ToOne;
import org.hibernate.orm.test.onetoone.formula.Person;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.SpecialOneToOneType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests parsing of {@code <join-formula/>} for a one-to-one
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class OneToOneTests {
	@Test
	@DomainModel(xmlMappings = "mappings/models/joinformula/one-to-one.xml")
	void testMappingXml(DomainModelScope domainModelScope) {
		final PersistentClass personEntityBinding = domainModelScope.getDomainModel().getEntityBinding( Person.class.getName() );
		validateAddress( personEntityBinding.getProperty( "address" ) );
		validateMailingAddress( personEntityBinding.getProperty( "mailingAddress" ) );
	}

	private void validateAddress(Property property) {
		final ToOne valueMapping = (ToOne) property.getValue();
		assertThat( valueMapping.getSelectables() ).hasSize( 2 );
		assertThat( valueMapping.getSelectables().get(0) ).isInstanceOf( Formula.class );
		assertThat( valueMapping.getSelectables().get(1) ).isInstanceOf( Formula.class );
	}

	private void validateMailingAddress(Property property) {
		final ToOne valueMapping = (ToOne) property.getValue();
		assertThat( valueMapping.getSelectables() ).hasSize( 2 );
		assertThat( valueMapping.getSelectables().get(0) ).isInstanceOf( Formula.class );
		assertThat( valueMapping.getSelectables().get(1) ).isInstanceOf( Formula.class );
	}

	/**
	 * Asserts the boot model produced for a formula-only one-to-one matches the
	 * legacy {@code hbm.xml} "special one-to-one" semantics: an explicit-selectable
	 * {@link OneToOne}, typed as {@link SpecialOneToOneType}, with {@code constrained}/
	 * {@code foreignKeyDirection} driven by {@code optional}, non-writable formula
	 * selectables in target-identifier order, and no physical foreign key or unique
	 * constraint.
	 */
	@Test
	@DomainModel(xmlMappings = "mappings/models/joinformula/one-to-one-optionality.xml")
	void testOptionalityMappingXml(DomainModelScope domainModelScope) {
		final PersistentClass personEntityBinding = domainModelScope.getDomainModel().getEntityBinding( Person.class.getName() );
		validateOptionalAddress( personEntityBinding.getProperty( "address" ) );
		validateRequiredMailingAddress( personEntityBinding.getProperty( "mailingAddress" ) );

		// formula-based one-to-ones must not generate a physical foreign key or unique constraint
		assertThat( personEntityBinding.getTable().getForeignKeys() ).isEmpty();
		assertThat( personEntityBinding.getTable().getUniqueKeys() ).isEmpty();
	}

	// optional="true" -> constrained=false, TO_PARENT, absent target resolves to null
	private void validateOptionalAddress(Property property) {
		final OneToOne oneToOne = assertSpecialOneToOne( property );
		assertThat( oneToOne.isReferenceToPrimaryKey() ).isTrue();
		assertThat( oneToOne.isConstrained() ).isFalse();
		assertThat( oneToOne.getForeignKeyType() ).isEqualTo( ForeignKeyDirection.TO_PARENT );
	}

	// optional="false" -> constrained=true, FROM_PARENT, required association
	private void validateRequiredMailingAddress(Property property) {
		final OneToOne oneToOne = assertSpecialOneToOne( property );
		assertThat( oneToOne.isReferenceToPrimaryKey() ).isTrue();
		assertThat( oneToOne.isConstrained() ).isTrue();
		assertThat( oneToOne.getForeignKeyType() ).isEqualTo( ForeignKeyDirection.FROM_PARENT );
	}

	private OneToOne assertSpecialOneToOne(Property property) {
		final ToOne valueMapping = (ToOne) property.getValue();
		assertThat( valueMapping ).isInstanceOf( OneToOne.class );
		final OneToOne oneToOne = (OneToOne) valueMapping;

		// explicit formulas, retained in target-identifier order
		assertThat( oneToOne.getSelectables() ).hasSize( 2 );
		assertThat( oneToOne.getSelectables().get( 0 ) ).isInstanceOf( Formula.class );
		assertThat( ( (Formula) oneToOne.getSelectables().get( 0 ) ).getFormula() ).isEqualTo( "name" );
		assertThat( oneToOne.getSelectables().get( 1 ) ).isInstanceOf( Formula.class );
		assertThat( ( (Formula) oneToOne.getSelectables().get( 1 ) ).getFormula() ).isEqualTo( "'HOME'" );

		// formula selectables are never insertable/updatable
		assertThat( oneToOne.getColumnInsertability() ).containsOnly( false );
		assertThat( oneToOne.getColumnUpdateability() ).containsOnly( false );

		assertThat( oneToOne.getType() ).isInstanceOf( SpecialOneToOneType.class );

		return oneToOne;
	}
}
