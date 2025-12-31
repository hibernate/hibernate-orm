/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.AnnotationBasedGenerator;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Member;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Yanming Zhou
 */
@Jpa(annotatedClasses = {MetaGeneratorTest.Thing.class, MetaGeneratorTest.SecondThing.class,
		MetaGeneratorTest.ThirdThing.class, MetaGeneratorTest.FourthThing.class, MetaGeneratorTest.FifthThing.class})
public class MetaGeneratorTest {

	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			Thing thing = new Thing();
			em.persist( thing );
			assertEquals( 1, thing.id );
			thing = new Thing();
			em.persist( thing );
			assertEquals( 2, thing.id );
		} );

		scope.inTransaction( em -> {
			SecondThing thing = new SecondThing();
			em.persist( thing );
			assertEquals( 2, thing.id );
			thing = new SecondThing();
			em.persist( thing );
			assertEquals( 3, thing.id );
		} );

		scope.inTransaction( em -> {
			ThirdThing thing = new ThirdThing();
			em.persist( thing );
			assertEquals( 3, thing.id );
			thing = new ThirdThing();
			em.persist( thing );
			assertEquals( 4, thing.id );
		} );

		scope.inTransaction( em -> {
			FourthThing thing = new FourthThing();
			em.persist( thing );
			assertEquals( 4, thing.id );
			thing = new FourthThing();
			em.persist( thing );
			assertEquals( 5, thing.id );
		} );

		scope.inTransaction( em -> {
			FifthThing thing = new FifthThing();
			em.persist( thing );
			assertEquals( 5, thing.id );
			thing = new FifthThing();
			em.persist( thing );
			assertEquals( 6, thing.id );
		} );
	}

	@Entity static class Thing {
		@Id @Sequence
		long id;
	}

	@Entity static class SecondThing {
		@Id @SecondSequence(initial = 2)
		long id;
	}

	@Entity static class ThirdThing {
		@Id @ThirdSequence(initial = 3)
		long id;
	}

	@Entity static class FourthThing {
		@Id @FourthSequence(initial = 4)
		long id;
	}

	@Entity static class FifthThing {
		@Id @FifthSequence(initial = 5)
		long id;
	}

	@IdGeneratorType(SequenceGenerator.class)
	@Target({METHOD, FIELD})
	@Retention(RUNTIME)
	public @interface Sequence {
		long initial() default 1;
	}

	@IdGeneratorType(SecondSequenceGenerator.class)
	@Target({METHOD, FIELD})
	@Retention(RUNTIME)
	public @interface SecondSequence{
		long initial() default 1;
	}

	@IdGeneratorType(ThirdSequenceGenerator.class)
	@Target({METHOD, FIELD})
	@Retention(RUNTIME)
	public @interface ThirdSequence {
		long initial() default 1;
	}

	@IdGeneratorType(FourthSequenceGenerator.class)
	@Target({METHOD, FIELD})
	@Retention(RUNTIME)
	public @interface FourthSequence {
		long initial() default 1;
	}

	@IdGeneratorType(FifthSequenceGenerator.class)
	@Target({METHOD, FIELD})
	@Retention(RUNTIME)
	public @interface FifthSequence {
		long initial() default 1;
	}

	static class SequenceGenerator extends AbstractSequenceGenerator {

		SequenceGenerator(Sequence sequence) {
			super(sequence.initial());
		}

	}

	static class SecondSequenceGenerator extends AbstractSequenceGenerator {

		SecondSequenceGenerator(GeneratorCreationContext context) {
			super( ( (Field) context.getMemberDetails().toJavaMember() ).getAnnotation( SecondSequence.class ).initial() );
		}

	}

	static class ThirdSequenceGenerator extends AbstractSequenceGenerator {

		ThirdSequenceGenerator(ThirdSequence sequence, GeneratorCreationContext context) {
			super(sequence.initial());
			if ( !sequence.equals( ( (Field) context.getMemberDetails().toJavaMember() ).getAnnotation( ThirdSequence.class ) )) {
				throw new IllegalArgumentException(context.getMemberDetails().toJavaMember() + " should be annotated with " + sequence);
			}
		}

	}

	static class FourthSequenceGenerator extends AbstractSequenceGenerator implements AnnotationBasedGenerator<FourthSequence> {

		FourthSequenceGenerator() {
			super(0);
		}

		@Override
		public void initialize(FourthSequence sequence, GeneratorCreationContext context) {
			initial = sequence.initial();
			if ( !sequence.equals( ( (Field) context.getMemberDetails().toJavaMember() ).getAnnotation( FourthSequence.class ) )) {
				// only for validation
				throw new IllegalArgumentException(context.getMemberDetails().toJavaMember() + " should be annotated with " + sequence);
			}
		}
	}

	/**
	 * test for deprecated signature (eventually remove)
	 */
	static class FifthSequenceGenerator extends AbstractSequenceGenerator implements AnnotationBasedGenerator<FifthSequence> {

		FifthSequenceGenerator() {
			super(0);
		}

		@SuppressWarnings( "removal" )
		@Override
		public void initialize(FifthSequence sequence, Member member, GeneratorCreationContext context) {
			initial = sequence.initial();
			if ( !member.equals( context.getMemberDetails().toJavaMember() ) ) {
				// only for validation
				throw new IllegalArgumentException(context.getMemberDetails().toJavaMember() + " should be equal to " + member);
			}
		}
	}

	static abstract class AbstractSequenceGenerator implements IdentifierGenerator {

		long initial;

		AbstractSequenceGenerator(long initial) {
			this.initial = initial;
		}

		@Override
		public Object generate(SharedSessionContractImplementor session, Object object) {
			return initial++;
		}
	}
}
