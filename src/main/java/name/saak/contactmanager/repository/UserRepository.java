package name.saak.contactmanager.repository;

import name.saak.contactmanager.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByGiteaUserId(Long giteaUserId);

    boolean existsByGiteaUserId(Long giteaUserId);

    @Query("SELECT COUNT(u) FROM User u")
    long countAllUsers();
}
