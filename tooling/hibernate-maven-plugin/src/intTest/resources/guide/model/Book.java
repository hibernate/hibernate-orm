package example;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/// Minimal model for the Build Tool Integration examples.
///
/// @author Steve Ebersole
@Entity
public class Book {
    @Id
    public Long id;
    public String title;
}
