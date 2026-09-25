package org.hibernate.processor.test.util;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(CompilationExtension.class)
public @interface CompilationTest {
}
