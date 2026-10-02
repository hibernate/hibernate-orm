package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Incubating;
import org.hibernate.query.common.FrameExclusion;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;

/**
 * Common contract for window parts used in window and aggregate functions.
 *
 * @author Marco Belladelli
 */
@Incubating(since = "6.3")
public interface JpaWindow {
	/**
	 * Add partition by expressions to the window.
	 *
	 * @param expressions the partition by expressions
	 *
	 * @return the modified window
	 */
	@Nonnull
	JpaWindow partitionBy(@Nonnull Expression<?>... expressions);

	/**
	 * Add order by expressions to the window.
	 *
	 * @param expressions the order by expressions
	 *
	 * @return the modified window
	 */
	@Nonnull
	JpaWindow orderBy(@Nonnull Order... expressions);

	/**
	 * Add a {@code ROWS} frame clause to the window and define
	 * start and end {@link JpaWindowFrame} specifications.
	 *
	 * @param startFrame the start frame
	 * @param endFrame the optional end frame
	 *
	 * @return the modified window
	 */
	@Nonnull
	JpaWindow frameRows(@Nullable JpaWindowFrame startFrame, @Nullable JpaWindowFrame endFrame);

	/**
	 * Add a {@code RANGE} frame clause to the window and define
	 * start and end {@link JpaWindowFrame} specifications.
	 *
	 * @param startFrame the start frame
	 * @param endFrame the optional end frame
	 *
	 * @return the modified window
	 */
	@Nonnull
	JpaWindow frameRange(@Nullable JpaWindowFrame startFrame, @Nullable JpaWindowFrame endFrame);

	/**
	 * Add a {@code GROUPS} frame clause to the window and define
	 * start and end {@link JpaWindowFrame} specifications.
	 *
	 * @param startFrame the start frame
	 * @param endFrame the optional end frame
	 *
	 * @return the modified window
	 */
	@Nonnull
	JpaWindow frameGroups(@Nullable JpaWindowFrame startFrame, @Nullable JpaWindowFrame endFrame);

	/**
	 * Set a {@link FrameExclusion} for this window's frame.
	 *
	 * @param frameExclusion the frame exclusion
	 *
	 * @return the modified window
	 */
	@Nonnull
	JpaWindow frameExclude(@Nonnull FrameExclusion frameExclusion);
}
