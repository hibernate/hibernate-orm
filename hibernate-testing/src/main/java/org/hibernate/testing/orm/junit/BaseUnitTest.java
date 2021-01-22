package org.hibernate.testing.orm.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

@Inherited
@Target( ElementType.TYPE )
@Retention( RetentionPolicy.RUNTIME )

@ExtendWith( FailureExpectedExtension.class )
@ExtendWith( ExpectedExceptionExtension.class )
@ExtendWith( DialectFilterExtension.class )
public @interface BaseUnitTest {

}
