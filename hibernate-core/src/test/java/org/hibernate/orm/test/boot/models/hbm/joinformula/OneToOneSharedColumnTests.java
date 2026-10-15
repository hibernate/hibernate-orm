package org.hibernate.orm.test.boot.models.hbm.joinformula;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.OneToOne;
import org.hibernate.type.SpecialOneToOneType;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests a read-only shared owner column combined with a formula in a special one-to-one.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class OneToOneSharedColumnTests {
	@Test
	@DomainModel( annotatedClasses = {SharedColumnPerson.class, SharedColumnAddress.class} )
	void testSharedReadOnlyColumnPlusFormula(DomainModelScope domainModelScope) {
		final PersistentClass personEntityBinding = domainModelScope.getEntityBinding( SharedColumnPerson.class );
		final Property address = personEntityBinding.getProperty( "address" );
		final Value addressValue = address.getValue();

		assertThat( addressValue ).isInstanceOf( OneToOne.class );
		assertThat( addressValue.getType() ).isInstanceOf( SpecialOneToOneType.class );

		assertThat( addressValue.getSelectables() ).hasSize( 2 );
		assertThat( ( (Column) addressValue.getSelectables().get( 0 ) ).getName() ).isEqualTo( "name" );
		assertThat( ( (Formula) addressValue.getSelectables().get( 1 ) ).getFormula() ).isEqualTo( "'HOME'" );

		// the shared column is read-only: the association must not write to it
		assertThat( addressValue.getColumnInsertability() ).containsOnly( false );
		assertThat( addressValue.getColumnUpdateability() ).containsOnly( false );

		// no duplicate/conflicting "name" column was created on Person's table
		assertThat( personEntityBinding.getTable().getColumns() )
				.extracting( Column::getName )
				.containsExactly( "name" );
	}
}
