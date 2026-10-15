package example;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/// Verifies enhancement selection independently of annotation processing.
///
/// @author Steve Ebersole
@Entity
public class Excluded {
    @Id
    public Long id;
}
