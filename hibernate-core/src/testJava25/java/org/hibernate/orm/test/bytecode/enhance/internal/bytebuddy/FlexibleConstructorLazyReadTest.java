package org.hibernate.orm.test.bytecode.enhance.internal.bytebuddy;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@BytecodeEnhanced
@EnhancementOptions(inlineDirtyChecking = true, lazyLoading = true)
public class FlexibleConstructorLazyReadTest {
	@Test
	void fieldReadFromAnotherEntityBeforeSuperCallInvokesInterceptor() {
		var target = new FlexEntity();
		List<String> reads = new ArrayList<>();
		( (PersistentAttributeInterceptable) target ).$$_hibernate_setInterceptor(
				(PersistentAttributeInterceptor) Proxy.newProxyInstance(
						getClass().getClassLoader(),
						new Class<?>[] { PersistentAttributeInterceptor.class },
						(proxy, method, args) -> {
							if ( method.getName().equals( "readObject" ) ) {
								reads.add( (String) args[1] );
								return "loaded";
							}
							return args == null ? null : args[args.length - 1];
						}
				) );

		var entity = new FlexEntity( target );

		assertThat( reads ).containsExactly( "title" );
		assertThat( entity.observedTitle ).isEqualTo( "loaded" );
	}

	@Entity(name = "LazyReadFlexEntity")
	public static class FlexEntity {
		@Id
		private Long id;

		private String title;

		@Transient
		private String observedTitle;

		public FlexEntity() {
		}

		public FlexEntity(FlexEntity other) {
			String value = other.title;
			super();
			this.observedTitle = value;
		}
	}
}
