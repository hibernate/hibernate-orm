package org.hibernate.orm.test.jpa;
import jakarta.annotation.Nonnull;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;

/**
 * @author Emmanuel Bernard
 */
public class NoOpListener implements PreInsertEventListener {
	public boolean onPreInsert(@Nonnull PreInsertEvent event) {
		return false;
	}
}
