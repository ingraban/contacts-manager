package name.saak.contactmanager.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "contact",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_contact_name_address",
           columnNames = {"vorname", "nachname", "strasse", "postleitzahl", "ort"}
       ))
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 100, message = "Anrede darf maximal 100 Zeichen lang sein")
    @Column(name = "anrede", length = 100)
    private String anrede;

    @NotBlank(message = "Vorname ist erforderlich")
    @Size(max = 100, message = "Vorname darf maximal 100 Zeichen lang sein")
    @Column(name = "vorname", nullable = false, length = 100)
    private String vorname;

    @NotBlank(message = "Nachname ist erforderlich")
    @Size(max = 100, message = "Nachname darf maximal 100 Zeichen lang sein")
    @Column(name = "nachname", nullable = false, length = 100)
    private String nachname;

    @NotBlank(message = "Straße ist erforderlich")
    @Size(max = 200, message = "Straße darf maximal 200 Zeichen lang sein")
    @Column(name = "strasse", nullable = false, length = 200)
    private String strasse;

    @NotBlank(message = "Postleitzahl ist erforderlich")
    @Size(max = 10, message = "Postleitzahl darf maximal 10 Zeichen lang sein")
    @Column(name = "postleitzahl", nullable = false, length = 10)
    private String postleitzahl;

    @NotBlank(message = "Ort ist erforderlich")
    @Size(max = 100, message = "Ort darf maximal 100 Zeichen lang sein")
    @Column(name = "ort", nullable = false, length = 100)
    private String ort;

    @Size(max = 30, message = "Telefon1 darf maximal 30 Zeichen lang sein")
    @Column(name = "telefon1", length = 30)
    private String telefon1;

    @Size(max = 30, message = "Telefon2 darf maximal 30 Zeichen lang sein")
    @Column(name = "telefon2", length = 30)
    private String telefon2;

    @Email(message = "Bitte geben Sie eine gültige E-Mail-Adresse ein")
    @Size(max = 255, message = "E-Mail darf maximal 255 Zeichen lang sein")
    @Column(name = "email", length = 255)
    private String email;

    @Size(max = 200, message = "Firma darf maximal 200 Zeichen lang sein")
    @Column(name = "firma", length = 200)
    private String firma;

    @Column(name = "bemerkung", columnDefinition = "text")
    private String bemerkung;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "contact_hashtag",
        joinColumns = @JoinColumn(name = "contact_id"),
        inverseJoinColumns = @JoinColumn(name = "hashtag_id")
    )
    private Set<Hashtag> hashtags = new HashSet<>();

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public Contact() {
    }

    public Contact(String vorname, String nachname, String strasse, String postleitzahl, String ort) {
        this.vorname = vorname;
        this.nachname = nachname;
        this.strasse = strasse;
        this.postleitzahl = postleitzahl;
        this.ort = ort;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAnrede() {
        return anrede;
    }

    public void setAnrede(String anrede) {
        this.anrede = anrede;
    }

    public String getVorname() {
        return vorname;
    }

    public void setVorname(String vorname) {
        this.vorname = vorname;
    }

    public String getNachname() {
        return nachname;
    }

    public void setNachname(String nachname) {
        this.nachname = nachname;
    }

    public String getStrasse() {
        return strasse;
    }

    public void setStrasse(String strasse) {
        this.strasse = strasse;
    }

    public String getPostleitzahl() {
        return postleitzahl;
    }

    public void setPostleitzahl(String postleitzahl) {
        this.postleitzahl = postleitzahl;
    }

    public String getOrt() {
        return ort;
    }

    public void setOrt(String ort) {
        this.ort = ort;
    }

    public String getTelefon1() {
        return telefon1;
    }

    public void setTelefon1(String telefon1) {
        this.telefon1 = telefon1;
    }

    public String getTelefon2() {
        return telefon2;
    }

    public void setTelefon2(String telefon2) {
        this.telefon2 = telefon2;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirma() {
        return firma;
    }

    public void setFirma(String firma) {
        this.firma = firma;
    }

    public String getBemerkung() {
        return bemerkung;
    }

    public void setBemerkung(String bemerkung) {
        this.bemerkung = bemerkung;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Set<Hashtag> getHashtags() {
        return hashtags;
    }

    public void setHashtags(Set<Hashtag> hashtags) {
        this.hashtags = hashtags;
    }

    // Helper methods for bidirectional relationship management
    public void addHashtag(Hashtag hashtag) {
        hashtags.add(hashtag);
        hashtag.getContacts().add(this);
    }

    public void removeHashtag(Hashtag hashtag) {
        hashtags.remove(hashtag);
        hashtag.getContacts().remove(this);
    }

    // equals and hashCode based on business key (name + address)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Contact contact = (Contact) o;
        return Objects.equals(vorname, contact.vorname) &&
               Objects.equals(nachname, contact.nachname) &&
               Objects.equals(strasse, contact.strasse) &&
               Objects.equals(postleitzahl, contact.postleitzahl) &&
               Objects.equals(ort, contact.ort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vorname, nachname, strasse, postleitzahl, ort);
    }

    @Override
    public String toString() {
        return "Contact{" +
               "id=" + id +
               ", vorname='" + vorname + '\'' +
               ", nachname='" + nachname + '\'' +
               ", ort='" + ort + '\'' +
               '}';
    }
}
