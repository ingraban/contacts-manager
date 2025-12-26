package name.saak.contactmanager.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "hashtag")
public class Hashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name ist erforderlich")
    @Size(min = 2, max = 50, message = "Name muss zwischen 2 und 50 Zeichen lang sein")
    @Pattern(regexp = "^#[a-zA-Z0-9_]+$", message = "Name muss mit # beginnen und darf nur Buchstaben, Zahlen und Unterstriche enthalten")
    @Column(name = "name", nullable = false, length = 50, unique = true)
    private String name;

    @Size(max = 500, message = "Beschreibung darf maximal 500 Zeichen lang sein")
    @Column(name = "beschreibung", length = 500)
    private String beschreibung;

    @Column(name = "gesperrt", nullable = false)
    private boolean gesperrt = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToMany(mappedBy = "hashtags", fetch = FetchType.LAZY)
    private Set<Contact> contacts = new HashSet<>();

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        normalizeName();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        normalizeName();
        updatedAt = LocalDateTime.now();
    }

    // Normalize name to lowercase
    private void normalizeName() {
        if (name != null) {
            name = name.toLowerCase();
        }
    }

    // Constructors
    public Hashtag() {
    }

    public Hashtag(String name) {
        this.name = name;
    }

    public Hashtag(String name, String beschreibung) {
        this.name = name;
        this.beschreibung = beschreibung;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    public void setBeschreibung(String beschreibung) {
        this.beschreibung = beschreibung;
    }

    public boolean isGesperrt() {
        return gesperrt;
    }

    public void setGesperrt(boolean gesperrt) {
        this.gesperrt = gesperrt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Set<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(Set<Contact> contacts) {
        this.contacts = contacts;
    }

    // equals and hashCode based on business key (lowercase name)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Hashtag hashtag = (Hashtag) o;
        return Objects.equals(name != null ? name.toLowerCase() : null,
                            hashtag.name != null ? hashtag.name.toLowerCase() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name != null ? name.toLowerCase() : null);
    }

    @Override
    public String toString() {
        return "Hashtag{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", gesperrt=" + gesperrt +
               '}';
    }
}
