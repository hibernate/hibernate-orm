/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.crud;

import java.time.Instant;
import java.util.Date;

import org.hibernate.boot.MetadataSources;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;
import org.hibernate.orm.test.support.domains.gambit.Component;
import org.hibernate.orm.test.support.domains.gambit.EntityOfComposites;
import org.hibernate.orm.test.support.domains.gambit.EntityWithManyToOneSelfReference;
import org.hibernate.orm.test.support.domains.gambit.SimpleEntity;
import org.hibernate.sql.exec.spi.BasicExecutionContext;
import org.hibernate.sql.exec.spi.ExecutionContext;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class StateArrayShapingTest extends SessionFactoryBasedFunctionalTest {

	@Test
	public void testSimple() {
		final Instant theInstant = Instant.EPOCH;
		final Date theDate = Date.from( theInstant );
		final Integer theInt = Integer.MAX_VALUE;
		final Long theLong = Long.MAX_VALUE;
		final String theString = "the string";

		final SimpleEntity initialInstance = new SimpleEntity(
				1,
				theDate,
				theInstant,
				theInt,
				theLong,
				theString
		);

		final EntityTypeDescriptor<SimpleEntity> simpleEntityDescriptor = sessionFactory().getMetamodel().getEntityDescriptor( SimpleEntity.class );

		sessionFactoryScope().inSession(
				session -> {
					// build the "shaped array" - note, because this class is all basic values, the flat and shaped arrays will actually be the same
					final Object[] stateArray = extractStateArray( simpleEntityDescriptor, initialInstance, session );
					assertThat( stateArray, notNullValue() );
					assertThat( stateArray.length, equalTo( 5 ) );

					assertThat( stateArray[0], equalTo( theDate ) );
					assertThat( stateArray[1], equalTo( theInstant ) );
					assertThat( stateArray[2], equalTo( theInt ) );
					assertThat( stateArray[3], equalTo( theLong ) );
					assertThat( stateArray[4], equalTo( theString ) );


					// and then back the other way.. but lets change one of the state array elements to make sure the injections work
					stateArray[ 4 ] = "new string";
					injectStateArray( simpleEntityDescriptor, initialInstance, stateArray, session );

					assertThat( initialInstance.getSomeDate(), equalTo( theDate ) );
					assertThat( initialInstance.getSomeInstant(), equalTo( theInstant ) );
					assertThat( initialInstance.getSomeInteger(), equalTo( theInt ) );
					assertThat( initialInstance.getSomeLong(), equalTo( theLong ) );
					assertThat( initialInstance.getSomeString(), equalTo( "new string" ) );
				}
		);
	}


	@Test
	public void testComposites() {
		final String entityName = "entity name";

		final String nestedString = "nested string";
		final String nestedString2 = "second nested string";
		final Component.Nested nestedComponentInstance = new Component.Nested(
				nestedString,
				nestedString2
		);

		final Integer componentInteger = 2;
		final Long componentLong = 4L;
		final int componentInt = 6;
		final String componentString = "component string";

		final Component componentInstance = new Component(
				componentInteger,
				componentLong,
				componentInt,
				componentString,
				nestedComponentInstance
		);

		final EntityOfComposites entityInstance = new EntityOfComposites(
				1,
				entityName,
				componentInstance
		);

		final EntityTypeDescriptor<EntityOfComposites> entityDescriptor = sessionFactory().getMetamodel().getEntityDescriptor( EntityOfComposites.class );

		sessionFactoryScope().inSession(
				session -> {
					// build the "shaped array" - note, because this class is all basic values, the flat and shaped arrays will actually be the same
					final Object[] stateArray = extractStateArray( entityDescriptor, entityInstance, session );
					assertThat( stateArray, notNullValue() );
					assertThat( stateArray.length, equalTo( 2 ) );

					assertThat( stateArray[0], instanceOf( Object[].class ) );

					assertThat( stateArray[1], equalTo( entityName ) );


					// and then back the other way.. but lets change one of the state array elements to make sure the injections work
					stateArray[ 1 ] = "new name";
					injectStateArray( entityDescriptor, entityInstance, stateArray, session );

					assertThat( entityInstance.getComponent().getBasicInteger(), equalTo( componentInteger ) );
					assertThat( entityInstance.getComponent().getBasicLong(), equalTo( componentLong ) );
					assertThat( entityInstance.getComponent().getBasicPrimitiveInt(), equalTo( componentInt ));
					assertThat( entityInstance.getComponent().getBasicString(), equalTo( componentString ) );
					assertThat( entityInstance.getComponent().getNested().getNestedValue(), equalTo( nestedString ) );
					assertThat( entityInstance.getComponent().getNested().getSecondNestedValue(), equalTo( nestedString2 ) );

					assertThat( entityInstance.getName(), equalTo( "new name" ) );
				}
		);
	}


	@Test
	public void testManyToOne() {
		final String rootEntityName = "root entity";
		final Integer rootEntityInteger = 5;

		final String subEntityName = "sub entity";
		final Integer subEntityInteger = 7;


		final EntityWithManyToOneSelfReference rootEntityInstance = new EntityWithManyToOneSelfReference(
				1,
				rootEntityName,
				rootEntityInteger
		);


		final EntityWithManyToOneSelfReference subEntityInstance = new EntityWithManyToOneSelfReference(
				2,
				subEntityName,
				rootEntityInstance,
				subEntityInteger
		);

		final EntityTypeDescriptor<EntityWithManyToOneSelfReference> entityDescriptor = sessionFactory().getMetamodel().getEntityDescriptor( EntityWithManyToOneSelfReference.class );

		sessionFactoryScope().inSession(
				session -> {
					// build the "shaped array" - note, because this class is all basic values, the flat and shaped arrays will actually be the same
					final Object[] stateArray = extractStateArray( entityDescriptor, subEntityInstance, session );
					assertThat( stateArray, notNullValue() );
					assertThat( stateArray.length, equalTo( 3 ) );

					assertThat( stateArray[0], equalTo( subEntityName ) );

					final int rootEntityId = 1;
					assertThat( stateArray[1], equalTo( rootEntityId ) );

					assertThat( stateArray[2], equalTo( subEntityInteger ) );

					// "back the other way" wont work here as the code would be unable to resolve the many-to-one instance
				}
		);
	}


















	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( SimpleEntity.class );
		metadataSources.addAnnotatedClass( EntityWithManyToOneSelfReference.class );
		metadataSources.addAnnotatedClass( EntityOfComposites.class );
	}

	// todo (6.0) : Need to decide how to best handle the "state array" versus the "shaped array", if anything

	private Object[] extractStateArray(
			EntityTypeDescriptor entityDescriptor,
			Object entityInstance,
			SharedSessionContractImplementor session) {
		// just to  make sure it works
		entityDescriptor.getIdentifier( entityInstance, session );

//		final ArrayList<Object> stateValues = new ArrayList<>();
//		entityDescriptor.dehydrate(
//				entityInstance,
//				(jdbcValue, type, boundColumn) -> stateValues.add( jdbcValue ),
//				Clause.IRRELEVANT,
//				session
//		);
//		return stateValues.toArray( new Object[0] );


		// This could be the implementation of `org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor#getPropertyValues`
		// or we could rename that method to `#extractStateArray`
		//
		// Calls `StateArrayContributor#unresolve` for each


//		final Object[] stateArray = new Object[ entityDescriptor.getStateArrayContributors().size() ];
//
//		entityDescriptor.visitStateArrayContributors(
//				contributor -> {
//					stateArray[ contributor.getStateArrayPosition() ] = contributor.unresolve(
//							contributor.getPropertyAccess().getGetter().get( entityInstance  ),
//							session
//					);
//				}
//		);


		final Object[] stateArray = entityDescriptor.getPropertyValues( entityInstance );

		entityDescriptor.visitStateArrayContributors(
				contributor -> {
					final int position = contributor.getStateArrayPosition();
					stateArray[ position ] = contributor.unresolve( stateArray[ position ], session );
				}
		);

		return stateArray;
	}

	private void injectStateArray(
			EntityTypeDescriptor entityDescriptor,
			Object entityInstance,
			Object[] stateArray,
			SharedSessionContractImplementor session) {
		injectStateArray(
				entityDescriptor,
				entityInstance,
				stateArray,
				new BasicExecutionContext( session ),
				session
		);
	}

	private void injectStateArray(
			EntityTypeDescriptor entityDescriptor,
			Object entityInstance,
			final Object[] stateArray,
			ExecutionContext executionContext,
			SharedSessionContractImplementor session) {
		entityDescriptor.visitStateArrayContributors(
				contributor -> {
					final int position = contributor.getStateArrayPosition();
					stateArray[ position ] = contributor.resolveHydratedState(
							stateArray[ position ],
							executionContext,
							session,
							entityInstance
					);
				}
		);

		entityDescriptor.setPropertyValues( entityInstance, stateArray );
	}
}
