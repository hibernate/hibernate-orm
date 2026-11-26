package org.hibernate.dialect.sql.ast.spi;

import java.util.Objects;
import java.util.function.Consumer;

import org.hibernate.SPI;

/// Dialect strategy for selecting the SQL rendering plan for a paginated query
/// part.
///
/// A custom translator should normally return one of the reusable strategies in
/// [StandardPaginationRenderingSupport] from its
/// `getPaginationRenderingSupport()` hook. Implement this contract only when
/// plan selection depends on different semantic conditions. Implementations
/// select behavior only: the translator retains ownership of SQL rendering,
/// traversal stacks, and parameter bindings.
///
/// @since 8.0
/// @author Steve Ebersole
@FunctionalInterface
@SPI(SPI.Role.IMPLEMENT)
public interface PaginationRenderingSupport {
	/// Select the rendering plan for the supplied semantic context.
	///
	/// @return a non-null plan which the translator can render
	PaginationRenderingPlan determinePlan(PaginationRenderingRequest request);

	/// Reject pagination of a query part which the Dialect cannot render.
	///
	/// The translator calls this once for each query part, at the position of its
	/// trailing offset/fetch clause, whether or not the selected plan renders
	/// anything there. Plan selection, by contrast, is requested several times
	/// for a query part and in different translator states, so a check belongs
	/// here and not in [#determinePlan(PaginationRenderingRequest)].
	///
	/// The default accepts every request.
	///
	/// @throws RuntimeException if the request cannot be rendered
	default void validate(PaginationRenderingRequest request) {
	}

	/// Combine plan selection with a validation.
	///
	/// @param planSelector selects the rendering plan
	/// @param validator rejects a request which cannot be rendered, by throwing
	///
	/// @return a strategy delegating to the two arguments
	static PaginationRenderingSupport withValidation(
			PaginationRenderingSupport planSelector,
			Consumer<PaginationRenderingRequest> validator) {
		Objects.requireNonNull( planSelector, "planSelector" );
		Objects.requireNonNull( validator, "validator" );
		return new PaginationRenderingSupport() {
			@Override
			public PaginationRenderingPlan determinePlan(PaginationRenderingRequest request) {
				return planSelector.determinePlan( request );
			}

			@Override
			public void validate(PaginationRenderingRequest request) {
				planSelector.validate( request );
				validator.accept( request );
			}
		};
	}
}
