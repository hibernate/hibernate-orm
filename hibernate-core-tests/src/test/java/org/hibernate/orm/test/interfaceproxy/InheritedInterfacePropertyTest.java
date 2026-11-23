package org.hibernate.orm.test.interfaceproxy;

import java.io.Serializable;

import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// HBM property access through an interface whose boolean accessor is inherited two levels up.
///
/// @author Steve Ebersole
@JiraKey("HHH-11498")
@DomainModel(xmlMappings = "org/hibernate/orm/test/interfaceproxy/InheritedInterfaceProperty.hbm.xml")
@SessionFactory
public class InheritedInterfacePropertyTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void inheritedBooleanAccessors(SessionFactoryScope scope) {
		assertThat( ReflectHelper.findGetterMethod( ContentElement.class, "trashed" ).getDeclaringClass() )
				.isEqualTo( Trashable.class );
		assertThat( ReflectHelper.findSetterMethod( ContentElement.class, "trashed", boolean.class )
				.getDeclaringClass() ).isEqualTo( Trashable.class );

		scope.inTransaction( session -> {
			var content = new ContentElementImpl();
			content.setId( 1L );
			content.setTrashed( true );
			session.persist( content );
		} );
		scope.inTransaction( session -> {
			ContentElement content = session.find( ContentElement.class, 1L );
			assertThat( content.isTrashed() ).isTrue();
			content.setTrashed( false );
		} );
		scope.inTransaction( session -> {
			ContentElement content = session.find( ContentElement.class, 1L );
			assertThat( content.isTrashed() ).isFalse();
		} );
	}

	public interface Trashable {
		boolean isTrashed();
		void setTrashed(boolean trashed);
	}

	public interface PageElement extends Trashable, Serializable {
	}

	public interface ContentElement extends PageElement {
		Long getId();
		void setId(Long id);
	}

	public static class ContentElementImpl implements ContentElement {
		private Long id;
		private boolean trashed;

		@Override
		public Long getId() {
			return id;
		}

		@Override
		public void setId(Long id) {
			this.id = id;
		}

		@Override
		public boolean isTrashed() {
			return trashed;
		}

		@Override
		public void setTrashed(boolean trashed) {
			this.trashed = trashed;
		}
	}
}
