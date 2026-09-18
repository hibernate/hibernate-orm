package org.hibernate.usertype;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Incubating;
import org.hibernate.annotations.Type;
import org.hibernate.models.spi.MemberDetails;

import java.util.Properties;

/**
 * Access to information useful during {@linkplain UserType} creation and initialization.
 *
 * @author Yanming Zhou
 * @see AnnotationBasedUserType
 *
 * @since 7.3
 */
@Incubating(since = "7.3")
public interface UserTypeCreationContext {
	/**
	 * Access to the {@link MemberDetails}.
	 */
	@Nullable
	MemberDetails getMemberDetails();

	/**
	 * Access to the parameters.
	 *
	 * @see Type#parameters()
	 */
	@Nonnull
	Properties getParameters();

}
