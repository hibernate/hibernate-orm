import jakarta.persistence.*
import org.hibernate.annotations.BatchSize

@Entity
@BatchSize(size = 20)
class TheEntity (
    @Id
    var id: Long? = null,
    var name: String? = null,

    @Embedded
    var theEmbeddable: TheEmbeddable? = null,

    @ManyToOne
    @JoinColumn
    val theManyToOne: TheEntity? = null,

    @OneToMany(mappedBy = "theManyToOne")
    val theOneToMany: Set<TheEntity>? = null,

    @ElementCollection
    @JoinColumn(name = "owner_id")
    val theEmbeddableCollection: Set<TheEmbeddable>? = null
)
