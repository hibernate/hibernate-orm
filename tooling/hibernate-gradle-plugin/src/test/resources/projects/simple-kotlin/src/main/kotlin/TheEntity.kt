import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
class TheEntity (
    @Id
    var id: Long? = null,
    var name: String? = null,
)