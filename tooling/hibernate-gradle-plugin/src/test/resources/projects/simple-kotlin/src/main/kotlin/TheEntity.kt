import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.BatchSize

@Entity
@BatchSize(size = 20)
class TheEntity (
    @Id
    var id: Long? = null,
    var name: String? = null,
)