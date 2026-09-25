package org.hibernate.sql.exec.spi;

import org.hibernate.Incubating;

/**
 * Common marker interface for {@linkplain PreAction} and {@linkplain PostAction}.
 *
 * @implSpec Split to allow implementing both simultaneously.
 *
 * @author Steve Ebersole
 */
@Incubating(since = "6.0", group = "sql-execution")
public interface SecondaryAction {
}
